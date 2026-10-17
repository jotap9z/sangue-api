package com.sangueplus.api.dtos

import com.sangueplus.api.models.Agendamento
import com.sangueplus.api.models.Doacao
import com.sangueplus.api.models.StatusAgendamento
import java.time.Instant

data class TriagemPresencialRequest(
    val aprovado: Boolean? = null,
    val observacao: String? = null
)

data class RegistrarDoacaoRequest(
    val tipoSanguineoConfirmado: String,
    val observacoes: String? = null
)

data class AtendimentoResponse(
    val agendamentoId: Long,
    val status: StatusAgendamento,
    val chegadaEm: Instant?,
    val triagemPresencialEm: Instant?,
    val observacaoTriagem: String?
) {
    companion object {
        fun from(agendamento: Agendamento): AtendimentoResponse =
            AtendimentoResponse(
                agendamentoId = agendamento.id,
                status = agendamento.status,
                chegadaEm = agendamento.chegadaEm,
                triagemPresencialEm = agendamento.triagemPresencialEm,
                observacaoTriagem = agendamento.observacaoTriagem
            )
    }
}

data class DoacaoResponse(
    val id: Long,
    val agendamentoId: Long,
    val hemocentroId: Long,
    val nomeHemocentro: String,
    val realizadaEm: Instant,
    val tipoSanguineoConfirmado: String,
    val observacoes: String?
) {
    companion object {
        fun from(doacao: Doacao): DoacaoResponse =
            DoacaoResponse(
                id = doacao.id,
                agendamentoId = doacao.agendamento.id,
                hemocentroId = doacao.hemocentro.id,
                nomeHemocentro = doacao.hemocentro.nome,
                realizadaEm = doacao.realizadaEm,
                tipoSanguineoConfirmado =
                    doacao.tipoSanguineoConfirmado,
                observacoes = doacao.observacoes
            )
    }
}