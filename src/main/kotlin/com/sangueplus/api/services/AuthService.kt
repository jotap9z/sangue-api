package com.sangueplus.api.services

import com.sangueplus.api.dtos.LoginRequest
import com.sangueplus.api.dtos.LoginResponse
import com.sangueplus.api.dtos.UsuarioResponse
import com.sangueplus.api.models.Sessao
import com.sangueplus.api.models.Usuario
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

    private val hashSimulado = requireNotNull(
        encoder.encode("SenhaSimulada!123")
    )

    private val formatoBcrypt =
        Regex("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$")

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

        // Contas antigas com senha sem hash não são autenticadas.
        val hashArmazenado = usuario?.senhaHash
        val hashValido = hashArmazenado != null &&
                formatoBcrypt.matches(hashArmazenado)

        val senhaCorreta = encoder.matches(
            request.senha,
            if (hashValido) hashArmazenado else hashSimulado
        )

        if (usuario == null || !hashValido || !senhaCorreta) {
            naoAutorizado("E-mail ou senha inválidos.")
        }

        if (
            usuario.tipoUsuario != "doador" &&
            usuario.tipoUsuario != "hemocentro"
        ) {
            proibido("Perfil de acesso inválido.")
        }

        if (
            usuario.tipoUsuario == "hemocentro" &&
            usuario.hemocentro == null
        ) {
            proibido("Sua conta não está vinculada a um hemocentro.")
        }

        val bytes = ByteArray(32)
        random.nextBytes(bytes)

        val token = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)

        val expiraEm = Instant.now().plus(8, ChronoUnit.HOURS)

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
        return UsuarioResponse.from(
            obterSessao(authorization).usuario
        )
    }

    @Transactional(readOnly = true)
    fun exigirDoador(authorization: String?): Usuario {
        val usuario = obterSessao(authorization).usuario

        if (usuario.tipoUsuario != "doador") {
            proibido("Esta operação é exclusiva para doadores.")
        }

        return usuario
    }

    @Transactional(readOnly = true)
    fun exigirHemocentro(authorization: String?): Usuario {
        val usuario = obterSessao(authorization).usuario

        if (
            usuario.tipoUsuario != "hemocentro" ||
            usuario.hemocentro == null
        ) {
            proibido("Esta operação é exclusiva para hemocentros.")
        }

        return usuario
    }

    @Transactional
    fun logout(authorization: String?) {
        sessoes.delete(obterSessao(authorization))
    }

    private fun obterSessao(authorization: String?): Sessao {
        val partes = authorization
            ?.trim()
            ?.split(Regex("\\s+"))

        if (
            partes == null ||
            partes.size != 2 ||
            !partes[0].equals("Bearer", ignoreCase = true) ||
            !Regex("^[A-Za-z0-9_-]{43}$").matches(partes[1])
        ) {
            naoAutorizado("Entre na sua conta para continuar.")
        }

        val sessao = sessoes.findByTokenHash(
            hash(partes[1])
        ) ?: naoAutorizado("Sua sessão é inválida. Entre novamente.")

        if (!sessao.expiraEm.isAfter(Instant.now())) {
            naoAutorizado("Sua sessão expirou. Entre novamente.")
        }

        return sessao
    }

    private fun hash(token: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            }
    }

    private fun naoAutorizado(mensagem: String): Nothing {
        throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            mensagem
        )
    }

    private fun proibido(mensagem: String): Nothing {
        throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            mensagem
        )
    }
}