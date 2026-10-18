package com.sangueplus.api.controllers

import com.sangueplus.api.dtos.*
import com.sangueplus.api.services.FluxoHemocentroService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api")
class FluxoHemocentroController(
    private val service: FluxoHemocentroService
) {

    @PostMapping("/doadores/me/agendamentos/{id}/qr")
    fun gerarQr(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long
    ): QrCheckinResponse {
        return service.gerarQr(authorization, id)
    }

    @PostMapping("/hemocentros/me/checkins/validar")
    fun validarQr(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @RequestBody request: ValidarQrRequest
    ): DetalheAtendimentoResponse {
        return service.validarQr(authorization, request)
    }

    @GetMapping("/hemocentros/me/agendamentos/{id}/detalhes")
    fun consultar(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long
    ): DetalheAtendimentoResponse {
        return service.consultar(authorization, id)
    }

    @PostMapping("/hemocentros/me/agendamentos/{id}/sem-coleta")
    fun encerrarSemColeta(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long,
        @RequestBody request: EncerrarAtendimentoRequest
    ): AgendamentoResponse {
        return service.encerrarSemColeta(
            authorization,
            id,
            request
        )
    }

    @PostMapping("/hemocentros/me/agendamentos/{id}/falta")
    fun registrarFalta(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long
    ): AgendamentoResponse {
        return service.registrarFalta(authorization, id)
    }

    @PutMapping("/hemocentros/me/horarios/{id}/disponibilidade")
    fun disponibilidade(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        authorization: String?,
        @PathVariable id: Long,
        @RequestBody request: DisponibilidadeHorarioRequest
    ): HorarioResponse {
        return service.disponibilidade(
            authorization,
            id,
            request
        )
    }

    @GetMapping("/hemocentros/me/horarios")
    fun meusHorarios(
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
    ): List<HorarioResponse> {
        return service.meusHorarios(
            authorization,
            de,
            ate
        )
    }
}