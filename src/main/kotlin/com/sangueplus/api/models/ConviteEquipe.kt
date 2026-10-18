package com.sangueplus.api.models

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "api_convites_equipe",
    indexes = [
        Index(
            name = "idx_api_convite_hemocentro",
            columnList = "hemocentro_id,criadoEm"
        ),
        Index(
            name = "idx_api_convite_email",
            columnList = "hemocentro_id,email"
        )
    ]
)
class ConviteEquipe(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hemocentro_id", nullable = false)
    val hemocentro: Hemocentro,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "convidado_por_id", nullable = false)
    val convidadoPor: Usuario,

    @Column(nullable = false, length = 100)
    val nome: String,

    @Column(nullable = false, length = 100)
    val email: String,

    @Column(length = 30)
    val telefone: String? = null,

    @Column(length = 100)
    val cargo: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val funcao: FuncaoEquipe,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val permissao: PermissaoEquipe,

    @Column(
        name = "codigo_hash",
        nullable = false,
        unique = true,
        length = 64
    )
    val codigoHash: String,

    @Column(nullable = false)
    val expiraEm: Instant,

    @Column(
        name = "criadoEm",
        nullable = false,
        updatable = false
    )
    val criadoEm: Instant = Instant.now(),

    var aceitoEm: Instant? = null,

    var canceladoEm: Instant? = null
)