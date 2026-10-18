package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.AtualizarPerfilDoadorRequest
import com.sangueplus.api.dtos.UsuarioResponse
import com.sangueplus.api.services.UsuarioService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/doadores/me/perfil")
class PerfilDoadorController(
    private val service: UsuarioService
) {

    @GetMapping
    fun consultar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?
    ): UsuarioResponse {
        return service.consultarMeuPerfil(authorization)
    }

    @PutMapping
    fun atualizar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestBody request: AtualizarPerfilDoadorRequest
    ): UsuarioResponse {
        return service.atualizarMeuPerfil(
            authorization,
            request
        )
    }
}