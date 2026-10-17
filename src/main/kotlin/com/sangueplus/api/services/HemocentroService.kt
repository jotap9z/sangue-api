package com.sangueplus.api.services

import com.sangueplus.api.dtos.CadastroHemocentroRequest
import com.sangueplus.api.dtos.CadastroHemocentroResponse
import com.sangueplus.api.dtos.HemocentroResponse
import com.sangueplus.api.dtos.UsuarioResponse
import com.sangueplus.api.models.Hemocentro
import com.sangueplus.api.models.Usuario
import com.sangueplus.api.repositories.HemocentroRepository
import com.sangueplus.api.repositories.UsuarioRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.Locale

@Service
class HemocentroService(
    private val repository: HemocentroRepository,
    private val usuarios: UsuarioRepository,
    private val authService: AuthService
) {
    private val encoder = BCryptPasswordEncoder()

    private val estados = setOf(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF",
        "ES", "GO", "MA", "MT", "MS", "MG", "PA",
        "PB", "PR", "PE", "PI", "RJ", "RN", "RS",
        "RO", "RR", "SC", "SP", "SE", "TO"
    )

    @Transactional
    fun cadastrarHemocentro(
        request: CadastroHemocentroRequest
    ): CadastroHemocentroResponse {
        val nome = request.nome.trim()
        val endereco = request.endereco.trim()
        val cidade = request.cidade.trim()
        val estado = request.estado.trim().uppercase(Locale.ROOT)
        val telefone = request.telefone
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val responsavel = request.responsavel
        val nomeResponsavel = responsavel.nome.trim()
        val email = responsavel.email.trim().lowercase(Locale.ROOT)
        val telefoneResponsavel = responsavel.telefone
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        validar(
            nome.length in 2..150,
            "O nome do hemocentro deve ter entre 2 e 150 caracteres."
        )

        validar(
            endereco.length in 5..255,
            "O endereço deve ter entre 5 e 255 caracteres."
        )

        validar(
            cidade.length in 2..100,
            "A cidade deve ter entre 2 e 100 caracteres."
        )

        validar(
            estado in estados,
            "Informe uma UF válida, por exemplo MA."
        )

        validar(
            telefone == null || telefone.length <= 20,
            "O telefone do hemocentro deve ter no máximo 20 caracteres."
        )

        validar(
            nomeResponsavel.length in 2..100,
            "O nome do responsável deve ter entre 2 e 100 caracteres."
        )

        validar(
            email.length <= 100 &&
                    Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                        .matches(email),
            "Informe um e-mail válido para o responsável."
        )

        validar(
            telefoneResponsavel == null ||
                    telefoneResponsavel.length <= 30,
            "O telefone do responsável deve ter no máximo 30 caracteres."
        )

        validar(
            responsavel.senha.length >= 6 &&
                    responsavel.senha.toByteArray(Charsets.UTF_8).size <= 72 &&
                    responsavel.senha.any { it.isUpperCase() } &&
                    responsavel.senha.any {
                        !it.isLetterOrDigit() && !it.isWhitespace()
                    },
            "A senha deve ter pelo menos 6 caracteres, " +
                    "uma letra maiúscula e um caractere especial, " +
                    "com limite de 72 bytes."
        )

        if (repository.existsByNomeIgnoreCase(nome)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Hemocentro já existente"
            )
        }

        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Este e-mail já está cadastrado."
            )
        }

        val senhaHash = requireNotNull(
            encoder.encode(responsavel.senha)
        ) {
            "Não foi possível gerar o hash da senha."
        }

        val hemocentro = repository.saveAndFlush(
            Hemocentro(
                nome = nome,
                endereco = endereco,
                cidade = cidade,
                estado = estado,
                telefone = telefone
            )
        )

        val usuario = usuarios.saveAndFlush(
            Usuario(
                nome = nomeResponsavel,
                email = email,
                senhaHash = senhaHash,
                tipoUsuario = "hemocentro",
                tipoSanguineo = "Nao_Sei",
                telefone = telefoneResponsavel,
                hemocentro = hemocentro
            )
        )

        return CadastroHemocentroResponse(
            hemocentro = HemocentroResponse.from(hemocentro),
            responsavel = UsuarioResponse.from(usuario)
        )
    }

    @Transactional(readOnly = true)
    fun listarHemocentros(
        cidade: String,
        estado: String
    ): List<HemocentroResponse> {
        val cidadeNormalizada = cidade.trim()
        val estadoNormalizado = estado.trim().uppercase(Locale.ROOT)

        validar(
            cidadeNormalizada.length <= 100,
            "O filtro de cidade deve ter no máximo 100 caracteres."
        )

        validar(
            estadoNormalizado.isEmpty() || estadoNormalizado in estados,
            "Informe uma UF válida para pesquisar."
        )

        return repository.buscarPorLocalizacao(
            cidadeNormalizada,
            estadoNormalizado
        ).map {
            HemocentroResponse.from(it)
        }
    }

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): HemocentroResponse {
        val hemocentro = repository.findById(id)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hemocentro não encontrado."
                )
            }

        return HemocentroResponse.from(hemocentro)
    }

    @Transactional(readOnly = true)
    fun meuHemocentro(
        authorization: String?
    ): HemocentroResponse {
        val usuario = authService.exigirHemocentro(authorization)

        val hemocentro = usuario.hemocentro
            ?: throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Sua conta não está vinculada a um hemocentro."
            )

        return HemocentroResponse.from(hemocentro)
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