package com.sangueplus.api.repositories

import com.sangueplus.api.models.QrCheckin
import org.springframework.data.jpa.repository.JpaRepository

interface QrCheckinRepository :
    JpaRepository<QrCheckin, Long> {

    fun findByCodigoHash(
        codigoHash: String
    ): QrCheckin?
}