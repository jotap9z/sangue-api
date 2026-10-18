package com.sangueplus.api.repositories

import com.sangueplus.api.models.ChamadoEmergencia
import com.sangueplus.api.models.StatusChamado
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChamadoEmergenciaRepository :
    JpaRepository<ChamadoEmergencia, Long> {

    @EntityGraph(attributePaths = ["hemocentro"])
    fun findByHemocentro_IdAndStatus(
        hemocentroId: Long,
        status: StatusChamado,
        pageable: Pageable
    ): Page<ChamadoEmergencia>

    @EntityGraph(attributePaths = ["hemocentro"])
    fun findByHemocentro_Id(
        hemocentroId: Long,
        pageable: Pageable
    ): Page<ChamadoEmergencia>

    @EntityGraph(attributePaths = ["hemocentro"])
    @Query(
        """
        select c
        from ChamadoEmergencia c
        where c.status = :status
          and (
            :cidade = ''
            or locate(
                lower(:cidade),
                lower(c.hemocentro.cidade)
            ) > 0
          )
          and (
            :estado = ''
            or lower(c.hemocentro.estado) = lower(:estado)
          )
          and (
            :tipoSanguineo = ''
            or c.tipoSanguineo = :tipoSanguineo
          )
        """
    )
    fun buscarPublicos(
        @Param("status")
        status: StatusChamado,

        @Param("cidade")
        cidade: String,

        @Param("estado")
        estado: String,

        @Param("tipoSanguineo")
        tipoSanguineo: String,

        pageable: Pageable
    ): Page<ChamadoEmergencia>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select c
        from ChamadoEmergencia c
        where c.id = :id
        """
    )
    fun buscarParaAlteracao(
        @Param("id")
        id: Long
    ): ChamadoEmergencia?
}