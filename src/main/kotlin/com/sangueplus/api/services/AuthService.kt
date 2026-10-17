package com.sangueplus.api.services

import com.sangueplus.api.dtos.LoginRequest
import com.sangueplus.api.dtos.LoginResponse
import com.sangueplus.api.dtos.UsuarioResponse
import com.sangueplus.api.models.Sessao
import com.sangueplus.api.repositories.SessaoRepository
import com.sangueplus.api.repositories.UsuarioRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Locale

@Service
class AuthService(
    private val usuarios: UsuarioRepository,
    private val sessoes: SessaoRepository
) {
    private val encoder = BCryptPasswordEncoder()
    private val random = SecureRandom()

    private val hashSimulado =
        encoder.encode("SenhaSimulada!123")

    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val email = request.email.trim().lowercase(Locale.ROOT)

        if (
            email.isBlank() ||
            email.length > 100 ||
            request.senha.isBlank() ||
            request.senha.toByteArray(Charsets.UTF_8).size > 72
        ) {
            naoAutorizado("E-mail ou senha inválidos.")
        }

        val usuario = usuarios.findByEmailIgnoreCase(email)

        val senhaCorreta = encoder.matches(
            request.senha,
            usuario?.senhaHash ?: hashSimulado
        )

        if (usuario == null || !senhaCorreta) {
            naoAutorizado("E-mail ou senha inválidos.")
        }

        val bytes = ByteArray(32)
        random.nextBytes(bytes)

        val token = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)

        val expiraEm = Instant.now()
            .plus(8, ChronoUnit.HOURS)

        sessoes.save(
            Sessao(
                usuario = usuario,
                tokenHash = hash(token),
                expiraEm = expiraEm
            )
        )

        return LoginResponse(
            token = token,
            expiraEm = expiraEm,
            usuario = UsuarioResponse.from(usuario)
        )
    }

    @Transactional(readOnly = true)
    fun usuarioAtual(
        authorization: String?
    ): UsuarioResponse {
        val sessao = obterSessao(authorization)

        return UsuarioResponse.from(sessao.usuario)
    }

    @Transactional
    fun logout(authorization: String?) {
        val sessao = obterSessao(authorization)
        sessoes.delete(sessao)
    }

    private fun obterSessao(
        authorization: String?
    ): Sessao {
        val partes = authorization
            ?.trim()
            ?.split(Regex("\\s+"))

        if (
            partes == null ||
            partes.size != 2 ||
            !partes[0].equals("Bearer", ignoreCase = true) ||
            !Regex("^[A-Za-z0-9_-]{43}$").matches(partes[1])
        ) {
            naoAutorizado("Informe um token Bearer válido.")
        }

        val sessao = sessoes.findByTokenHash(
            hash(partes[1])
        ) ?: naoAutorizado("Sessão inválida.")

        if (!sessao.expiraEm.isAfter(Instant.now())) {
            naoAutorizado("Sessão expirada. Entre novamente.")
        }

        return sessao
    }

    private fun hash(token: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }

    private fun naoAutorizado(
        mensagem: String
    ): Nothing {
        throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            mensagem
        )
    }
}