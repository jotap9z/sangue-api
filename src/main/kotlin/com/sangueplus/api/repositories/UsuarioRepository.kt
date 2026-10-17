package com.sangueplus.api.repositories

import com.sangueplus.api.models.Usuario
import org.springframework.data.jpa.repository.JpaRepository

interface UsuarioRepository : JpaRepository<Usuario, Long> {

    fun findByEmailIgnoreCase(email: String): Usuario?

    fun existsByEmailIgnoreCase(email: String): Boolean

    // Mantidos para compatibilidade com chamadas anteriores do projeto.
    fun findByEmail(email: String): Usuario?

    fun existsByEmail(email: String): Boolean
}