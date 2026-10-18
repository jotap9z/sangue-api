package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.*
import com.sangueplus.api.services.EquipeService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class EquipeController(
    private val service: EquipeService
) {

    @PostMapping("/hemocentros/me/equipe/inicializar")
    fun inicializar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?
    ): MembroEquipeResponse {
        return service.inicializar(authorization)
    }

    @GetMapping("/hemocentros/me/equipe/me")
    fun meuVinculo(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?
    ): MembroEquipeResponse {
        return service.meuVinculo(authorization)
    }

    @GetMapping("/hemocentros/me/equipe")
    fun listar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?
    ): EquipeResponse {
        return service.listar(authorization)
    }

    @PostMapping("/hemocentros/me/equipe/convites")
    fun convidar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestBody request: CriarConviteEquipeRequest
    ): ConviteEquipeCriadoResponse {
        return service.convidar(authorization, request)
    }

    @PostMapping("/hemocentros/me/equipe/convites/{id}/cancelar")
    fun cancelarConvite(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long
    ): ConviteEquipeResponse {
        return service.cancelarConvite(authorization, id)
    }

    @PostMapping("/equipe/convites/aceitar")
    fun aceitarConvite(
        @RequestBody request: AceitarConviteEquipeRequest
    ): MembroEquipeResponse {
        return service.aceitarConvite(request)
    }

    @PutMapping("/hemocentros/me/equipe/{usuarioId}/funcao")
    fun atualizarFuncao(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable usuarioId: Long,
        @RequestBody request: AtualizarFuncaoEquipeRequest
    ): MembroEquipeResponse {
        return service.atualizarFuncao(
            authorization,
            usuarioId,
            request
        )
    }

    @PutMapping("/hemocentros/me/equipe/{usuarioId}/status")
    fun atualizarStatus(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable usuarioId: Long,
        @RequestBody request: AtualizarStatusEquipeRequest
    ): MembroEquipeResponse {
        return service.atualizarStatus(
            authorization,
            usuarioId,
            request
        )
    }

    @PostMapping("/hemocentros/me/equipe/administrador-principal/transferir")
    fun transferirAdministrador(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestBody request: TransferirAdministradorRequest
    ): TransferenciaAdministradorResponse {
        return service.transferirAdministrador(
            authorization,
            request
        )
    }
}