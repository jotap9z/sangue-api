package com.sangueplus.api.dtos

import com.sangueplus.api.models.ConviteEquipe
import com.sangueplus.api.models.FuncaoEquipe
import com.sangueplus.api.models.PermissaoEquipe
import com.sangueplus.api.models.VinculoEquipe
import java.time.Instant

data class CriarConviteEquipeRequest(
    val nome: String,
    val email: String,
    val funcao: FuncaoEquipe,
    val permissao: PermissaoEquipe = PermissaoEquipe.PADRAO,
    val telefone: String? = null,
    val cargo: String? = null
)

data class AceitarConviteEquipeRequest(
    val codigo: String,
    val senha: String
)

data class AtualizarFuncaoEquipeRequest(
    val funcao: FuncaoEquipe,
    val permissao: PermissaoEquipe,
    val cargo: String? = null
)

data class AtualizarStatusEquipeRequest(
    val ativo: Boolean
)

data class TransferirAdministradorRequest(
    val novoAdministradorUsuarioId: Long,
    val senhaAtual: String
)

data class MembroEquipeResponse(
    val usuarioId: Long,
    val hemocentroId: Long,
    val nome: String,
    val email: String,
    val telefone: String?,
    val cargo: String?,
    val funcao: FuncaoEquipe,
    val permissao: PermissaoEquipe,
    val ativo: Boolean,
    val administradorPrincipal: Boolean,
    val criadoEm: Instant,
    val atualizadoEm: Instant
) {
    companion object {
        fun from(vinculo: VinculoEquipe): MembroEquipeResponse {
            return MembroEquipeResponse(
                usuarioId = vinculo.usuario.id,
                hemocentroId = vinculo.hemocentro.id,
                nome = vinculo.usuario.nome,
                email = vinculo.usuario.email,
                telefone = vinculo.usuario.telefone,
                cargo = vinculo.cargo,
                funcao = vinculo.funcao,
                permissao = vinculo.permissao,
                ativo = vinculo.ativo,
                administradorPrincipal =
                    vinculo.administradorPrincipal,
                criadoEm = vinculo.criadoEm,
                atualizadoEm = vinculo.atualizadoEm
            )
        }
    }
}

enum class StatusConviteEquipe {
    PENDENTE,
    ACEITO,
    CANCELADO,
    EXPIRADO
}

data class ConviteEquipeResponse(
    val id: Long,
    val hemocentroId: Long,
    val nome: String,
    val email: String,
    val telefone: String?,
    val cargo: String?,
    val funcao: FuncaoEquipe,
    val permissao: PermissaoEquipe,
    val status: StatusConviteEquipe,
    val criadoEm: Instant,
    val expiraEm: Instant,
    val aceitoEm: Instant?,
    val canceladoEm: Instant?
) {
    companion object {
        fun from(
            convite: ConviteEquipe,
            agora: Instant = Instant.now()
        ): ConviteEquipeResponse {
            val status = when {
                convite.aceitoEm != null ->
                    StatusConviteEquipe.ACEITO

                convite.canceladoEm != null ->
                    StatusConviteEquipe.CANCELADO

                !convite.expiraEm.isAfter(agora) ->
                    StatusConviteEquipe.EXPIRADO

                else ->
                    StatusConviteEquipe.PENDENTE
            }

            return ConviteEquipeResponse(
                id = convite.id,
                hemocentroId = convite.hemocentro.id,
                nome = convite.nome,
                email = convite.email,
                telefone = convite.telefone,
                cargo = convite.cargo,
                funcao = convite.funcao,
                permissao = convite.permissao,
                status = status,
                criadoEm = convite.criadoEm,
                expiraEm = convite.expiraEm,
                aceitoEm = convite.aceitoEm,
                canceladoEm = convite.canceladoEm
            )
        }
    }
}

data class ConviteEquipeCriadoResponse(
    val convite: ConviteEquipeResponse,
    val codigoConvite: String,
    val mensagem: String =
        "Compartilhe o código com o funcionário. " +
                "O convite ainda não foi aceito."
)

data class EquipeResponse(
    val membros: List<MembroEquipeResponse>,
    val convites: List<ConviteEquipeResponse>
)

data class TransferenciaAdministradorResponse(
    val administradorAnterior: MembroEquipeResponse,
    val administradorAtual: MembroEquipeResponse
)