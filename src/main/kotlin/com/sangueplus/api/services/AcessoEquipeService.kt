package com.sangueplus.api.services

import com.sangueplus.api.models.FuncaoEquipe
import com.sangueplus.api.models.PermissaoEquipe
import com.sangueplus.api.models.VinculoEquipe
import com.sangueplus.api.repositories.VinculoEquipeRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AcessoEquipeService(
    private val auth: AuthService,
    private val equipe: VinculoEquipeRepository
) {

    @Transactional(readOnly = true)
    fun exigirMembro(
        authorization: String?
    ): VinculoEquipe {
        val usuario = auth.exigirHemocentro(authorization)

        val vinculo = equipe.findByUsuario_Id(usuario.id)
            ?: negar(
                "Inicialize a equipe institucional antes de continuar."
            )

        if (
            !vinculo.ativo ||
            vinculo.hemocentro.id != usuario.hemocentro?.id
        ) {
            negar("Acesso institucional inválido ou desativado.")
        }

        return vinculo
    }

    @Transactional(readOnly = true)
    fun exigirAdministrador(
        authorization: String?
    ): VinculoEquipe {
        val membro = exigirMembro(authorization)

        if (membro.permissao != PermissaoEquipe.ADMINISTRADOR) {
            negar("Esta operação exige permissão de administrador.")
        }

        return membro
    }

    @Transactional(readOnly = true)
    fun exigirAcessoChamados(
        authorization: String?,
        escrita: Boolean = false
    ): VinculoEquipe {
        val membro = exigirMembro(authorization)
        validarAcessoChamados(membro, escrita)
        return membro
    }

    @Transactional(readOnly = true)
    fun autorizar(
        authorization: String?,
        metodo: String,
        caminho: String
    ) {
        val membro = exigirMembro(authorization)

        if (membro.permissao == PermissaoEquipe.ADMINISTRADOR) {
            return
        }

        val leitura = metodo == "GET" || metodo == "HEAD"

        val rota = caminho
            .removePrefix("/api/hemocentros/me")
            .trimEnd('/')

        if (leitura && rota in setOf("", "/equipe/me")) {
            return
        }

        if (
            rota == "/chamados" ||
            rota.startsWith("/chamados/")
        ) {
            validarAcessoChamados(membro, escrita = !leitura)
            return
        }

        if (
            !leitura &&
            membro.permissao == PermissaoEquipe.SOMENTE_VISUALIZACAO
        ) {
            negar("Sua permissão permite apenas consultas.")
        }

        val permitido = when {
            rota == "/estoque" ||
                    rota.startsWith("/estoque/") -> {
                membro.funcao == FuncaoEquipe.ESTOQUE
            }

            rota == "/horarios" ||
                    rota.startsWith("/horarios/") -> {
                membro.funcao == FuncaoEquipe.ATENDIMENTO
            }

            leitura && (
                    rota == "/agendamentos" ||
                            rota.startsWith("/agendamentos/")
                    ) -> {
                membro.funcao in setOf(
                    FuncaoEquipe.ATENDIMENTO,
                    FuncaoEquipe.TRIAGEM
                )
            }

            !leitura && rota == "/checkins/validar" -> {
                membro.funcao == FuncaoEquipe.ATENDIMENTO
            }

            !leitura && Regex(
                "^/agendamentos/[0-9]+/(chegada|cancelar|falta)$"
            ).matches(rota) -> {
                membro.funcao == FuncaoEquipe.ATENDIMENTO
            }

            !leitura && Regex(
                "^/agendamentos/[0-9]+/(triagem-presencial|doacao|regra-intervalo)$"
            ).matches(rota) -> {
                membro.funcao == FuncaoEquipe.TRIAGEM
            }

            !leitura && Regex(
                "^/agendamentos/[0-9]+/sem-coleta$"
            ).matches(rota) -> {
                membro.funcao in setOf(
                    FuncaoEquipe.ATENDIMENTO,
                    FuncaoEquipe.TRIAGEM
                )
            }

            else -> false
        }

        if (!permitido) {
            negar("Sua função não permite esta operação.")
        }
    }

    private fun validarAcessoChamados(
        membro: VinculoEquipe,
        escrita: Boolean
    ) {
        if (membro.permissao == PermissaoEquipe.ADMINISTRADOR) {
            return
        }

        if (
            escrita &&
            membro.permissao == PermissaoEquipe.SOMENTE_VISUALIZACAO
        ) {
            negar("Sua permissão permite apenas consultas.")
        }

        val permitido = if (escrita) {
            membro.funcao == FuncaoEquipe.ESTOQUE
        } else {
            membro.funcao in setOf(
                FuncaoEquipe.ESTOQUE,
                FuncaoEquipe.ATENDIMENTO
            )
        }

        if (!permitido) {
            negar("Sua função não permite esta operação com chamados.")
        }
    }

    private fun negar(mensagem: String): Nothing {
        throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            mensagem
        )
    }
}