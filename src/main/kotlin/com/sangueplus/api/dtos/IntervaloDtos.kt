package com.sangueplus.api.dtos

import com.sangueplus.api.models.RegraIntervalo
import java.time.LocalDate

data class ConfigurarIntervaloRequest(
    val regra: RegraIntervalo
)

enum class SituacaoIntervalo {
    PERFIL_PENDENTE,
    SEM_DOACOES_REGISTRADAS,
    AGUARDANDO_INTERVALO,
    INTERVALO_CUMPRIDO
}

data class IntervaloResponse(
    val situacao: SituacaoIntervalo,
    val regra: RegraIntervalo?,
    val ultimaDoacao: LocalDate?,
    val proximaDataPeloHistorico: LocalDate?,
    val diasRestantes: Long?,
    val doacoesUltimosDozeMeses: Long,
    val mensagem: String,
    val aviso: String =
        "Previsão baseada apenas nas doações registradas neste sistema. " +
                "Informe doações realizadas em outros serviços. " +
                "Cumprir o intervalo não significa estar clinicamente apto."
)