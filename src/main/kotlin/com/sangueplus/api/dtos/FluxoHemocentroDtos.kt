package com.sangueplus.api.dtos

import java.time.Instant

data class QrCheckinResponse(
    val agendamentoId: Long,
    val conteudo: String,
    val expiraEm: Instant
)

data class ValidarQrRequest(
    val codigo: String
)

data class EncerrarAtendimentoRequest(
    val motivo: String
)

data class DisponibilidadeHorarioRequest(
    val ativo: Boolean? = null
)

data class DetalheAtendimentoResponse(
    val agendamento: AgendamentoResponse,
    val atendimento: AtendimentoResponse,
    val doador: UsuarioResponse,
    val respostas: PreTriagemRequest,
    val preTriagem: PreTriagemResponse,
    val motivoEncerramento: String?,
    val encerradoEm: Instant?
)