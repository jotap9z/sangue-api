package com.sangueplus.api.repositories

import com.sangueplus.api.models.Doacao
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface DoacaoRepository : JpaRepository<Doacao, Long> {

    fun findByAgendamento_Id(agendamentoId: Long): Doacao?

    fun findByDoador_IdOrderByRealizadaEmDescIdDesc(
        doadorId: Long,
        pageable: Pageable
    ): List<Doacao>
}