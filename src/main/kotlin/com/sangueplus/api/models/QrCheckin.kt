package com.sangueplus.api.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "api_qr_checkin")
class QrCheckin(
    @Id
    val agendamentoId: Long,

    @Column(
        nullable = false,
        unique = true,
        length = 64
    )
    var codigoHash: String,

    @Column(nullable = false)
    var expiraEm: Instant,

    var utilizadoEm: Instant? = null
)