package com.sangueplus.api.repositories

import com.sangueplus.api.models.Usuario
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UsuarioRepository : JpaRepository<Usuario, Long> {

    fun findByEmailIgnoreCase(email: String): Usuario?

    fun existsByEmailIgnoreCase(email: String): Boolean

    fun findByEmail(email: String): Usuario?

    fun existsByEmail(email: String): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    fun buscarParaAgendamento(
        @Param("id") id: Long
    ): Usuario?
}