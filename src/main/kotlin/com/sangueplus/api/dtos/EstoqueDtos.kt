package com.sangueplus.api.dtos

import java.time.Instant

data class AtualizarEstoqueRequest(
    val quantidadeDisponivel: Int?,
    val nivelMinimo: Int?,
    val nivelIdeal: Int?,
    val versao: Long? = null
)

enum class SituacaoEstoque {
    NAO_INFORMADO,
    CRITICO,
    BAIXO,
    NORMAL
}

data class EstoqueItemResponse(
    val tipoSanguineo: String,
    val quantidadeDisponivel: Int?,
    val nivelMinimo: Int?,
    val nivelIdeal: Int?,
    val situacao: SituacaoEstoque,
    val atualizadoEm: Instant?,
    val versao: Long?
)

data class EstoqueHemocentroResponse(
    val hemocentroId: Long,
    val nomeHemocentro: String,
    val itens: List<EstoqueItemResponse>
)