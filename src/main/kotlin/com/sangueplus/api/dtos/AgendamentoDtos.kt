package com.sangueplus.api.dtos

import com.sangueplus.api.models.Agendamento
import com.sangueplus.api.models.StatusAgendamento
import java.time.Instant
import java.time.LocalDateTime

data class CriarHorarioRequest(
    val inicio: LocalDateTime,
    val capacidade: Int
)

data class CriarAgendamentoRequest(
    val horarioId: Long
)

data class CancelarAgendamentoRequest(
    val motivo: String? = null
)

data class HorarioResponse(
    val id: Long,
    val hemocentroId: Long,
    val inicio: LocalDateTime,
    val capacidade: Int,
    val vagasDisponiveis: Long,
    val ativo: Boolean,
    val fusoHorario: String = "America/Sao_Paulo"
)

data class AgendamentoResponse(
    val id: Long,
    val doadorId: Long,
    val nomeDoador: String,
    val hemocentroId: Long,
    val nomeHemocentro: String,
    val enderecoHemocentro: String,
    val horarioId: Long,
    val inicio: LocalDateTime,
    val status: StatusAgendamento,
    val preTriagemId: Long,
    val resultadoPreTriagem: ResultadoPreTriagem,
    val criadoEm: Instant,
    val canceladoEm: Instant?,
    val motivoCancelamento: String?,
    val fusoHorario: String = "America/Sao_Paulo",
    val aviso: String =
        "Agendamento não confirma aptidão nem realização da doação. " +
                "A avaliação presencial é obrigatória."
) {
    companion object {
        fun from(agendamento: Agendamento): AgendamentoResponse {
            val horario = agendamento.horario
            val hemocentro = horario.hemocentro

            return AgendamentoResponse(
                id = agendamento.id,
                doadorId = agendamento.doador.id,
                nomeDoador = agendamento.doador.nome,
                hemocentroId = hemocentro.id,
                nomeHemocentro = hemocentro.nome,
                enderecoHemocentro = hemocentro.endereco,
                horarioId = horario.id,
                inicio = horario.inicio,
                status = agendamento.status,
                preTriagemId = agendamento.preTriagem.id,
                resultadoPreTriagem = agendamento.preTriagem.resultado,
                criadoEm = agendamento.criadoEm,
                canceladoEm = agendamento.canceladoEm,
                motivoCancelamento = agendamento.motivoCancelamento
            )
        }
    }
}