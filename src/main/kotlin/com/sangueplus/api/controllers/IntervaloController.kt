package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.ConfigurarIntervaloRequest
import com.sangueplus.api.dtos.IntervaloResponse
import com.sangueplus.api.services.IntervaloService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class IntervaloController(
    private val service: IntervaloService
) {
    @GetMapping("/doadores/me/intervalo")
    fun consultarMeu(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?
    ): IntervaloResponse {
        return service.consultarMeu(authorization)
    }

    @PutMapping("/hemocentros/me/agendamentos/{id}/regra-intervalo")
    fun configurar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long,
        @RequestBody request: ConfigurarIntervaloRequest
    ): IntervaloResponse {
        return service.configurarNoAtendimento(
            authorization,
            id,
            request
        )
    }
}