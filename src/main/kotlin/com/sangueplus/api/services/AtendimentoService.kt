package com.sangueplus.api.services

import com.sangueplus.api.dtos.*
import com.sangueplus.api.models.*
import com.sangueplus.api.repositories.AgendamentoRepository
import com.sangueplus.api.repositories.DoacaoRepository
import com.sangueplus.api.repositories.UsuarioRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class AtendimentoService(
    private val agendamentos: AgendamentoRepository,
    private val doacoes: DoacaoRepository,
    private val usuarios: UsuarioRepository,
    private val auth: AuthService,
    private val intervalos: IntervaloService
) {
    private val zona = ZoneId.of("America/Sao_Paulo")

    private val tiposPermitidos = setOf(
        "A+", "A-",
        "B+", "B-",
        "AB+", "AB-",
        "O+", "O-"
    )

    @Transactional
    fun registrarChegada(
        authorization: String?,
        agendamentoId: Long
    ): AtendimentoResponse {
        val responsavel = auth.exigirHemocentro(authorization)

        val agendamento = obterAgendamento(
            responsavel,
            agendamentoId
        )

        if (agendamento.status == StatusAgendamento.EM_ATENDIMENTO) {
            return AtendimentoResponse.from(agendamento)
        }

        exigirStatus(
            agendamento,
            StatusAgendamento.AGENDADO,
            "Somente agendamentos ativos podem iniciar atendimento."
        )

        validarJanelaAtendimento(agendamento)

        agendamento.chegadaEm = Instant.now()
        agendamento.status = StatusAgendamento.EM_ATENDIMENTO

        return AtendimentoResponse.from(
            agendamentos.saveAndFlush(agendamento)
        )
    }

    @Transactional
    fun registrarTriagem(
        authorization: String?,
        agendamentoId: Long,
        request: TriagemPresencialRequest
    ): AtendimentoResponse {
        val responsavel = auth.exigirHemocentro(authorization)

        val agendamento = obterAgendamento(
            responsavel,
            agendamentoId
        )

        exigirStatus(
            agendamento,
            StatusAgendamento.EM_ATENDIMENTO,
            "Registre a chegada antes da triagem presencial. " +
                    "Uma triagem já encerrada não pode ser repetida."
        )

        val aprovado = request.aprovado
            ?: falhar(
                HttpStatus.BAD_REQUEST,
                "Informe o resultado da avaliação presencial."
            )

        val observacao = limparTexto(
            request.observacao,
            "A observação"
        )

        if (!aprovado && observacao == null) {
            falhar(
                HttpStatus.BAD_REQUEST,
                "Informe uma orientação para o resultado não aprovado."
            )
        }

        agendamento.status = if (aprovado) {
            StatusAgendamento.APROVADO_TRIAGEM
        } else {
            StatusAgendamento.NAO_APTO_TRIAGEM
        }

        agendamento.triagemPresencialEm = Instant.now()
        agendamento.observacaoTriagem = observacao
        agendamento.responsavelTriagem = responsavel

        return AtendimentoResponse.from(
            agendamentos.saveAndFlush(agendamento)
        )
    }

    @Transactional
    fun registrarDoacao(
        authorization: String?,
        agendamentoId: Long,
        request: RegistrarDoacaoRequest
    ): DoacaoResponse {
        val responsavel = auth.exigirHemocentro(authorization)

        val agendamento = obterAgendamento(
            responsavel,
            agendamentoId
        )

        validar(
            request.tipoSanguineoConfirmado in tiposPermitidos,
            "Informe um tipo sanguíneo confirmado válido."
        )

        val observacoes = limparTexto(
            request.observacoes,
            "As observações"
        )

        val existente = doacoes.findByAgendamento_Id(agendamento.id)

        if (existente != null) {
            if (
                existente.tipoSanguineoConfirmado !=
                request.tipoSanguineoConfirmado ||
                existente.observacoes != observacoes
            ) {
                falhar(
                    HttpStatus.CONFLICT,
                    "A doação já foi registrada com outros dados."
                )
            }

            return DoacaoResponse.from(existente)
        }

        exigirStatus(
            agendamento,
            StatusAgendamento.APROVADO_TRIAGEM,
            "A coleta só pode ser registrada após aprovação " +
                    "na triagem presencial."
        )

        // O doador já está bloqueado pela transação.
        // Outra coleta concorrente desse doador terá de aguardar.
        intervalos.exigirIntervaloParaColeta(
            agendamento.doador.id
        )

        val doacao = doacoes.saveAndFlush(
            Doacao(
                agendamento = agendamento,
                doador = agendamento.doador,
                hemocentro = agendamento.horario.hemocentro,
                registradoPor = responsavel,
                tipoSanguineoConfirmado =
                    request.tipoSanguineoConfirmado,
                observacoes = observacoes
            )
        )

        agendamento.status = StatusAgendamento.DOACAO_REALIZADA
        agendamentos.saveAndFlush(agendamento)

        return DoacaoResponse.from(doacao)
    }

    @Transactional(readOnly = true)
    fun historicoDoador(
        authorization: String?,
        pagina: Int,
        tamanho: Int
    ): List<DoacaoResponse> {
        val doador = auth.exigirDoador(authorization)

        validar(pagina >= 0, "A página não pode ser negativa.")

        validar(
            tamanho in 1..100,
            "O tamanho da página deve estar entre 1 e 100."
        )

        return doacoes
            .findByDoador_IdOrderByRealizadaEmDescIdDesc(
                doador.id,
                PageRequest.of(pagina, tamanho)
            )
            .map { DoacaoResponse.from(it) }
    }

    private fun obterAgendamento(
        responsavel: Usuario,
        agendamentoId: Long
    ): Agendamento {
        val leitura = agendamentos.findById(agendamentoId)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Agendamento não encontrado."
                )
            }

        verificarInstituicao(responsavel, leitura)

        usuarios.buscarParaAgendamento(leitura.doador.id)
            ?: falhar(
                HttpStatus.NOT_FOUND,
                "Doador não encontrado."
            )

        val agendamento =
            agendamentos.buscarParaAlteracao(agendamentoId)
                ?: falhar(
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
            falhar(
                HttpStatus.NOT_FOUND,
                "Agendamento não encontrado."
            )
        }
    }

    private fun validarJanelaAtendimento(
        agendamento: Agendamento
    ) {
        val agora = LocalDateTime.now(zona)
        val inicio = agendamento.horario.inicio

        validar(
            inicio.toLocalDate() == agora.toLocalDate(),
            "O atendimento deve começar no dia agendado."
        )

        validar(
            !agora.isBefore(inicio.minusHours(1)),
            "A chegada pode ser registrada a partir de uma hora " +
                    "antes do horário agendado."
        )
    }

    private fun exigirStatus(
        agendamento: Agendamento,
        esperado: StatusAgendamento,
        mensagem: String
    ) {
        if (agendamento.status != esperado) {
            falhar(HttpStatus.CONFLICT, mensagem)
        }
    }

    private fun limparTexto(
        valor: String?,
        campo: String
    ): String? {
        val texto = valor?.trim()?.takeIf { it.isNotEmpty() }

        validar(
            texto == null || texto.length <= 500,
            "$campo deve ter no máximo 500 caracteres."
        )

        return texto
    }

    private fun validar(
        condicao: Boolean,
        mensagem: String
    ) {
        if (!condicao) {
            falhar(HttpStatus.BAD_REQUEST, mensagem)
        }
    }

    private fun falhar(
        status: HttpStatus,
        mensagem: String
    ): Nothing {
        throw ResponseStatusException(status, mensagem)
    }
}