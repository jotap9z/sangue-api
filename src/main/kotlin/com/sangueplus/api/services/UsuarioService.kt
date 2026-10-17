package com.sangueplus.api.services

import com.sangueplus.api.dtos.CadastroDoadorRequest
import com.sangueplus.api.dtos.UsuarioResponse
import com.sangueplus.api.models.Usuario
import com.sangueplus.api.repositories.UsuarioRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.Locale

@Service
class UsuarioService(
    private val repository: UsuarioRepository
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    private val tiposPermitidos = setOf(
        "A+", "A-",
        "B+", "B-",
        "AB+", "AB-",
        "O+", "O-",
        "Nao_Sei"
    )

    @Transactional
    fun cadastrarDoador(
        request: CadastroDoadorRequest
    ): UsuarioResponse {
        val nome = request.nome.trim()
        val email = request.email.trim().lowercase(Locale.ROOT)

        val telefone = request.telefone
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        validar(
            nome.length in 2..100,
            "Informe um nome entre 2 e 100 caracteres."
        )

        validar(
            email.length <= 100 &&
                    Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                        .matches(email),
            "Informe um e-mail válido."
        )

        validar(
            telefone == null || telefone.length <= 30,
            "O telefone deve ter no máximo 30 caracteres."
        )

        validar(
            request.senha.length >= 6 &&
                    request.senha.toByteArray(Charsets.UTF_8).size <= 72 &&
                    request.senha.any { it.isUpperCase() } &&
                    request.senha.any {
                        !it.isLetterOrDigit() && !it.isWhitespace()
                    },
            "A senha deve ter pelo menos 6 caracteres, " +
                    "uma letra maiúscula e um caractere especial, " +
                    "com limite de 72 bytes."
        )

        validar(
            request.tipoSanguineo in tiposPermitidos,
            "Tipo sanguíneo inválido. " +
                    "Use A+, A-, B+, B-, AB+, AB-, O+, O- ou Nao_Sei."
        )

        val hoje = LocalDate.now(
            ZoneId.of("America/Sao_Paulo")
        )

        validar(
            !request.dataNascimento.isAfter(hoje),
            "A data de nascimento não pode estar no futuro."
        )

        val idade = Period.between(
            request.dataNascimento,
            hoje
        ).years

        validar(
            idade >= 16,
            "O cadastro de doador está disponível a partir de 16 anos."
        )

        if (repository.existsByEmailIgnoreCase(email)) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Este e-mail já está cadastrado."
            )
        }

        val senhaHash = requireNotNull(
            passwordEncoder.encode(request.senha)
        ) {
            "Não foi possível gerar o hash da senha."
        }

        val usuario = Usuario(
            nome = nome,
            email = email,
            senhaHash = senhaHash,
            tipoUsuario = "doador",
            tipoSanguineo = request.tipoSanguineo,
            telefone = telefone,
            dataNascimento = request.dataNascimento
        )

        return UsuarioResponse.from(
            repository.saveAndFlush(usuario)
        )
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