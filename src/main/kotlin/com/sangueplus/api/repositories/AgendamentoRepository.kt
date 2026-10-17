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

    @Query(
        """
        select count(a)
        from Agendamento a
        where a.horario.id = :horarioId
          and (
            a.status = :status
            or a.status in (
                com.sangueplus.api.models.StatusAgendamento.EM_ATENDIMENTO,
                com.sangueplus.api.models.StatusAgendamento.APROVADO_TRIAGEM,
                com.sangueplus.api.models.StatusAgendamento.NAO_APTO_TRIAGEM,
                com.sangueplus.api.models.StatusAgendamento.DOACAO_REALIZADA
            )
          )
        """
    )
    fun countByHorario_IdAndStatus(
        @Param("horarioId") horarioId: Long,
        @Param("status") status: StatusAgendamento
    ): Long

    @Query(
        """
        select case when count(a) > 0 then true else false end
        from Agendamento a
        where a.doador.id = :doadorId
          and a.horario.inicio = :inicio
          and (
            a.status = :status
            or a.status in (
                com.sangueplus.api.models.StatusAgendamento.EM_ATENDIMENTO,
                com.sangueplus.api.models.StatusAgendamento.APROVADO_TRIAGEM,
                com.sangueplus.api.models.StatusAgendamento.NAO_APTO_TRIAGEM,
                com.sangueplus.api.models.StatusAgendamento.DOACAO_REALIZADA
            )
          )
        """
    )
    fun existsByDoador_IdAndHorario_InicioAndStatus(
        @Param("doadorId") doadorId: Long,
        @Param("inicio") inicio: LocalDateTime,
        @Param("status") status: StatusAgendamento
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