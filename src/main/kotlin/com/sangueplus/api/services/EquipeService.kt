package com.sangueplus.api.services

import com.sangueplus.api.dtos.*
import com.sangueplus.api.models.*
import com.sangueplus.api.repositories.*
import jakarta.persistence.EntityManager
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
class EquipeService(
    private val auth: AuthService,
    private val acesso: AcessoEquipeService,
    private val membros: VinculoEquipeRepository,
    private val convites: ConviteEquipeRepository,
    private val usuarios: UsuarioRepository,
    private val hemocentros: HemocentroRepository,
    private val entityManager: EntityManager
) {

    private val encoder = BCryptPasswordEncoder()
    private val random = SecureRandom()

    @Transactional
    fun inicializar(
        authorization: String?
    ): MembroEquipeResponse {
        val usuario = auth.exigirHemocentro(authorization)
        val hemocentro = bloquearHemocentro(
            requireNotNull(usuario.hemocentro).id
        )

        if (membros.existsByHemocentro_Id(hemocentro.id)) {
            conflito("A equipe deste hemocentro já foi inicializada.")
        }

        if (
            membros.contarContasInstitucionais(
                hemocentro.id
            ) != 1L
        ) {
            conflito(
                "A inicialização automática exige exatamente uma " +
                        "conta institucional neste hemocentro. " +
                        "Não foi escolhido um administrador automaticamente."
            )
        }

        val membro = membros.saveAndFlush(
            VinculoEquipe(
                usuario = usuario,
                hemocentro = hemocentro,
                funcao = FuncaoEquipe.ADMINISTRADOR,
                permissao = PermissaoEquipe.ADMINISTRADOR,
                administradorPrincipal = true
            )
        )

        return MembroEquipeResponse.from(membro)
    }

    @Transactional(readOnly = true)
    fun meuVinculo(
        authorization: String?
    ): MembroEquipeResponse {
        return MembroEquipeResponse.from(
            acesso.exigirMembro(authorization)
        )
    }

    @Transactional(readOnly = true)
    fun listar(
        authorization: String?
    ): EquipeResponse {
        val administrador = acesso.exigirAdministrador(
            authorization
        )

        val id = administrador.hemocentro.id

        return EquipeResponse(
            membros = membros
                .findByHemocentro_IdOrderByUsuario_NomeAsc(id)
                .map { MembroEquipeResponse.from(it) },
            convites = convites
                .findByHemocentro_IdOrderByCriadoEmDesc(id)
                .map { ConviteEquipeResponse.from(it) }
        )
    }

    @Transactional
    fun convidar(
        authorization: String?,
        request: CriarConviteEquipeRequest
    ): ConviteEquipeCriadoResponse {
        val administrador = administradorBloqueado(
            authorization
        )

        val nome = request.nome.trim()
        val email = request.email.trim().lowercase(Locale.ROOT)
        val telefone = opcional(request.telefone, 30, "telefone")
        val cargo = opcional(request.cargo, 100, "cargo")

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

        validarFuncao(request.funcao, request.permissao)

        val existente = usuarios.findByEmailIgnoreCase(email)

        if (existente != null) {
            if (
                existente.tipoUsuario != "hemocentro" ||
                existente.hemocentro?.id != administrador.hemocentro.id
            ) {
                conflito(
                    "Este e-mail já pertence a outra conta. " +
                            "Use um e-mail disponível para o funcionário."
                )
            }

            if (membros.findByUsuario_Id(existente.id) != null) {
                conflito("Este usuário já possui vínculo de equipe.")
            }
        }

        if (
            convites.existeConvitePendente(
                administrador.hemocentro.id,
                email,
                Instant.now()
            )
        ) {
            conflito("Já existe um convite pendente para esse e-mail.")
        }

        val codigo = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                ByteArray(32).also {
                    random.nextBytes(it)
                }
            )

        val convite = convites.saveAndFlush(
            ConviteEquipe(
                hemocentro = administrador.hemocentro,
                convidadoPor = administrador.usuario,
                nome = nome,
                email = email,
                telefone = telefone,
                cargo = cargo,
                funcao = request.funcao,
                permissao = request.permissao,
                codigoHash = hash(codigo),
                expiraEm = Instant.now().plus(
                    48,
                    ChronoUnit.HOURS
                )
            )
        )

        return ConviteEquipeCriadoResponse(
            convite = ConviteEquipeResponse.from(convite),
            codigoConvite = codigo
        )
    }

    @Transactional
    fun cancelarConvite(
        authorization: String?,
        id: Long
    ): ConviteEquipeResponse {
        val administrador = administradorBloqueado(
            authorization
        )

        val convite = convites.buscarParaAlteracao(id)
            ?: naoEncontrado("Convite não encontrado.")

        entityManager.refresh(convite)

        if (
            convite.hemocentro.id != administrador.hemocentro.id
        ) {
            naoEncontrado("Convite não encontrado.")
        }

        if (convite.aceitoEm != null) {
            conflito("Um convite aceito não pode ser cancelado.")
        }

        if (convite.canceladoEm == null) {
            convite.canceladoEm = Instant.now()
            convites.saveAndFlush(convite)
        }

        return ConviteEquipeResponse.from(convite)
    }

    @Transactional
    fun aceitarConvite(
        request: AceitarConviteEquipeRequest
    ): MembroEquipeResponse {
        val codigo = request.codigo.trim()

        validar(
            Regex("^[A-Za-z0-9_-]{43}$").matches(codigo),
            "Código de convite inválido."
        )

        val encontrado = convites.findByCodigoHash(hash(codigo))
            ?: naoEncontrado("Convite inválido.")

        bloquearHemocentro(encontrado.hemocentro.id)

        val convite = convites.buscarParaAlteracao(
            encontrado.id
        ) ?: naoEncontrado("Convite inválido.")

        entityManager.refresh(convite)

        if (
            convite.canceladoEm != null ||
            convite.aceitoEm != null ||
            !convite.expiraEm.isAfter(Instant.now())
        ) {
            conflito("O convite foi utilizado, cancelado ou expirou.")
        }

        val existente = usuarios.findByEmailIgnoreCase(
            convite.email
        )

        val usuario = if (existente == null) {
            validarSenha(request.senha)

            usuarios.saveAndFlush(
                Usuario(
                    nome = convite.nome,
                    email = convite.email,
                    senhaHash = requireNotNull(
                        encoder.encode(request.senha)
                    ),
                    tipoUsuario = "hemocentro",
                    telefone = convite.telefone,
                    hemocentro = convite.hemocentro
                )
            )
        } else {
            if (
                existente.tipoUsuario != "hemocentro" ||
                existente.hemocentro?.id != convite.hemocentro.id
            ) {
                conflito("Este e-mail já pertence a outra conta.")
            }

            if (membros.findByUsuario_Id(existente.id) != null) {
                conflito("Este usuário já possui vínculo de equipe.")
            }

            validar(
                request.senha.isNotBlank() &&
                        request.senha.toByteArray(Charsets.UTF_8).size <= 72,
                "Informe a senha atual desta conta."
            )

            val correta = runCatching {
                encoder.matches(
                    request.senha,
                    existente.senhaHash
                )
            }.getOrDefault(false)

            validar(correta, "Senha da conta incorreta.")

            existente
        }

        val membro = membros.saveAndFlush(
            VinculoEquipe(
                usuario = usuario,
                hemocentro = convite.hemocentro,
                funcao = convite.funcao,
                permissao = convite.permissao,
                cargo = convite.cargo
            )
        )

        convite.aceitoEm = Instant.now()
        convites.saveAndFlush(convite)

        return MembroEquipeResponse.from(membro)
    }

    @Transactional
    fun atualizarFuncao(
        authorization: String?,
        usuarioId: Long,
        request: AtualizarFuncaoEquipeRequest
    ): MembroEquipeResponse {
        val administrador = administradorBloqueado(
            authorization
        )

        val membro = membroDaInstituicao(
            usuarioId,
            administrador.hemocentro.id
        )

        validarFuncao(request.funcao, request.permissao)

        if (
            membro.administradorPrincipal &&
            (
                    request.funcao != FuncaoEquipe.ADMINISTRADOR ||
                            request.permissao != PermissaoEquipe.ADMINISTRADOR
                    )
        ) {
            conflito(
                "Transfira a administração principal antes de " +
                        "reduzir as permissões deste usuário."
            )
        }

        if (
            membro.usuario.id == administrador.usuario.id &&
            request.permissao != PermissaoEquipe.ADMINISTRADOR
        ) {
            conflito(
                "Você não pode remover sua própria permissão de administrador."
            )
        }

        membro.funcao = request.funcao
        membro.permissao = request.permissao
        membro.cargo = opcional(request.cargo, 100, "cargo")
        membro.atualizadoEm = Instant.now()

        return MembroEquipeResponse.from(
            membros.saveAndFlush(membro)
        )
    }

    @Transactional
    fun atualizarStatus(
        authorization: String?,
        usuarioId: Long,
        request: AtualizarStatusEquipeRequest
    ): MembroEquipeResponse {
        val administrador = administradorBloqueado(
            authorization
        )

        val membro = membroDaInstituicao(
            usuarioId,
            administrador.hemocentro.id
        )

        if (!request.ativo && membro.administradorPrincipal) {
            conflito(
                "O administrador principal não pode ser desativado."
            )
        }

        if (
            !request.ativo &&
            membro.usuario.id == administrador.usuario.id
        ) {
            conflito("Você não pode desativar seu próprio acesso.")
        }

        membro.ativo = request.ativo
        membro.atualizadoEm = Instant.now()

        membros.saveAndFlush(membro)

        if (!request.ativo) {
            entityManager.createQuery(
                "delete from Sessao s where s.usuario.id = :usuarioId"
            )
                .setParameter("usuarioId", membro.usuario.id)
                .executeUpdate()
        }

        return MembroEquipeResponse.from(membro)
    }

    @Transactional
    fun transferirAdministrador(
        authorization: String?,
        request: TransferirAdministradorRequest
    ): TransferenciaAdministradorResponse {
        val anterior = administradorBloqueado(
            authorization,
            exigirPrincipal = true
        )

        validar(
            request.senhaAtual.isNotBlank() &&
                    request.senhaAtual.toByteArray(Charsets.UTF_8).size <= 72,
            "Informe sua senha atual."
        )

        val senhaCorreta = runCatching {
            encoder.matches(
                request.senhaAtual,
                anterior.usuario.senhaHash
            )
        }.getOrDefault(false)

        validar(senhaCorreta, "Senha atual incorreta.")

        if (
            request.novoAdministradorUsuarioId ==
            anterior.usuario.id
        ) {
            conflito("Selecione outro membro da equipe.")
        }

        val novo = membroDaInstituicao(
            request.novoAdministradorUsuarioId,
            anterior.hemocentro.id
        )

        if (!novo.ativo) {
            conflito("O novo administrador precisa estar ativo.")
        }

        if (
            novo.funcao != FuncaoEquipe.ADMINISTRADOR ||
            novo.permissao != PermissaoEquipe.ADMINISTRADOR
        ) {
            conflito(
                "Primeiro atribua ao membro a função e a " +
                        "permissão de administrador."
            )
        }

        anterior.administradorPrincipal = false
        anterior.atualizadoEm = Instant.now()

        // Libera a posição antes de atribuí-la ao novo principal.
        membros.saveAndFlush(anterior)

        novo.administradorPrincipal = true
        novo.atualizadoEm = Instant.now()
        membros.saveAndFlush(novo)

        return TransferenciaAdministradorResponse(
            administradorAnterior = MembroEquipeResponse.from(anterior),
            administradorAtual = MembroEquipeResponse.from(novo)
        )
    }

    private fun administradorBloqueado(
        authorization: String?,
        exigirPrincipal: Boolean = false
    ): VinculoEquipe {
        val usuario = auth.exigirHemocentro(authorization)

        val hemocentro = bloquearHemocentro(
            requireNotNull(usuario.hemocentro).id
        )

        val membro = membros.findByUsuario_Id(usuario.id)
            ?: proibido("Inicialize a equipe institucional.")

        entityManager.refresh(membro)

        if (
            !membro.ativo ||
            membro.hemocentro.id != hemocentro.id ||
            membro.permissao != PermissaoEquipe.ADMINISTRADOR
        ) {
            proibido("Esta operação exige um administrador ativo.")
        }

        if (exigirPrincipal && !membro.administradorPrincipal) {
            proibido(
                "Somente o administrador principal pode transferir essa função."
            )
        }

        return membro
    }

    private fun bloquearHemocentro(id: Long): Hemocentro {
        return hemocentros.buscarParaAlteracao(id)
            ?: naoEncontrado("Hemocentro não encontrado.")
    }

    private fun membroDaInstituicao(
        usuarioId: Long,
        hemocentroId: Long
    ): VinculoEquipe {
        val membro = membros.findByUsuario_Id(usuarioId)
            ?: naoEncontrado("Membro não encontrado.")

        entityManager.refresh(membro)

        if (membro.hemocentro.id != hemocentroId) {
            naoEncontrado("Membro não encontrado.")
        }

        return membro
    }

    private fun validarFuncao(
        funcao: FuncaoEquipe,
        permissao: PermissaoEquipe
    ) {
        validar(
            funcao != FuncaoEquipe.ADMINISTRADOR ||
                    permissao == PermissaoEquipe.ADMINISTRADOR,
            "A função Administrador exige permissão Administrador."
        )
    }

    private fun opcional(
        valor: String?,
        limite: Int,
        campo: String
    ): String? {
        val texto = valor?.trim()
            ?.takeIf { it.isNotEmpty() }

        validar(
            texto == null || texto.length <= limite,
            "O campo $campo deve ter no máximo $limite caracteres."
        )

        return texto
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

    private fun hash(valor: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(valor.toByteArray(Charsets.UTF_8))
            .joinToString("") {
                "%02x".format(it.toInt() and 255)
            }
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

    private fun proibido(mensagem: String): Nothing {
        throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            mensagem
        )
    }

    private fun naoEncontrado(mensagem: String): Nothing {
        throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            mensagem
        )
    }
}