package com.sangueplus.api.dtos

import com.sangueplus.api.models.ChamadoEmergencia
import com.sangueplus.api.models.NivelUrgencia
import com.sangueplus.api.models.RespostaChamado
import com.sangueplus.api.models.StatusChamado
import java.time.Instant

data class CriarChamadoRequest(
    val tipoSanguineo: String,
    val urgencia: NivelUrgencia,
    val metaDoadores: Int,
    val mensagem: String,
    val titulo: String? = null
)

data class AtualizarChamadoRequest(
    val tipoSanguineo: String,
    val urgencia: NivelUrgencia,
    val metaDoadores: Int,
    val mensagem: String,
    val versao: Long,
    val titulo: String? = null
)

data class EncerrarChamadoRequest(
    val motivo: String,
    val versao: Long
)

data class ChamadoResponse(
    val id: Long,
    val hemocentroId: Long,
    val nomeHemocentro: String,
    val endereco: String,
    val cidade: String,
    val estado: String,
    val telefone: String?,
    val titulo: String,
    val tipoSanguineo: String,
    val urgencia: NivelUrgencia,
    val metaDoadores: Int,
    val respostasAtivas: Long,
    val mensagem: String,
    val status: StatusChamado,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val encerradoEm: Instant?,
    val versao: Long?,
    val aviso: String =
        "Responder demonstra interesse. Não confirma agendamento, " +
                "aptidão para doar ou realização de doação."
) {
    companion object {
        fun from(
            chamado: ChamadoEmergencia,
            respostasAtivas: Long
        ): ChamadoResponse {
            return ChamadoResponse(
                id = chamado.id,
                hemocentroId = chamado.hemocentro.id,
                nomeHemocentro = chamado.hemocentro.nome,
                endereco = chamado.hemocentro.endereco,
                cidade = chamado.hemocentro.cidade,
                estado = chamado.hemocentro.estado,
                telefone = chamado.hemocentro.telefone,
                titulo = chamado.titulo,
                tipoSanguineo = chamado.tipoSanguineo,
                urgencia = chamado.urgencia,
                metaDoadores = chamado.metaDoadores,
                respostasAtivas = respostasAtivas,
                mensagem = chamado.mensagem,
                status = chamado.status,
                criadoEm = chamado.criadoEm,
                atualizadoEm = chamado.atualizadoEm,
                encerradoEm = chamado.encerradoEm,
                versao = chamado.versao
            )
        }
    }
}

data class ChamadoInstitucionalResponse(
    val chamado: ChamadoResponse,
    val criadoPorUsuarioId: Long,
    val atualizadoPorUsuarioId: Long?,
    val encerradoPorUsuarioId: Long?,
    val motivoEncerramento: String?
) {
    companion object {
        fun from(
            chamado: ChamadoEmergencia,
            respostasAtivas: Long
        ): ChamadoInstitucionalResponse {
            return ChamadoInstitucionalResponse(
                chamado = ChamadoResponse.from(
                    chamado,
                    respostasAtivas
                ),
                criadoPorUsuarioId = chamado.criadoPor.id,
                atualizadoPorUsuarioId =
                    chamado.atualizadoPor?.id,
                encerradoPorUsuarioId =
                    chamado.encerradoPor?.id,
                motivoEncerramento =
                    chamado.motivoEncerramento
            )
        }
    }
}

data class MinhaRespostaChamadoResponse(
    val id: Long,
    val chamadoId: Long,
    val ativa: Boolean,
    val criadaEm: Instant,
    val atualizadaEm: Instant,
    val canceladaEm: Instant?,
    val tituloChamado: String,
    val hemocentroId: Long,
    val nomeHemocentro: String,
    val statusChamado: StatusChamado
) {
    companion object {
        fun from(
            resposta: RespostaChamado
        ): MinhaRespostaChamadoResponse {
            return MinhaRespostaChamadoResponse(
                id = resposta.id,
                chamadoId = resposta.chamado.id,
                ativa = resposta.ativa,
                criadaEm = resposta.criadaEm,
                atualizadaEm = resposta.atualizadaEm,
                canceladaEm = resposta.canceladaEm,
                tituloChamado = resposta.chamado.titulo,
                hemocentroId = resposta.chamado.hemocentro.id,
                nomeHemocentro = resposta.chamado.hemocentro.nome,
                statusChamado = resposta.chamado.status
            )
        }
    }
}

data class InteressadoChamadoResponse(
    val respostaId: Long,
    val doadorId: Long,
    val nome: String,
    val tipoSanguineoInformado: String,
    val respondeuEm: Instant
) {
    companion object {
        fun from(
            resposta: RespostaChamado
        ): InteressadoChamadoResponse {
            return InteressadoChamadoResponse(
                respostaId = resposta.id,
                doadorId = resposta.doador.id,
                nome = resposta.doador.nome,
                tipoSanguineoInformado =
                    resposta.doador.tipoSanguineo,
                respondeuEm = resposta.atualizadaEm
            )
        }
    }
}

data class PaginaChamadosResponse<T>(
    val itens: List<T>,
    val pagina: Int,
    val tamanho: Int,
    val totalElementos: Long,
    val totalPaginas: Int
)