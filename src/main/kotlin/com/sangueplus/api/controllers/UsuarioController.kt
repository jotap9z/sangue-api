package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.CadastroDoadorRequest
import com.sangueplus.api.dtos.UsuarioResponse
import com.sangueplus.api.services.UsuarioService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/usuarios")
class UsuarioController(
    private val service: UsuarioService
) {

    @PostMapping("/doador")
    fun cadastrarDoador(
        @RequestBody request: CadastroDoadorRequest
    ): ResponseEntity<UsuarioResponse> {
        val usuario = service.cadastrarDoador(request)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(usuario)
    }
}