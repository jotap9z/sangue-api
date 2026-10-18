package com.sangueplus.api.models

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "api_respostas_chamados",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_api_resposta_chamado_doador",
            columnNames = ["chamado_id", "doador_id"]
        )
    ],
    indexes = [
        Index(
            name = "idx_api_resposta_chamado_ativa",
            columnList = "chamado_id,ativa"
        ),
        Index(
            name = "idx_api_resposta_doador",
            columnList = "doador_id"
        )
    ]
)
class RespostaChamado(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id", nullable = false)
    val chamado: ChamadoEmergencia,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doador_id", nullable = false)
    val doador: Usuario,

    @Column(nullable = false)
    var ativa: Boolean = true,

    @Column(
        name = "criada_em",
        nullable = false,
        updatable = false
    )
    val criadaEm: Instant = Instant.now(),

    @Column(name = "atualizada_em", nullable = false)
    var atualizadaEm: Instant = Instant.now(),

    @Column(name = "cancelada_em")
    var canceladaEm: Instant? = null
)