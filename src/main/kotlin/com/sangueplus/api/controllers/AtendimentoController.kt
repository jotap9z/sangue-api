package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.*
import com.sangueplus.api.services.AtendimentoService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class AtendimentoController(
    private val service: AtendimentoService
) {
    @PostMapping("/hemocentros/me/agendamentos/{id}/chegada")
    fun registrarChegada(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long
    ): AtendimentoResponse {
        return service.registrarChegada(authorization, id)
    }

    @PostMapping("/hemocentros/me/agendamentos/{id}/triagem-presencial")
    fun registrarTriagem(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long,
        @RequestBody request: TriagemPresencialRequest
    ): AtendimentoResponse {
        return service.registrarTriagem(
            authorization,
            id,
            request
        )
    }

    @PostMapping("/hemocentros/me/agendamentos/{id}/doacao")
    fun registrarDoacao(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long,
        @RequestBody request: RegistrarDoacaoRequest
    ): DoacaoResponse {
        return service.registrarDoacao(
            authorization,
            id,
            request
        )
    }

    @GetMapping("/doadores/me/doacoes")
    fun historicoDoador(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanho: Int
    ): List<DoacaoResponse> {
        return service.historicoDoador(
            authorization,
            pagina,
            tamanho
        )
    }
}