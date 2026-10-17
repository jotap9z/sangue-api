package com.sangueplus.api.repositories

import com.sangueplus.api.models.Hemocentro
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface HemocentroRepository : JpaRepository<Hemocentro, Long> {
    fun existsByNomeIgnoreCase(nome: String): Boolean

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