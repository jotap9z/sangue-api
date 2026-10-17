package com.sangueplus.api.dtos

import com.sangueplus.api.models.PreTriagem
import java.math.BigDecimal
import java.time.Instant

data class PreTriagemRequest(
    val pesoKg: BigDecimal? = null,
    val bemDeSaude: Boolean? = null,
    val sintomasRecentes: Boolean? = null,
    val cirurgiaRecente: Boolean? = null,
    val tatuagemOuPiercingRecente: Boolean? = null,
    val gravidaOuAmamentando: Boolean? = null,
    val condicaoDeSaudeOuMedicamento: Boolean? = null,
    val descansadoEAlimentado: Boolean? = null,
    val autorizacaoResponsavel: Boolean? = null,
    val observacoes: String? = null
)

enum class ResultadoPreTriagem {
    SEM_ALERTAS_INICIAIS,
    NECESSITA_AVALIACAO
}

data class PreTriagemResponse(
    val id: Long,
    val resultado: ResultadoPreTriagem,
    val mensagem: String,
    val motivos: List<String>,
    val avaliadaEm: Instant,
    val versaoQuestionario: String,
    val aviso: String
) {
    companion object {
        fun from(triagem: PreTriagem): PreTriagemResponse {
            return PreTriagemResponse(
                id = triagem.id,
                resultado = triagem.resultado,
                mensagem = when (triagem.resultado) {
                    ResultadoPreTriagem.SEM_ALERTAS_INICIAIS ->
                        "Nenhum alerta foi identificado nas respostas " +
                                "deste questionário inicial."

                    ResultadoPreTriagem.NECESSITA_AVALIACAO ->
                        "Procure orientação do hemocentro antes da doação."
                },
                motivos = triagem.motivos.toList(),
                avaliadaEm = triagem.avaliadaEm,
                versaoQuestionario = triagem.versaoQuestionario,
                aviso = "Esta pré-triagem não confirma aptidão para doar. " +
                        "A decisão depende da avaliação presencial e dos " +
                        "critérios aplicados pelo serviço de hemoterapia."
            )
        }
    }
}