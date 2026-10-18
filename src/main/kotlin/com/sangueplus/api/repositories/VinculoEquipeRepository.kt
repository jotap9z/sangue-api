package com.sangueplus.api.repositories

import com.sangueplus.api.models.VinculoEquipe
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VinculoEquipeRepository :
    JpaRepository<VinculoEquipe, Long> {

    @EntityGraph(
        attributePaths = ["usuario", "hemocentro"]
    )
    fun findByUsuario_Id(
        usuarioId: Long
    ): VinculoEquipe?

    @EntityGraph(
        attributePaths = ["usuario", "hemocentro"]
    )
    fun findByHemocentro_IdOrderByUsuario_NomeAsc(
        hemocentroId: Long
    ): List<VinculoEquipe>

    fun existsByHemocentro_Id(
        hemocentroId: Long
    ): Boolean

    @EntityGraph(
        attributePaths = ["usuario", "hemocentro"]
    )
    fun findByHemocentro_IdAndAdministradorPrincipalTrue(
        hemocentroId: Long
    ): VinculoEquipe?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select v
        from VinculoEquipe v
        where v.usuario.id = :usuarioId
        """
    )
    fun buscarParaAlteracao(
        @Param("usuarioId")
        usuarioId: Long
    ): VinculoEquipe?

    @Query(
        """
        select count(u)
        from Usuario u
        where u.hemocentro.id = :hemocentroId
          and u.tipoUsuario = 'hemocentro'
        """
    )
    fun contarContasInstitucionais(
        @Param("hemocentroId")
        hemocentroId: Long
    ): Long
}