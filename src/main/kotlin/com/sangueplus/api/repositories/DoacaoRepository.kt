package com.sangueplus.api.repositories

import com.sangueplus.api.models.Doacao
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface DoacaoRepository : JpaRepository<Doacao, Long> {

    fun findByAgendamento_Id(agendamentoId: Long): Doacao?

    fun findByDoador_IdOrderByRealizadaEmDescIdDesc(
        doadorId: Long,
        pageable: Pageable
    ): List<Doacao>

    fun findTop4ByDoador_IdOrderByRealizadaEmDescIdDesc(
        doadorId: Long
    ): List<Doacao>

    fun countByDoador_IdAndRealizadaEmGreaterThanEqual(
        doadorId: Long,
        desde: Instant
    ): Long
}