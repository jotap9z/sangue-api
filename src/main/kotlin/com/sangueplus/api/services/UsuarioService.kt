package com.sangueplus.api.services

import com.sangueplus.api.dtos.AtualizarPerfilDoadorRequest
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
    private val repository: UsuarioRepository,
    private val authService: AuthService
) {

    private val encoder = BCryptPasswordEncoder()

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
        val nome = validarNome(request.nome)
        val email = validarEmail(request.email)
        val telefone = validarTelefone(request.telefone)
        val cidade = validarCidade(request.cidade)
        val cpf = validarCpf(request.cpf)

        validarSenha(request.senha)

        validar(
            request.tipoSanguineo in tiposPermitidos,
            "Tipo sanguíneo inválido. Use A+, A-, B+, B-, " +
                    "AB+, AB-, O+, O- ou Nao_Sei."
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
            conflito("Este e-mail já está cadastrado.")
        }

        if (cpf != null && repository.existsByCpf(cpf)) {
            conflito("Este CPF já está cadastrado.")
        }

        val senhaHash = requireNotNull(
            encoder.encode(request.senha)
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
            dataNascimento = request.dataNascimento,
            cpf = cpf,
            cidade = cidade
        )

        return UsuarioResponse.from(
            repository.saveAndFlush(usuario)
        )
    }

    @Transactional(readOnly = true)
    fun consultarMeuPerfil(
        authorization: String?
    ): UsuarioResponse {
        return UsuarioResponse.from(
            authService.exigirDoador(authorization)
        )
    }

    @Transactional
    fun atualizarMeuPerfil(
        authorization: String?,
        request: AtualizarPerfilDoadorRequest
    ): UsuarioResponse {
        val autenticado = authService.exigirDoador(
            authorization
        )

        val usuario = repository.buscarParaAgendamento(
            autenticado.id
        ) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Usuário não encontrado."
        )

        val nome = validarNome(request.nome)
        val email = validarEmail(request.email)
        val telefone = validarTelefone(request.telefone)
        val cidade = validarCidade(request.cidade)

        // Omissão ou valor vazio não remove o CPF existente.
        val cpfInformado = validarCpf(request.cpf)
        val cpfFinal = cpfInformado ?: usuario.cpf

        if (
            usuario.cpf != null &&
            cpfInformado != null &&
            cpfInformado != usuario.cpf
        ) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "O CPF já registrado não pode ser alterado por este endpoint."
            )
        }

        if (
            cpfFinal != null &&
            repository.existsByCpfAndIdNot(
                cpfFinal,
                usuario.id
            )
        ) {
            conflito("Este CPF já está cadastrado.")
        }

        if (
            repository.existsByEmailIgnoreCaseAndIdNot(
                email,
                usuario.id
            )
        ) {
            conflito("Este e-mail já está cadastrado.")
        }

        if (!email.equals(usuario.email, ignoreCase = true)) {
            val senhaAtual = request.senhaAtual

            validar(
                senhaAtual != null &&
                        senhaAtual.isNotBlank() &&
                        senhaAtual.toByteArray(Charsets.UTF_8).size <= 72,
                "Informe sua senha atual para alterar o e-mail."
            )

            val senhaCorreta = runCatching {
                encoder.matches(
                    requireNotNull(senhaAtual),
                    usuario.senhaHash
                )
            }.getOrDefault(false)

            validar(
                senhaCorreta,
                "Senha atual incorreta."
            )
        }

        usuario.nome = nome
        usuario.email = email
        usuario.telefone = telefone
        usuario.cidade = cidade
        usuario.cpf = cpfFinal

        return UsuarioResponse.from(
            repository.saveAndFlush(usuario)
        )
    }

    private fun validarNome(valor: String): String {
        val nome = valor.trim()

        validar(
            nome.length in 2..100,
            "Informe um nome entre 2 e 100 caracteres."
        )

        return nome
    }

    private fun validarEmail(valor: String): String {
        val email = valor.trim().lowercase(Locale.ROOT)

        validar(
            email.length <= 100 &&
                    Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                        .matches(email),
            "Informe um e-mail válido."
        )

        return email
    }

    private fun validarTelefone(valor: String?): String? {
        val telefone = valor
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        validar(
            telefone == null || telefone.length <= 30,
            "O telefone deve ter no máximo 30 caracteres."
        )

        return telefone
    }

    private fun validarCidade(valor: String?): String? {
        val cidade = valor
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        validar(
            cidade == null || cidade.length in 2..100,
            "A cidade deve ter entre 2 e 100 caracteres."
        )

        return cidade
    }

    private fun validarSenha(senha: String) {
        validar(
            senha.length >= 6 &&
                    senha.toByteArray(Charsets.UTF_8).size <= 72 &&
                    senha.any { it.isUpperCase() } &&
                    senha.any {
                        !it.isLetterOrDigit() &&
                                !it.isWhitespace()
                    },
            "A senha deve ter pelo menos 6 caracteres, " +
                    "uma letra maiúscula e um caractere especial, " +
                    "com limite de 72 bytes."
        )
    }

    private fun validarCpf(valor: String?): String? {
        val informado = valor
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        validar(
            Regex("^[0-9.\\-\\s]+$").matches(informado),
            "Informe um CPF válido."
        )

        val cpf = informado.filter { it in '0'..'9' }

        validar(
            cpf.length == 11 &&
                    cpf.toSet().size > 1,
            "Informe um CPF válido."
        )

        fun digito(tamanho: Int): Int {
            val soma = (0 until tamanho).sumOf { indice ->
                cpf[indice].digitToInt() *
                        (tamanho + 1 - indice)
            }

            val resto = soma % 11
            return if (resto < 2) 0 else 11 - resto
        }

        validar(
            cpf[9].digitToInt() == digito(9) &&
                    cpf[10].digitToInt() == digito(10),
            "Informe um CPF válido."
        )

        return cpf
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

    private fun conflito(mensagem: String): Nothing {
        throw ResponseStatusException(
            HttpStatus.CONFLICT,
            mensagem
        )
    }
}