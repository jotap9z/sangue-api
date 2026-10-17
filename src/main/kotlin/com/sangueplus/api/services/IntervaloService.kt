package com.sangueplus.api.services

import com.sangueplus.api.dtos.*
import com.sangueplus.api.models.*
import com.sangueplus.api.repositories.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class IntervaloService(
    private val perfis: PerfilDoacaoRepository,
    private val doacoes: DoacaoRepository,
    private val agendamentos: AgendamentoRepository,
    private val usuarios: UsuarioRepository,
    private val auth: AuthService
) {
    private val zona = ZoneId.of("America/Sao_Paulo")

    @Transactional(readOnly = true)
    fun consultarMeu(
        authorization: String?
    ): IntervaloResponse {
        val doador = auth.exigirDoador(authorization)
        return calcular(doador.id)
    }

    @Transactional
    fun configurarNoAtendimento(
        authorization: String?,
        agendamentoId: Long,
        request: ConfigurarIntervaloRequest
    ): IntervaloResponse {
        val responsavel = auth.exigirHemocentro(authorization)

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

        if (
            agendamento.status != StatusAgendamento.EM_ATENDIMENTO &&
            agendamento.status != StatusAgendamento.APROVADO_TRIAGEM
        ) {
            falhar(
                HttpStatus.CONFLICT,
                "Configure a regra durante um atendimento iniciado, " +
                        "antes da confirmação da coleta."
            )
        }

        val existente = perfis.findById(agendamento.doador.id)
            .orElse(null)

        if (existente == null) {
            perfis.saveAndFlush(
                PerfilDoacao(
                    doadorId = agendamento.doador.id,
                    regra = request.regra,
                    atualizadoPor = responsavel
                )
            )
        } else {
            existente.regra = request.regra
            existente.atualizadoEm = Instant.now()
            existente.atualizadoPor = responsavel

            perfis.saveAndFlush(existente)
        }

        return calcular(agendamento.doador.id)
    }

    @Transactional(readOnly = true)
    fun exigirIntervaloParaColeta(doadorId: Long) {
        val resultado = calcular(doadorId)

        when (resultado.situacao) {
            SituacaoIntervalo.PERFIL_PENDENTE ->
                falhar(
                    HttpStatus.CONFLICT,
                    "O hemocentro deve configurar a regra de intervalo " +
                            "do doador antes de registrar a coleta."
                )

            SituacaoIntervalo.AGUARDANDO_INTERVALO ->
                falhar(
                    HttpStatus.CONFLICT,
                    "O intervalo entre doações ainda não foi cumprido. " +
                            "Previsão pelo histórico: " +
                            resultado.proximaDataPeloHistorico
                )

            else -> Unit
        }
    }

    private fun calcular(doadorId: Long): IntervaloResponse {
        val hoje = LocalDate.now(zona)

        val recentes =
            doacoes.findTop4ByDoador_IdOrderByRealizadaEmDescIdDesc(
                doadorId
            )

        val ultima = recentes.firstOrNull()
            ?.realizadaEm
            ?.atZone(zona)
            ?.toLocalDate()

        // Janela por datas locais: exclui o dia que completou
        // exatamente doze meses.
        val inicioJanela = hoje.minusYears(1)
            .plusDays(1)
            .atStartOfDay(zona)
            .toInstant()

        val quantidade = doacoes
            .countByDoador_IdAndRealizadaEmGreaterThanEqual(
                doadorId,
                inicioJanela
            )

        val perfil = perfis.findById(doadorId).orElse(null)

        if (perfil == null) {
            return IntervaloResponse(
                situacao = SituacaoIntervalo.PERFIL_PENDENTE,
                regra = null,
                ultimaDoacao = ultima,
                proximaDataPeloHistorico = null,
                diasRestantes = null,
                doacoesUltimosDozeMeses = quantidade,
                mensagem = "O hemocentro precisa definir a regra " +
                        "aplicável ao seu acompanhamento."
            )
        }

        if (ultima == null) {
            return IntervaloResponse(
                situacao = SituacaoIntervalo.SEM_DOACOES_REGISTRADAS,
                regra = perfil.regra,
                ultimaDoacao = null,
                proximaDataPeloHistorico = null,
                diasRestantes = null,
                doacoesUltimosDozeMeses = quantidade,
                mensagem = "Não há doações registradas neste sistema. " +
                        "Informe seu histórico à equipe do hemocentro."
            )
        }

        var proximaData = ultima.plusMonths(perfil.regra.meses)

        val limite = perfil.regra.limiteDozeMeses

        if (recentes.size >= limite) {
            val doacaoLimite = recentes[limite - 1]
                .realizadaEm
                .atZone(zona)
                .toLocalDate()
                .plusYears(1)

            if (doacaoLimite.isAfter(proximaData)) {
                proximaData = doacaoLimite
            }
        }

        val aguardando = hoje.isBefore(proximaData)

        return IntervaloResponse(
            situacao = if (aguardando) {
                SituacaoIntervalo.AGUARDANDO_INTERVALO
            } else {
                SituacaoIntervalo.INTERVALO_CUMPRIDO
            },
            regra = perfil.regra,
            ultimaDoacao = ultima,
            proximaDataPeloHistorico = proximaData,
            diasRestantes = ChronoUnit.DAYS
                .between(hoje, proximaData)
                .coerceAtLeast(0),
            doacoesUltimosDozeMeses = quantidade,
            mensagem = if (aguardando) {
                "Aguarde o intervalo. Previsão pelo histórico: $proximaData."
            } else {
                "O intervalo calculado foi cumprido. " +
                        "Uma nova avaliação presencial continua necessária."
            }
        )
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

    private fun falhar(
        status: HttpStatus,
        mensagem: String
    ): Nothing {
        throw ResponseStatusException(status, mensagem)
    }
}