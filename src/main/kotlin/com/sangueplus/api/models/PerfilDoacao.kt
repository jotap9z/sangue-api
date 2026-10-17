package com.sangueplus.api.models

import jakarta.persistence.*
import java.time.Instant

enum class RegraIntervalo(
    val meses: Long,
    val limiteDozeMeses: Int
) {
    DOIS_MESES_QUATRO_DOACOES(2, 4),
    TRES_MESES_TRES_DOACOES(3, 3)
}

@Entity
@Table(name = "api_perfis_doacao")
class PerfilDoacao(
    @Id
    @Column(name = "doador_id")
    val doadorId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var regra: RegraIntervalo,

    @Column(nullable = false)
    var atualizadoEm: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atualizado_por_id", nullable = false)
    var atualizadoPor: Usuario
)