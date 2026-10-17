package com.sangueplus.api.repositories

import com.sangueplus.api.models.Hemocentro
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HemocentroRepository : JpaRepository<Hemocentro, Long> {
    fun findByCidadeContainingIgnoreCaseOrEstadoContainingIgnoreCase(cidade: String, estado: String): List<Hemocentro>

    // Adicionado para verificar se já existe um hemocentro com este nome exato
    fun findByNomeIgnoreCase(nome: String): Hemocentro?
}