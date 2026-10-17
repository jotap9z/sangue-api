package com.sangueplus.api.models

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "api_estoques_sangue",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_api_estoque_hemocentro_tipo",
            columnNames = ["hemocentro_id", "tipo_sanguineo"]
        )
    ]
)
class EstoqueSangue(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hemocentro_id", nullable = false)
    val hemocentro: Hemocentro,

    @Column(name = "tipo_sanguineo", nullable = false, length = 3)
    val tipoSanguineo: String,

    @Column(nullable = false)
    var quantidadeDisponivel: Int,

    @Column(nullable = false)
    var nivelMinimo: Int,

    @Column(nullable = false)
    var nivelIdeal: Int,

    @Column(nullable = false)
    var atualizadoEm: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atualizado_por_id", nullable = false)
    var atualizadoPor: Usuario,

    @Version
    var versao: Long = 0
)