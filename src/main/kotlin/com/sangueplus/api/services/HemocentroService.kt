package com.sangueplus.api.services

import com.sangueplus.api.models.Hemocentro
import com.sangueplus.api.repositories.HemocentroRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class HemocentroService(private val repository: HemocentroRepository) {

    // UC03: Cadastro de Hemocentro com a mensagem exata solicitada
    fun cadastrarHemocentro(hemocentro: Hemocentro): Hemocentro {
        val hemocentroExistente = repository.findByNomeIgnoreCase(hemocentro.nome)
        if (hemocentroExistente != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Hemocentro já existente")
        }

        try {
            return repository.save(hemocentro)
        } catch (e: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Hemocentro já existente")
        }
    }

    // UC06: Pesquisar Hemocentros por localização
    fun listarHemocentros(cidade: String, estado: String): List<Hemocentro> {
        return repository.findByCidadeContainingIgnoreCaseOrEstadoContainingIgnoreCase(cidade, estado)
    }
}