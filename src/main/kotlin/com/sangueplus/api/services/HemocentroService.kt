package com.sangueplus.api.services

import com.sangueplus.api.dtos.*
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
        val nome = texto(
            request.nome,
            2,
            150,
            "nome do hemocentro"
        )

        val endereco = texto(
            request.endereco,
            5,
            255,
            "endereço"
        )

        val cidade = texto(
            request.cidade,
            2,
            100,
            "nome da cidade"
        )

        val estado = validarEstado(request.estado)
        val telefone = telefone(request.telefone, 20)
        val cnpj = normalizarCnpj(request.cnpj)
        val cnes = normalizarCnes(request.cnes)
        val emailContato = emailOpcional(request.emailContato)

        val responsavel = request.responsavel

        val nomeResponsavel = texto(
            responsavel.nome,
            2,
            100,
            "nome do responsável"
        )

        val email = emailObrigatorio(responsavel.email)

        val telefoneResponsavel = telefone(
            responsavel.telefone,
            30
        )

        validarSenha(responsavel.senha)

        if (repository.existsByNomeIgnoreCase(nome)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Hemocentro já existente"
            )
        }

        if (usuarios.existsByEmailIgnoreCase(email)) {
            conflito("Este e-mail já está cadastrado.")
        }

        verificarDocumentosDisponiveis(cnpj, cnes, 0)

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
                telefone = telefone,
                cnpj = cnpj,
                cnes = cnes,
                emailContato = emailContato
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
        val estadoNormalizado = estado.trim()
            .uppercase(Locale.ROOT)

        validar(
            cidadeNormalizada.length <= 100,
            "O filtro de cidade deve ter no máximo 100 caracteres."
        )

        validar(
            estadoNormalizado.isEmpty() ||
                    estadoNormalizado in estados,
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
        val usuario = authService.exigirHemocentro(
            authorization
        )

        val hemocentro = usuario.hemocentro
            ?: throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Sua conta não está vinculada a um hemocentro."
            )

        return HemocentroResponse.from(hemocentro)
    }

    @Transactional
    fun atualizarMeuHemocentro(
        authorization: String?,
        request: AtualizarHemocentroRequest
    ): HemocentroResponse {
        val usuario = authService.exigirHemocentro(
            authorization
        )

        val hemocentroId = usuario.hemocentro?.id
            ?: throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Sua conta não está vinculada a um hemocentro."
            )

        val hemocentro = repository.buscarParaAlteracao(
            hemocentroId
        ) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Hemocentro não encontrado."
        )

        val nome = texto(
            request.nome,
            2,
            150,
            "nome do hemocentro"
        )

        val endereco = texto(
            request.endereco,
            5,
            255,
            "endereço"
        )

        val cidade = texto(
            request.cidade,
            2,
            100,
            "nome da cidade"
        )

        val estado = validarEstado(request.estado)
        val telefone = telefone(request.telefone, 20)
        val emailContato = emailOpcional(request.emailContato)

        val cnpjInformado = normalizarCnpj(request.cnpj)
        val cnesInformado = normalizarCnes(request.cnes)

        if (
            hemocentro.cnpj != null &&
            cnpjInformado != null &&
            cnpjInformado != hemocentro.cnpj
        ) {
            conflito(
                "O CNPJ já registrado não pode ser alterado por este endpoint."
            )
        }

        val cnpjFinal = cnpjInformado ?: hemocentro.cnpj
        val cnesFinal = cnesInformado ?: hemocentro.cnes

        if (
            repository.existsByNomeIgnoreCaseAndIdNot(
                nome,
                hemocentro.id
            )
        ) {
            conflito(
                "Já existe outro hemocentro com esse nome."
            )
        }

        verificarDocumentosDisponiveis(
            cnpjFinal,
            cnesFinal,
            hemocentro.id
        )

        hemocentro.nome = nome
        hemocentro.endereco = endereco
        hemocentro.cidade = cidade
        hemocentro.estado = estado
        hemocentro.telefone = telefone
        hemocentro.emailContato = emailContato
        hemocentro.cnpj = cnpjFinal
        hemocentro.cnes = cnesFinal

        return HemocentroResponse.from(
            repository.saveAndFlush(hemocentro)
        )
    }

    private fun verificarDocumentosDisponiveis(
        cnpj: String?,
        cnes: String?,
        idAtual: Long
    ) {
        if (
            cnpj != null &&
            repository.existsByCnpjAndIdNot(cnpj, idAtual)
        ) {
            conflito("Este CNPJ já está cadastrado.")
        }

        if (
            cnes != null &&
            repository.existsByCnesAndIdNot(cnes, idAtual)
        ) {
            conflito("Este CNES já está cadastrado.")
        }
    }

    private fun normalizarCnpj(valor: String?): String? {
        val informado = valor?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val normalizado = informado
            .replace(Regex("[.\\-/\\s]"), "")
            .uppercase(Locale.ROOT)

        validar(
            Regex("^[A-Z0-9]{14}$").matches(normalizado),
            "Informe o CNPJ com 14 caracteres, sem contar a pontuação."
        )

        return normalizado
    }

    private fun normalizarCnes(valor: String?): String? {
        val cnes = valor?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        validar(
            Regex("^[0-9]{7}$").matches(cnes),
            "Informe o CNES com 7 dígitos."
        )

        return cnes
    }

    private fun texto(
        valor: String,
        minimo: Int,
        maximo: Int,
        campo: String
    ): String {
        val normalizado = valor.trim()

        validar(
            normalizado.length in minimo..maximo,
            "O $campo deve ter entre $minimo e $maximo caracteres."
        )

        return normalizado
    }

    private fun validarEstado(valor: String): String {
        val estado = valor.trim().uppercase(Locale.ROOT)

        validar(
            estado in estados,
            "Informe uma UF válida, por exemplo MA."
        )

        return estado
    }

    private fun telefone(
        valor: String?,
        limite: Int
    ): String? {
        val telefone = valor?.trim()
            ?.takeIf { it.isNotEmpty() }

        validar(
            telefone == null || telefone.length <= limite,
            "O telefone deve ter no máximo $limite caracteres."
        )

        return telefone
    }

    private fun emailObrigatorio(valor: String): String {
        val email = valor.trim().lowercase(Locale.ROOT)

        validar(
            email.length <= 100 &&
                    Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                        .matches(email),
            "Informe um e-mail válido."
        )

        return email
    }

    private fun emailOpcional(valor: String?): String? {
        val email = valor?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        return emailObrigatorio(email)
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