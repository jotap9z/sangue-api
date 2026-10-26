package com.sangueplus.api.complementos

import com.sangueplus.api.dtos.HemocentroResponse
import com.sangueplus.api.repositories.HemocentroRepository
import com.sangueplus.api.services.AcessoEquipeService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

data class CoordenadasHemocentro(val latitude: Double?, val longitude: Double?)

@RestController
@RequestMapping("/api/hemocentros/me/coordenadas")
class CoordenadasController(private val acesso: AcessoEquipeService, private val centros: HemocentroRepository) {
    @PutMapping
    @Transactional
    fun salvar(@RequestHeader(value="Authorization",required=false) a: String?,@RequestBody r: CoordenadasHemocentro): HemocentroResponse {
        val m=acesso.exigirAdministrador(a)
        if((r.latitude==null)!=(r.longitude==null) || r.latitude?.let{!it.isFinite() || it !in -90.0..90.0}==true || r.longitude?.let{!it.isFinite() || it !in -180.0..180.0}==true)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe latitude e longitude válidas, ou deixe ambas vazias.")
        val h=centros.buscarParaAlteracao(m.hemocentro.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND,"Hemocentro não encontrado.")
        h.latitude=r.latitude
        h.longitude=r.longitude
        return HemocentroResponse.from(centros.saveAndFlush(h))
    }
}