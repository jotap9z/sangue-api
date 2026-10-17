package com.sangueplus.api.controllers

import com.sangueplus.api.models.Usuario
import com.sangueplus.api.services.UsuarioService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/usuarios")
class UsuarioController(private val service: UsuarioService) {

    // Cria a rota POST para o telemóvel Android enviar os dados
    @PostMapping("/doador")
    fun cadastrarDoador(@RequestBody usuario: Usuario): ResponseEntity<Any> {
        return try {
            val novoUsuario = service.cadastrarDoador(usuario)
            ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario)
        } catch (e: IllegalArgumentException) {
            // Se falhar nas regras de negócio (ex: senha fraca), devolve o erro para o telemóvel
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("erro" to e.message))
        }
    }
}