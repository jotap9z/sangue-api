package com.sangueplus.api.repositories

import com.sangueplus.api.models.Hemocentro
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface HemocentroRepository :
    JpaRepository<Hemocentro, Long> {

    fun existsByNomeIgnoreCase(nome: String): Boolean

    fun existsByNomeIgnoreCaseAndIdNot(
        nome: String,
        id: Long
    ): Boolean

    fun existsByCnpjAndIdNot(
        cnpj: String,
        id: Long
    ): Boolean

    fun existsByCnesAndIdNot(
        cnes: String,
        id: Long
    ): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hemocentro h where h.id = :id")
    fun buscarParaAlteracao(
        @Param("id") id: Long
    ): Hemocentro?

    @Query(
        """
        select h
        from Hemocentro h
        where (
            :cidade = ''
            or locate(lower(:cidade), lower(h.cidade)) > 0
        )
        and (
            :estado = ''
            or lower(h.estado) = lower(:estado)
        )
        order by h.nome
        """
    )
    fun buscarPorLocalizacao(
        @Param("cidade") cidade: String,
        @Param("estado") estado: String
    ): List<Hemocentro>
}