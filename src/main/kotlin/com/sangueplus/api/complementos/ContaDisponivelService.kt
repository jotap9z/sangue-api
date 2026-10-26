package com.sangueplus.api.complementos

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class ContaDisponivelService(private val jdbc: JdbcTemplate) {
    fun exigir(id: Long) {
        val bloqueada = jdbc.queryForObject(
            "select exists(select 1 from api_solicitacoes_exclusao where usuario_id=?)",
            Boolean::class.java, id
        ) == true
        if (bloqueada) throw ResponseStatusException(HttpStatus.FORBIDDEN,
            "Esta conta possui solicitação de exclusão e está com acesso bloqueado.")
    }
}