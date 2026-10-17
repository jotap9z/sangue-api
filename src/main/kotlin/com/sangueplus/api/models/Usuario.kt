package com.sangueplus.api.models

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "usuarios")
data class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    var nome: String,

    @Column(nullable = false, unique = true, length = 100)
    var email: String,

    @Column(name = "senha_hash", nullable = false)
    var senhaHash: String,

    @Column(name = "tipo_usuario")
    var tipoUsuario: String = "doador",

    @Column(name = "tipo_sanguineo")
    var tipoSanguineo: String = "Nao_Sei",

    var telefone: String? = null,

    @Column(name = "data_nascimento")
    var dataNascimento: LocalDate? = null,

    @Column(name = "criado_em", insertable = false, updatable = false)
    val criadoEm: LocalDateTime? = null
)