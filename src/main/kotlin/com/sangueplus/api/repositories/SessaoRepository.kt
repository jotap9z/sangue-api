package com.sangueplus.api.repositories

import com.sangueplus.api.models.Sessao
import org.springframework.data.jpa.repository.JpaRepository

interface SessaoRepository : JpaRepository<Sessao, Long> {
    fun findByTokenHash(tokenHash: String): Sessao?
}