package com.sangueplus.api.models

import jakarta.persistence.*
import java.time.Instant

enum class NivelUrgencia {
    BAIXA,
    MEDIA,
    ALTA,
    CRITICA
}

enum class StatusChamado {
    ATIVO,
    ENCERRADO
}

@Entity
@Table(
    name = "api_chamados_emergencia",
    indexes = [
        Index(
            name = "idx_api_chamado_instituicao_status",
            columnList = "hemocentro_id,status"
        ),
        Index(
            name = "idx_api_chamado_tipo_status",
            columnList = "tipo_sanguineo,status"
        ),
        Index(
            name = "idx_api_chamado_criado",
            columnList = "criado_em"
        )
    ]
)
class ChamadoEmergencia(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hemocentro_id", nullable = false)
    val hemocentro: Hemocentro,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false)
    val criadoPor: Usuario,

    @Column(nullable = false, length = 150)
    var titulo: String,

    @Column(
        name = "tipo_sanguineo",
        nullable = false,
        length = 3
    )
    var tipoSanguineo: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var urgencia: NivelUrgencia,

    @Column(name = "meta_doadores", nullable = false)
    var metaDoadores: Int,

    @Column(nullable = false, length = 2000)
    var mensagem: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StatusChamado = StatusChamado.ATIVO,

    @Column(
        name = "criado_em",
        nullable = false,
        updatable = false
    )
    val criadoEm: Instant = Instant.now(),

    @Column(name = "atualizado_em", nullable = false)
    var atualizadoEm: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por_id")
    var atualizadoPor: Usuario? = null,

    @Column(name = "encerrado_em")
    var encerradoEm: Instant? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encerrado_por_id")
    var encerradoPor: Usuario? = null,

    @Column(name = "motivo_encerramento", length = 500)
    var motivoEncerramento: String? = null,

    @Version
    var versao: Long? = null
)