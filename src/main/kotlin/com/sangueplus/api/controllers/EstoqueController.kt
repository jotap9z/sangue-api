package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.AtualizarEstoqueRequest
import com.sangueplus.api.dtos.EstoqueHemocentroResponse
import com.sangueplus.api.dtos.EstoqueItemResponse
import com.sangueplus.api.services.EstoqueService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/hemocentros")
class EstoqueController(
    private val service: EstoqueService
) {
    @GetMapping("/{hemocentroId}/estoque")
    fun consultarPublico(
        @PathVariable hemocentroId: Long
    ): EstoqueHemocentroResponse {
        return service.consultarPublico(hemocentroId)
    }

    @GetMapping("/me/estoque")
    fun consultarMeu(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?
    ): EstoqueHemocentroResponse {
        return service.consultarMeu(authorization)
    }

    @PutMapping("/me/estoque/{tipoSanguineo}")
    fun atualizar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable tipoSanguineo: String,
        @RequestBody request: AtualizarEstoqueRequest
    ): EstoqueItemResponse {
        return service.atualizar(
            authorization,
            tipoSanguineo,
            request
        )
    }
}