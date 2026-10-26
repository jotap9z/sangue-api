package com.sangueplus.api.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "hemocentros")
class Hemocentro(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 150)
    var nome: String,

    @Column(nullable = false, length = 255)
    var endereco: String,

    @Column(nullable = false, length = 100)
    var cidade: String,

    @Column(nullable = false, length = 50)
    var estado: String,

    @Column(length = 20)
    var telefone: String? = null,

    @Column(name = "criado_em", updatable = false)
    val criadoEm: LocalDateTime = LocalDateTime.now(),

    @Column(length = 14, unique = true)
    var cnpj: String? = null,

    @Column(length = 7, unique = true)
    var cnes: String? = null,

    @Column(name = "email_contato", length = 100)
    var emailContato: String? = null,

    var latitude: Double? = null,
    var longitude: Double? = null
)