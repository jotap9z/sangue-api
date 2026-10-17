package com.sangueplus.api.models

import com.sangueplus.api.dtos.ResultadoPreTriagem
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "pre_triagens")
class PreTriagem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doador_id", nullable = false)
    val doador: Usuario,

    @Column(nullable = false, precision = 5, scale = 2)
    val pesoKg: BigDecimal,

    @Column(nullable = false)
    val bemDeSaude: Boolean,

    @Column(nullable = false)
    val sintomasRecentes: Boolean,

    @Column(nullable = false)
    val cirurgiaRecente: Boolean,

    @Column(nullable = false)
    val tatuagemOuPiercingRecente: Boolean,

    @Column(nullable = false)
    val gravidaOuAmamentando: Boolean,

    @Column(nullable = false)
    val condicaoDeSaudeOuMedicamento: Boolean,

    @Column(nullable = false)
    val descansadoEAlimentado: Boolean,

    val autorizacaoResponsavel: Boolean? = null,

    @Column(length = 500)
    val observacoes: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val resultado: ResultadoPreTriagem,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "pre_triagem_motivos",
        joinColumns = [JoinColumn(name = "pre_triagem_id")]
    )
    @OrderColumn(name = "ordem")
    @Column(name = "motivo", nullable = false, length = 500)
    val motivos: MutableList<String> = mutableListOf(),

    @Column(nullable = false)
    val avaliadaEm: Instant = Instant.now(),

    @Column(nullable = false, length = 40)
    val versaoQuestionario: String = "2026-10-v1"
)