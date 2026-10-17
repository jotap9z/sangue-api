package com.sangueplus.api.controllers

import com.sangueplus.api.models.Hemocentro
import com.sangueplus.api.services.HemocentroService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/hemocentros")
class HemocentroController(private val service: HemocentroService) {

    @PostMapping
    fun cadastrarHemocentro(@RequestBody hemocentro: Hemocentro): ResponseEntity<Hemocentro> {
        val novoHemocentro = service.cadastrarHemocentro(hemocentro)
        return ResponseEntity.status(HttpStatus.CREATED).body(novoHemocentro)
    }

    @GetMapping("/buscar")
    fun buscarHemocentros(
        @RequestParam(defaultValue = "") cidade: String,
        @RequestParam(defaultValue = "") estado: String
    ): ResponseEntity<List<Hemocentro>> {
        val resultados = service.listarHemocentros(cidade, estado)
        return ResponseEntity.ok(resultados)
    }
}