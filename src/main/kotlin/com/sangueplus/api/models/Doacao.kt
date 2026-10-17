package com.sangueplus.api.models

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "api_doacoes",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_api_doacao_agendamento",
            columnNames = ["agendamento_id"]
        )
    ],
    indexes = [
        Index(
            name = "idx_api_doacao_doador_data",
            columnList = "doador_id,realizada_em"
        )
    ]
)
class Doacao(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false)
    val agendamento: Agendamento,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doador_id", nullable = false)
    val doador: Usuario,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hemocentro_id", nullable = false)
    val hemocentro: Hemocentro,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    val registradoPor: Usuario,

    @Column(name = "realizada_em", nullable = false)
    val realizadaEm: Instant = Instant.now(),

    @Column(nullable = false, length = 10)
    val tipoSanguineoConfirmado: String,

    @Column(length = 500)
    val observacoes: String? = null
)