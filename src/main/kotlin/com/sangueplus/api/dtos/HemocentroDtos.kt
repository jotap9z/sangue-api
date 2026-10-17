package com.sangueplus.api.dtos

import com.sangueplus.api.models.Hemocentro

data class CadastroHemocentroRequest(
    val nome: String,
    val endereco: String,
    val cidade: String,
    val estado: String,
    val telefone: String? = null,
    val responsavel: ResponsavelHemocentroRequest
)

data class ResponsavelHemocentroRequest(
    val nome: String,
    val email: String,
    val senha: String,
    val telefone: String? = null
)

data class HemocentroResponse(
    val id: Long,
    val nome: String,
    val endereco: String,
    val cidade: String,
    val estado: String,
    val telefone: String?
) {
    companion object {
        fun from(hemocentro: Hemocentro): HemocentroResponse =
            HemocentroResponse(
                id = hemocentro.id,
                nome = hemocentro.nome,
                endereco = hemocentro.endereco,
                cidade = hemocentro.cidade,
                estado = hemocentro.estado,
                telefone = hemocentro.telefone
            )
    }
}

data class CadastroHemocentroResponse(
    val hemocentro: HemocentroResponse,
    val responsavel: UsuarioResponse
)