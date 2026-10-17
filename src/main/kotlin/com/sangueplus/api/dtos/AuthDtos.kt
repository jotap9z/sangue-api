package com.sangueplus.api.dtos

import com.sangueplus.api.models.Usuario
import java.time.Instant
import java.time.LocalDate

data class CadastroDoadorRequest(
    val nome: String,
    val email: String,
    val senha: String,
    val dataNascimento: LocalDate,
    val tipoSanguineo: String = "Nao_Sei",
    val telefone: String? = null
)

data class LoginRequest(
    val email: String,
    val senha: String
)

data class UsuarioResponse(
    val id: Long,
    val nome: String,
    val email: String,
    val tipoUsuario: String,
    val tipoSanguineo: String,
    val telefone: String?,
    val dataNascimento: LocalDate?
) {
    companion object {
        fun from(usuario: Usuario): UsuarioResponse =
            UsuarioResponse(
                id = usuario.id,
                nome = usuario.nome,
                email = usuario.email,
                tipoUsuario = usuario.tipoUsuario,
                tipoSanguineo = usuario.tipoSanguineo,
                telefone = usuario.telefone,
                dataNascimento = usuario.dataNascimento
            )
    }
}

data class LoginResponse(
    val token: String,
    val tipoToken: String = "Bearer",
    val expiraEm: Instant,
    val usuario: UsuarioResponse
)