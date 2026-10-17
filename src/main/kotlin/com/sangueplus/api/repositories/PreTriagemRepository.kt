package com.sangueplus.api.repositories

import com.sangueplus.api.models.PreTriagem
import org.springframework.data.jpa.repository.JpaRepository

interface PreTriagemRepository : JpaRepository<PreTriagem, Long> {

    fun findFirstByDoador_IdOrderByAvaliadaEmDescIdDesc(
        doadorId: Long
    ): PreTriagem?
}