package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.LoginRequest
import com.sangueplus.api.dtos.LoginResponse
import com.sangueplus.api.dtos.UsuarioResponse
import com.sangueplus.api.services.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val service: AuthService
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest
    ): LoginResponse {
        return service.login(request)
    }

    @GetMapping("/me")
    fun usuarioAtual(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?
    ): UsuarioResponse {
        return service.usuarioAtual(authorization)
    }

    @PostMapping("/logout")
    fun logout(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?
    ): ResponseEntity<Void> {
        service.logout(authorization)

        return ResponseEntity.noContent().build()
    }
}