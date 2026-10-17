package com.sangueplus.api.models

import jakarta.persistence.*
import java.time.Instant

enum class StatusAgendamento {
    AGENDADO,
    EM_ATENDIMENTO,
    APROVADO_TRIAGEM,
    NAO_APTO_TRIAGEM,
    DOACAO_REALIZADA,
    CANCELADO_DOADOR,
    CANCELADO_HEMOCENTRO
}

@Entity
@Table(
    name = "api_agendamentos",
    indexes = [
        Index(
            name = "idx_api_agendamento_horario_status",
            columnList = "horario_id,status"
        ),
        Index(
            name = "idx_api_agendamento_doador_status",
            columnList = "doador_id,status"
        )
    ]
)
class Agendamento(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doador_id", nullable = false)
    val doador: Usuario,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horario_id", nullable = false)
    val horario: HorarioDoacao,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pre_triagem_id", nullable = false)
    val preTriagem: PreTriagem,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    var status: StatusAgendamento = StatusAgendamento.AGENDADO,

    @Column(nullable = false)
    val criadoEm: Instant = Instant.now(),

    var canceladoEm: Instant? = null,

    @Column(length = 250)
    var motivoCancelamento: String? = null,

    var chegadaEm: Instant? = null,

    var triagemPresencialEm: Instant? = null,

    @Column(length = 500)
    var observacaoTriagem: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_triagem_id")
    var responsavelTriagem: Usuario? = null
)