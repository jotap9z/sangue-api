package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.CadastroHemocentroRequest
import com.sangueplus.api.dtos.CadastroHemocentroResponse
import com.sangueplus.api.dtos.HemocentroResponse
import com.sangueplus.api.services.HemocentroService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/hemocentros")
class HemocentroController(
    private val service: HemocentroService
) {
    @PostMapping
    fun cadastrarHemocentro(
        @RequestBody request: CadastroHemocentroRequest
    ): ResponseEntity<CadastroHemocentroResponse> {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(service.cadastrarHemocentro(request))
    }

    @GetMapping("/buscar")
    fun buscarHemocentros(
        @RequestParam(defaultValue = "") cidade: String,
        @RequestParam(defaultValue = "") estado: String
    ): List<HemocentroResponse> {
        return service.listarHemocentros(cidade, estado)
    }

    @GetMapping("/me")
    fun meuHemocentro(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?
    ): HemocentroResponse {
        return service.meuHemocentro(authorization)
    }

    @GetMapping("/{id}")
    fun buscarPorId(
        @PathVariable id: Long
    ): HemocentroResponse {
        return service.buscarPorId(id)
    }
}