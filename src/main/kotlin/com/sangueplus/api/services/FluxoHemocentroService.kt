package com.sangueplus.api.services

import com.sangueplus.api.dtos.*
import com.sangueplus.api.models.*
import com.sangueplus.api.repositories.*
import jakarta.persistence.EntityManager
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class FluxoHemocentroService(
    private val auth: AuthService,
    private val agendamentos: AgendamentoRepository,
    private val horarios: HorarioDoacaoRepository,
    private val codigos: QrCheckinRepository,
    private val entityManager: EntityManager
) {

    private val zona = ZoneId.of("America/Sao_Paulo")
    private val random = SecureRandom()
    private val prefixo = "sangueplus:checkin:v1:"

    @Transactional
    fun gerarQr(
        authorization: String?,
        id: Long
    ): QrCheckinResponse {
        val doador = auth.exigirDoador(authorization)

        val agendamento = agendamentos.buscarParaAlteracao(id)
            ?: erro(
                HttpStatus.NOT_FOUND,
                "Agendamento não encontrado."
            )

        if (agendamento.doador.id != doador.id) {
            erro(
                HttpStatus.NOT_FOUND,
                "Agendamento não encontrado."
            )
        }

        exigir(
            agendamento.status == StatusAgendamento.AGENDADO,
            "O QR só pode ser emitido para um agendamento ativo."
        )

        val expiraEm = agendamento.horario.inicio
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zona)
            .toInstant()

        exigir(
            Instant.now().isBefore(expiraEm),
            "Esse agendamento já expirou."
        )

        val bytes = ByteArray(32).also {
            random.nextBytes(it)
        }

        val conteudo = prefixo +
                Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(bytes)

        val qr = codigos.findById(id).orElse(null)

        if (qr == null) {
            codigos.saveAndFlush(
                QrCheckin(
                    agendamentoId = id,
                    codigoHash = hash(conteudo),
                    expiraEm = expiraEm
                )
            )
        } else {
            exigir(
                qr.utilizadoEm == null,
                "O check-in já foi utilizado."
            )

            qr.codigoHash = hash(conteudo)
            qr.expiraEm = expiraEm

            codigos.saveAndFlush(qr)
        }

        return QrCheckinResponse(
            agendamentoId = id,
            conteudo = conteudo,
            expiraEm = expiraEm
        )
    }

    @Transactional
    fun validarQr(
        authorization: String?,
        request: ValidarQrRequest
    ): DetalheAtendimentoResponse {
        val responsavel = auth.exigirHemocentro(authorization)
        val codigo = request.codigo.trim()

        val formatoValido = Regex(
            "^sangueplus:checkin:v1:[A-Za-z0-9_-]{43}$"
        ).matches(codigo)

        if (!formatoValido) {
            erro(
                HttpStatus.BAD_REQUEST,
                "QR Code inválido."
            )
        }

        val codigoHash = hash(codigo)

        val encontrado = codigos.findByCodigoHash(codigoHash)
            ?: erro(
                HttpStatus.NOT_FOUND,
                "QR Code inválido ou substituído."
            )

        // Emissão e validação bloqueiam primeiro o agendamento.
        val agendamento = obterInstitucional(
            responsavel,
            encontrado.agendamentoId
        )

        val qr = codigos.findById(agendamento.id)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "QR Code não encontrado."
                )
            }

        // Recarrega após obter o bloqueio, considerando uma
        // possível renovação ou utilização concorrente.
        entityManager.refresh(qr)

        if (qr.codigoHash != codigoHash) {
            erro(
                HttpStatus.NOT_FOUND,
                "QR Code substituído. Peça o mais recente."
            )
        }

        exigir(
            qr.utilizadoEm == null,
            "Este QR Code já foi utilizado."
        )

        exigir(
            Instant.now().isBefore(qr.expiraEm),
            "Este QR Code expirou."
        )

        exigir(
            agendamento.status == StatusAgendamento.AGENDADO,
            "O agendamento não está disponível para check-in."
        )

        val agora = LocalDateTime.now(zona)
        val inicio = agendamento.horario.inicio

        exigir(
            inicio.toLocalDate() == agora.toLocalDate(),
            "O check-in deve ser feito no dia agendado."
        )

        exigir(
            !agora.isBefore(inicio.minusHours(1)),
            "O check-in abre uma hora antes do atendimento."
        )

        val chegadaEm = Instant.now()

        agendamento.chegadaEm = chegadaEm
        agendamento.status = StatusAgendamento.EM_ATENDIMENTO
        qr.utilizadoEm = chegadaEm

        codigos.saveAndFlush(qr)
        agendamentos.saveAndFlush(agendamento)

        return detalhe(agendamento)
    }

    @Transactional(readOnly = true)
    fun consultar(
        authorization: String?,
        id: Long
    ): DetalheAtendimentoResponse {
        val responsavel = auth.exigirHemocentro(authorization)

        val agendamento = agendamentos.findById(id)
            .orElse(null)
            ?: erro(
                HttpStatus.NOT_FOUND,
                "Agendamento não encontrado."
            )

        verificarInstituicao(responsavel, agendamento)

        return detalhe(agendamento)
    }

    @Transactional
    fun encerrarSemColeta(
        authorization: String?,
        id: Long,
        request: EncerrarAtendimentoRequest
    ): AgendamentoResponse {
        val responsavel = auth.exigirHemocentro(authorization)

        val agendamento = obterInstitucional(
            responsavel,
            id
        )

        val motivo = request.motivo.trim()

        if (motivo.length !in 3..500) {
            erro(
                HttpStatus.BAD_REQUEST,
                "Informe um motivo entre 3 e 500 caracteres."
            )
        }

        exigir(
            agendamento.status in setOf(
                StatusAgendamento.EM_ATENDIMENTO,
                StatusAgendamento.APROVADO_TRIAGEM
            ),
            "Somente um atendimento em andamento pode ser " +
                    "encerrado sem coleta."
        )

        agendamento.status =
            StatusAgendamento.ENCERRADO_SEM_COLETA
        agendamento.motivoEncerramento = motivo
        agendamento.encerradoEm = Instant.now()
        agendamento.encerradoPor = responsavel

        return AgendamentoResponse.from(
            agendamentos.saveAndFlush(agendamento)
        )
    }

    @Transactional
    fun registrarFalta(
        authorization: String?,
        id: Long
    ): AgendamentoResponse {
        val responsavel = auth.exigirHemocentro(authorization)

        val agendamento = obterInstitucional(
            responsavel,
            id
        )

        exigir(
            agendamento.status == StatusAgendamento.AGENDADO,
            "Somente um agendamento sem chegada pode ser " +
                    "marcado como falta."
        )

        exigir(
            !LocalDateTime.now(zona).isBefore(
                agendamento.horario.inicio.plusHours(1)
            ),
            "A falta pode ser registrada uma hora após " +
                    "o horário agendado."
        )

        agendamento.status = StatusAgendamento.FALTOU
        agendamento.motivoEncerramento = "Não compareceu."
        agendamento.encerradoEm = Instant.now()
        agendamento.encerradoPor = responsavel

        return AgendamentoResponse.from(
            agendamentos.saveAndFlush(agendamento)
        )
    }

    @Transactional
    fun disponibilidade(
        authorization: String?,
        id: Long,
        request: DisponibilidadeHorarioRequest
    ): HorarioResponse {
        val responsavel = auth.exigirHemocentro(authorization)

        val horario = horarios.buscarParaReserva(id)
            ?: erro(
                HttpStatus.NOT_FOUND,
                "Horário não encontrado."
            )

        if (horario.hemocentro.id != responsavel.hemocentro?.id) {
            erro(
                HttpStatus.NOT_FOUND,
                "Horário não encontrado."
            )
        }

        val ativo = request.ativo
            ?: erro(
                HttpStatus.BAD_REQUEST,
                "Informe se o horário deve ficar ativo."
            )

        exigir(
            horario.inicio.isAfter(LocalDateTime.now(zona)),
            "Não é possível alterar a disponibilidade " +
                    "de um horário passado."
        )

        horario.ativo = ativo
        horarios.saveAndFlush(horario)

        val ocupadas = agendamentos.countByHorario_IdAndStatus(
            horario.id,
            StatusAgendamento.AGENDADO
        )

        return HorarioResponse(
            horario.id,
            horario.hemocentro.id,
            horario.inicio,
            horario.capacidade,
            (horario.capacidade.toLong() - ocupadas)
                .coerceAtLeast(0),
            horario.ativo
        )
    }

    @Transactional(readOnly = true)
    fun meusHorarios(
        authorization: String?,
        de: LocalDate,
        ate: LocalDate
    ): List<HorarioResponse> {
        val responsavel = auth.exigirHemocentro(authorization)

        if (
            ate.isBefore(de) ||
            ChronoUnit.DAYS.between(de, ate) > 30
        ) {
            erro(
                HttpStatus.BAD_REQUEST,
                "Informe um período de até 31 dias."
            )
        }

        val hemocentroId = responsavel.hemocentro?.id
            ?: erro(
                HttpStatus.FORBIDDEN,
                "Hemocentro não vinculado."
            )

        val encontrados =
            horarios.findByHemocentro_IdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                hemocentroId,
                de.atStartOfDay(),
                ate.plusDays(1).atStartOfDay()
            )

        return encontrados.map { horario ->
            val ocupadas =
                agendamentos.countByHorario_IdAndStatus(
                    horario.id,
                    StatusAgendamento.AGENDADO
                )

            HorarioResponse(
                horario.id,
                hemocentroId,
                horario.inicio,
                horario.capacidade,
                (horario.capacidade.toLong() - ocupadas)
                    .coerceAtLeast(0),
                horario.ativo
            )
        }
    }

    private fun obterInstitucional(
        responsavel: Usuario,
        id: Long
    ): Agendamento {
        val agendamento = agendamentos.buscarParaAlteracao(id)
            ?: erro(
                HttpStatus.NOT_FOUND,
                "Agendamento não encontrado."
            )

        verificarInstituicao(responsavel, agendamento)

        return agendamento
    }

    private fun verificarInstituicao(
        responsavel: Usuario,
        agendamento: Agendamento
    ) {
        if (
            agendamento.horario.hemocentro.id !=
            responsavel.hemocentro?.id
        ) {
            erro(
                HttpStatus.NOT_FOUND,
                "Agendamento não encontrado."
            )
        }
    }

    private fun detalhe(
        agendamento: Agendamento
    ): DetalheAtendimentoResponse {
        val preTriagem = agendamento.preTriagem

        return DetalheAtendimentoResponse(
            agendamento = AgendamentoResponse.from(agendamento),
            atendimento = AtendimentoResponse.from(agendamento),
            doador = UsuarioResponse.from(agendamento.doador),
            respostas = PreTriagemRequest(
                pesoKg = preTriagem.pesoKg,
                bemDeSaude = preTriagem.bemDeSaude,
                sintomasRecentes = preTriagem.sintomasRecentes,
                cirurgiaRecente = preTriagem.cirurgiaRecente,
                tatuagemOuPiercingRecente =
                    preTriagem.tatuagemOuPiercingRecente,
                gravidaOuAmamentando =
                    preTriagem.gravidaOuAmamentando,
                condicaoDeSaudeOuMedicamento =
                    preTriagem.condicaoDeSaudeOuMedicamento,
                descansadoEAlimentado =
                    preTriagem.descansadoEAlimentado,
                autorizacaoResponsavel =
                    preTriagem.autorizacaoResponsavel,
                observacoes = preTriagem.observacoes
            ),
            preTriagem = PreTriagemResponse.from(preTriagem),
            motivoEncerramento = agendamento.motivoEncerramento,
            encerradoEm = agendamento.encerradoEm
        )
    }

    private fun hash(codigo: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(codigo.toByteArray(Charsets.UTF_8))
            .joinToString("") {
                "%02x".format(it.toInt() and 255)
            }
    }

    private fun exigir(
        condicao: Boolean,
        mensagem: String
    ) {
        if (!condicao) {
            erro(HttpStatus.CONFLICT, mensagem)
        }
    }

    private fun erro(
        status: HttpStatus,
        mensagem: String
    ): Nothing {
        throw ResponseStatusException(status, mensagem)
    }
}