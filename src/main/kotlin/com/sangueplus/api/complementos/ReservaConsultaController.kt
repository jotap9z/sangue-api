package com.sangueplus.api.complementos

import com.sangueplus.api.dtos.AgendamentoResponse
import com.sangueplus.api.repositories.AgendamentoRepository
import com.sangueplus.api.services.AuthService
import org.springframework.web.bind.annotation.*
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@RestController
class ReservaConsultaController(private val auth: AuthService, private val reservas: AgendamentoRepository) {
    @GetMapping("/api/doadores/me/agendamentos/{id}")
    @Transactional(readOnly = true)
    fun consultar(@RequestHeader(value="Authorization",required=false) a: String?,@PathVariable id: Long): AgendamentoResponse {
        val u=auth.exigirDoador(a)
        val r=reservas.findById(id).orElse(null)
        if(r==null || r.doador.id!=u.id) throw ResponseStatusException(HttpStatus.NOT_FOUND,"Agendamento não encontrado.")
        return AgendamentoResponse.from(r)
    }
}