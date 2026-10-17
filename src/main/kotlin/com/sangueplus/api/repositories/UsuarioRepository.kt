package com.sangueplus.api.repositories

import com.sangueplus.api.models.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UsuarioRepository : JpaRepository<Usuario, Long> {
    // RN003: Unicidade de Registros - Verifica se o e-mail já existe
    fun existsByEmail(email: String): Boolean

    // UC01: Realizar Login - Busca o usuário pelo e-mail
    fun findByEmail(email: String): Usuario?
}