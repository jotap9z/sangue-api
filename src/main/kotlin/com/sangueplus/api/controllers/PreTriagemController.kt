package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.PreTriagemRequest
import com.sangueplus.api.dtos.PreTriagemResponse
import com.sangueplus.api.services.PreTriagemService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/doadores/me/pre-triagens")
class PreTriagemController(
    private val service: PreTriagemService
) {
    @PostMapping
    fun responder(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestBody request: PreTriagemRequest
    ): ResponseEntity<PreTriagemResponse> {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(service.responder(authorization, request))
    }

    @GetMapping("/ultima")
    fun consultarUltima(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?
    ): PreTriagemResponse {
        return service.consultarUltima(authorization)
    }
}