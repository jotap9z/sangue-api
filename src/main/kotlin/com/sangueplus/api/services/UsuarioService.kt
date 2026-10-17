package com.sangueplus.api.services

import com.sangueplus.api.models.Usuario
import com.sangueplus.api.repositories.UsuarioRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period

@Service
class UsuarioService(private val repository: UsuarioRepository) {

    fun cadastrarDoador(usuario: Usuario): Usuario {
        // RN003: Validação de E-mail Único
        if (repository.existsByEmail(usuario.email)) {
            throw IllegalArgumentException("MSG016: Este E-mail já está cadastrado no sistema.")
        }

        // RN002: Validação da Força da Senha (Mínimo 6 chars, 1 Maiúscula, 1 Especial)
        val senhaRegex = "^(?=.*[A-Z])(?=.*[!@#\$%]).{6,}\$".toRegex()
        if (!senhaRegex.matches(usuario.senhaHash)) {
            throw IllegalArgumentException("MSG008: A senha deve conter no mínimo 6 caracteres, incluindo uma letra maiúscula e um caractere especial (!@#\$%).")
        }

        // RN004: Restrição Etária (Mínimo 16, Máximo 69 anos)
        usuario.dataNascimento?.let {
            val idade = Period.between(it, LocalDate.now()).years
            if (idade < 16 || idade > 69) {
                throw IllegalArgumentException("MSG021: Idade incompatível com os critérios regulamentares de doação de sangue.")
            }
        }

        // Define os valores padrão para garantir que é um Doador com status não verificado
        usuario.tipoUsuario = "doador"

        // Salva as informações validadas na base de dados PostgreSQL
        return repository.save(usuario)
    }
}