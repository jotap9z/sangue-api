package com.sangueplus.api.repositories

import com.sangueplus.api.models.HorarioDoacao
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface HorarioDoacaoRepository :
    JpaRepository<HorarioDoacao, Long> {

    fun existsByHemocentro_IdAndInicio(
        hemocentroId: Long,
        inicio: LocalDateTime
    ): Boolean

    fun findByHemocentro_IdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
        hemocentroId: Long,
        inicio: LocalDateTime,
        fim: LocalDateTime
    ): List<HorarioDoacao>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HorarioDoacao h where h.id = :id")
    fun buscarParaReserva(
        @Param("id") id: Long
    ): HorarioDoacao?
}