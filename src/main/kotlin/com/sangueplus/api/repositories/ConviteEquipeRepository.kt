package com.sangueplus.api.repositories

import com.sangueplus.api.models.ConviteEquipe
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ConviteEquipeRepository :
    JpaRepository<ConviteEquipe, Long> {

    fun findByCodigoHash(
        codigoHash: String
    ): ConviteEquipe?

    fun findByHemocentro_IdOrderByCriadoEmDesc(
        hemocentroId: Long
    ): List<ConviteEquipe>

    @Query(
        """
        select case when count(c) > 0 then true else false end
        from ConviteEquipe c
        where c.hemocentro.id = :hemocentroId
          and lower(c.email) = lower(:email)
          and c.aceitoEm is null
          and c.canceladoEm is null
          and c.expiraEm > :agora
        """
    )
    fun existeConvitePendente(
        @Param("hemocentroId")
        hemocentroId: Long,

        @Param("email")
        email: String,

        @Param("agora")
        agora: Instant
    ): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select c
        from ConviteEquipe c
        where c.id = :id
        """
    )
    fun buscarParaAlteracao(
        @Param("id")
        id: Long
    ): ConviteEquipe?
}