package com.sangueplus.api.models

import jakarta.persistence.*
import java.time.Instant

enum class FuncaoEquipe {
    ADMINISTRADOR,
    ATENDIMENTO,
    TRIAGEM,
    ESTOQUE
}

enum class PermissaoEquipe {
    ADMINISTRADOR,
    PADRAO,
    SOMENTE_VISUALIZACAO
}

@Entity
@Table(
    name = "api_vinculos_equipe",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_api_equipe_usuario",
            columnNames = ["usuario_id"]
        )
    ],
    indexes = [
        Index(
            name = "idx_api_equipe_hemocentro",
            columnList = "hemocentro_id,ativo"
        )
    ]
)
class VinculoEquipe(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    val usuario: Usuario,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hemocentro_id", nullable = false)
    val hemocentro: Hemocentro,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var funcao: FuncaoEquipe,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var permissao: PermissaoEquipe,

    @Column(length = 100)
    var cargo: String? = null,

    @Column(nullable = false)
    var ativo: Boolean = true,

    @Column(nullable = false)
    var administradorPrincipal: Boolean = false,

    @Column(nullable = false, updatable = false)
    val criadoEm: Instant = Instant.now(),

    @Column(nullable = false)
    var atualizadoEm: Instant = Instant.now()
)