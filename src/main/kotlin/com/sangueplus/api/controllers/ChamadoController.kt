package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.*
import com.sangueplus.api.models.StatusChamado
import com.sangueplus.api.services.ChamadoService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ChamadoController(
    private val service: ChamadoService
) {

    @PostMapping("/hemocentros/me/chamados")
    fun criar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestBody request: CriarChamadoRequest
    ): ResponseEntity<ChamadoInstitucionalResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.criar(authorization, request))
    }

    @GetMapping("/hemocentros/me/chamados")
    fun listarInstitucionais(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestParam(required = false)
        status: StatusChamado?,
        @RequestParam(defaultValue = "0")
        pagina: Int,
        @RequestParam(defaultValue = "20")
        tamanho: Int
    ): PaginaChamadosResponse<ChamadoInstitucionalResponse> {
        return service.listarInstitucionais(
            authorization,
            status,
            pagina,
            tamanho
        )
    }

    @GetMapping("/hemocentros/me/chamados/{id}")
    fun consultarInstitucional(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long
    ): ChamadoInstitucionalResponse {
        return service.consultarInstitucional(authorization, id)
    }

    @PutMapping("/hemocentros/me/chamados/{id}")
    fun atualizar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long,
        @RequestBody request: AtualizarChamadoRequest
    ): ChamadoInstitucionalResponse {
        return service.atualizar(authorization, id, request)
    }

    @PostMapping("/hemocentros/me/chamados/{id}/encerrar")
    fun encerrar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long,
        @RequestBody request: EncerrarChamadoRequest
    ): ChamadoInstitucionalResponse {
        return service.encerrar(authorization, id, request)
    }

    @GetMapping("/hemocentros/me/chamados/{id}/interessados")
    fun listarInteressados(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0")
        pagina: Int,
        @RequestParam(defaultValue = "20")
        tamanho: Int
    ): PaginaChamadosResponse<InteressadoChamadoResponse> {
        return service.listarInteressados(
            authorization,
            id,
            pagina,
            tamanho
        )
    }

    @GetMapping("/chamados")
    fun listarPublicos(
        @RequestParam(defaultValue = "")
        cidade: String,
        @RequestParam(defaultValue = "")
        estado: String,
        @RequestParam(defaultValue = "")
        tipoSanguineo: String,
        @RequestParam(defaultValue = "0")
        pagina: Int,
        @RequestParam(defaultValue = "20")
        tamanho: Int
    ): PaginaChamadosResponse<ChamadoResponse> {
        return service.listarPublicos(
            cidade,
            estado,
            tipoSanguineo,
            pagina,
            tamanho
        )
    }

    @GetMapping("/chamados/{id}")
    fun consultarPublico(
        @PathVariable id: Long
    ): ChamadoResponse {
        return service.consultarPublico(id)
    }

    @PostMapping("/doadores/me/chamados/{id}/responder")
    fun responder(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long
    ): MinhaRespostaChamadoResponse {
        return service.responder(authorization, id)
    }

    @DeleteMapping("/doadores/me/chamados/{id}/resposta")
    fun cancelarResposta(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long
    ): MinhaRespostaChamadoResponse {
        return service.cancelarResposta(authorization, id)
    }

    @GetMapping("/doadores/me/respostas-chamados")
    fun minhasRespostas(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestParam(defaultValue = "0")
        pagina: Int,
        @RequestParam(defaultValue = "20")
        tamanho: Int
    ): PaginaChamadosResponse<MinhaRespostaChamadoResponse> {
        return service.minhasRespostas(
            authorization,
            pagina,
            tamanho
        )
    }
}