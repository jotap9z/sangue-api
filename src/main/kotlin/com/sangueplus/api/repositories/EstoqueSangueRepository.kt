package com.sangueplus.api.repositories

import com.sangueplus.api.models.EstoqueSangue
import org.springframework.data.jpa.repository.JpaRepository

interface EstoqueSangueRepository :
    JpaRepository<EstoqueSangue, Long> {

    fun findByHemocentro_Id(
        hemocentroId: Long
    ): List<EstoqueSangue>

    fun findByHemocentro_IdAndTipoSanguineo(
        hemocentroId: Long,
        tipoSanguineo: String
    ): EstoqueSangue?
}