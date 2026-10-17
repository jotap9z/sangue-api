package com.sangueplus.api.repositories

import com.sangueplus.api.models.Agendamento
import com.sangueplus.api.models.StatusAgendamento
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface AgendamentoRepository :
    JpaRepository<Agendamento, Long> {

    fun countByHorario_IdAndStatus(
        horarioId: Long,
        status: StatusAgendamento
    ): Long

    fun existsByDoador_IdAndHorario_InicioAndStatus(
        doadorId: Long,
        inicio: LocalDateTime,
        status: StatusAgendamento
    ): Boolean

    fun findByDoador_IdAndHorario_InicioGreaterThanEqualAndHorario_InicioLessThanOrderByHorario_InicioAsc(
        doadorId: Long,
        inicio: LocalDateTime,
        fim: LocalDateTime
    ): List<Agendamento>

    fun findByHorario_Hemocentro_IdAndHorario_InicioGreaterThanEqualAndHorario_InicioLessThanOrderByHorario_InicioAsc(
        hemocentroId: Long,
        inicio: LocalDateTime,
        fim: LocalDateTime
    ): List<Agendamento>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Agendamento a where a.id = :id")
    fun buscarParaAlteracao(
        @Param("id") id: Long
    ): Agendamento?
}