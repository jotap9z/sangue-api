package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.UsuarioResponse
import com.sangueplus.api.models.Usuario
import com.sangueplus.api.services.AuthService
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

data class AtualizarDadosContaRequest(
    val nome: String,
    val telefone: String? = null
)

data class AlterarSenhaContaRequest(
    val senhaAtual: String,
    val novaSenha: String
)

data class MensagemContaResponse(
    val mensagem: String
)

@RestController
@RequestMapping("/api/conta/me")
class ContaController(
    private val service: DadosContaService
) {

    @PutMapping
    fun atualizarDados(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestBody request: AtualizarDadosContaRequest
    ): UsuarioResponse {
        return service.atualizarDados(
            authorization,
            request
        )
    }

    @PostMapping("/senha")
    fun alterarSenha(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestBody request: AlterarSenhaContaRequest
    ): MensagemContaResponse {
        return service.alterarSenha(
            authorization,
            request
        )
    }
}

@Service
class DadosContaService(
    private val auth: AuthService,
    private val entityManager: EntityManager
) {

    private val encoder = BCryptPasswordEncoder()

    @Transactional
    fun atualizarDados(
        authorization: String?,
        request: AtualizarDadosContaRequest
    ): UsuarioResponse {
        val autenticado = auth.exigirHemocentro(authorization)
        val usuario = bloquearUsuario(autenticado.id)

        val nome = request.nome.trim()
        validar(
            nome.length in 2..100,
            "O nome deve ter entre 2 e 100 caracteres."
        )

        val telefoneInformado = request.telefone
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val telefone = telefoneInformado?.let {
            validar(
                it.length <= 30 &&
                        Regex("^[0-9()\\s+.-]+$").matches(it),
                "Informe um telefone válido."
            )

            val digitos = it.filter { caractere ->
                caractere in '0'..'9'
            }

            validar(
                digitos.length in 10..11,
                "Informe o telefone com DDD e 10 ou 11 dígitos."
            )

            if (digitos.length == 11) {
                "(${digitos.take(2)}) " +
                        "${digitos.substring(2, 7)}-" +
                        digitos.substring(7)
            } else {
                "(${digitos.take(2)}) " +
                        "${digitos.substring(2, 6)}-" +
                        digitos.substring(6)
            }
        }

        usuario.nome = nome
        usuario.telefone = telefone

        entityManager.flush()

        return UsuarioResponse.from(usuario)
    }

    @Transactional
    fun alterarSenha(
        authorization: String?,
        request: AlterarSenhaContaRequest
    ): MensagemContaResponse {
        val autenticado = auth.usuarioAtual(authorization)
        val usuario = bloquearUsuario(autenticado.id)

        validar(
            request.senhaAtual.isNotBlank() &&
                    request.senhaAtual
                        .toByteArray(Charsets.UTF_8).size <= 72,
            "Informe sua senha atual."
        )

        validar(
            encoder.matches(
                request.senhaAtual,
                usuario.senhaHash
            ),
            "A senha atual está incorreta."
        )

        val novaSenha = request.novaSenha

        validar(
            novaSenha.length >= 6 &&
                    novaSenha.toByteArray(Charsets.UTF_8).size <= 72 &&
                    novaSenha.any { it.isUpperCase() } &&
                    novaSenha.any {
                        !it.isLetterOrDigit() &&
                                !it.isWhitespace()
                    },
            "A nova senha deve ter pelo menos 6 caracteres, " +
                    "uma letra maiúscula e um caractere especial, " +
                    "com limite de 72 bytes."
        )

        validar(
            !encoder.matches(novaSenha, usuario.senhaHash),
            "A nova senha deve ser diferente da senha atual."
        )

        usuario.senhaHash = requireNotNull(
            encoder.encode(novaSenha)
        )

        entityManager.flush()

        entityManager.createQuery(
            "delete from Sessao s where s.usuario.id = :usuarioId"
        )
            .setParameter("usuarioId", usuario.id)
            .executeUpdate()

        return MensagemContaResponse(
            mensagem = "Senha alterada. Entre novamente " +
                    "com sua nova senha."
        )
    }

    private fun bloquearUsuario(id: Long): Usuario {
        val usuario = entityManager.find(
            Usuario::class.java,
            id,
            LockModeType.PESSIMISTIC_WRITE
        ) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Conta não encontrada."
        )

        // Recarrega os dados após obter o bloqueio.
        entityManager.refresh(
            usuario,
            LockModeType.PESSIMISTIC_WRITE
        )

        return usuario
    }

    private fun validar(
        condicao: Boolean,
        mensagem: String
    ) {
        if (!condicao) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                mensagem
            )
        }
    }
}