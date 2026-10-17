package com.sangueplus.api.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "api_horarios_doacao",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_api_horario_hemocentro_inicio",
            columnNames = ["hemocentro_id", "inicio"]
        )
    ]
)
class HorarioDoacao(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hemocentro_id", nullable = false)
    val hemocentro: Hemocentro,

    @Column(nullable = false)
    val inicio: LocalDateTime,

    @Column(nullable = false)
    val capacidade: Int,

    @Column(nullable = false)
    var ativo: Boolean = true
)