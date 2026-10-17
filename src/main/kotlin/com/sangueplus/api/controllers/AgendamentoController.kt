package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.*
import com.sangueplus.api.services.AgendamentoService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api")
class AgendamentoController(
    private val service: AgendamentoService
) {
    @PostMapping("/hemocentros/me/horarios")
    fun criarHorario(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestBody request: CriarHorarioRequest
    ): ResponseEntity<HorarioResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            service.criarHorario(authorization, request)
        )
    }

    @GetMapping("/hemocentros/{hemocentroId}/horarios")
    fun listarHorarios(
        @PathVariable hemocentroId: Long,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        de: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        ate: LocalDate
    ): List<HorarioResponse> {
        return service.listarHorarios(hemocentroId, de, ate)
    }

    @PostMapping("/doadores/me/agendamentos")
    fun reservar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestBody request: CriarAgendamentoRequest
    ): ResponseEntity<AgendamentoResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            service.reservar(authorization, request)
        )
    }

    @GetMapping("/doadores/me/agendamentos")
    fun meusAgendamentos(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        de: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        ate: LocalDate
    ): List<AgendamentoResponse> {
        return service.meusAgendamentos(authorization, de, ate)
    }

    @GetMapping("/hemocentros/me/agendamentos")
    fun agendaHemocentro(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        de: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        ate: LocalDate
    ): List<AgendamentoResponse> {
        return service.agendaHemocentro(authorization, de, ate)
    }

    @PostMapping("/doadores/me/agendamentos/{id}/cancelar")
    fun cancelarDoador(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long,
        @RequestBody request: CancelarAgendamentoRequest
    ): AgendamentoResponse {
        return service.cancelarDoador(authorization, id, request)
    }

    @PostMapping("/hemocentros/me/agendamentos/{id}/cancelar")
    fun cancelarHemocentro(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long,
        @RequestBody request: CancelarAgendamentoRequest
    ): AgendamentoResponse {
        return service.cancelarHemocentro(
            authorization,
            id,
            request
        )
    }
}