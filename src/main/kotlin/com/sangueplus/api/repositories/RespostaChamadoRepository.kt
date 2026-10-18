package com.sangueplus.api.repositories

import com.sangueplus.api.models.RespostaChamado
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface RespostaChamadoRepository :
    JpaRepository<RespostaChamado, Long> {

    fun findByChamado_IdAndDoador_Id(
        chamadoId: Long,
        doadorId: Long
    ): RespostaChamado?

    fun countByChamado_IdAndAtivaTrue(
        chamadoId: Long
    ): Long

    @EntityGraph(attributePaths = ["doador"])
    fun findByChamado_IdAndAtivaTrue(
        chamadoId: Long,
        pageable: Pageable
    ): Page<RespostaChamado>

    @EntityGraph(
        attributePaths = ["chamado", "chamado.hemocentro"]
    )
    fun findByDoador_Id(
        doadorId: Long,
        pageable: Pageable
    ): Page<RespostaChamado>
}