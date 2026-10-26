package com.sangueplus.api.complementos

import com.sangueplus.api.models.Usuario
import com.sangueplus.api.services.AuthService
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType

data class PreferenciasReais(val avisosAgenda: Boolean = true, val avisosEmergencia: Boolean = true, val avisosMensagens: Boolean = true)
data class SolicitarExclusao(val senhaAtual: String)

@Service
class PreferenciasContaService(private val jdbc: JdbcTemplate, private val auth: AuthService, private val em: EntityManager) {
    @Transactional(readOnly = true)
    fun consultar(authorization: String?): PreferenciasReais {
        val id = auth.usuarioAtual(authorization).id
        return jdbc.query("select * from api_preferencias where usuario_id=?", { rs, _ -> PreferenciasReais(rs.getBoolean("avisos_agenda"),rs.getBoolean("avisos_emergencia"),rs.getBoolean("avisos_mensagens")) }, id).firstOrNull() ?: PreferenciasReais()
    }
    @Transactional
    fun salvar(authorization: String?, p: PreferenciasReais): PreferenciasReais {
        val id = auth.usuarioAtual(authorization).id
        jdbc.update("""insert into api_preferencias(usuario_id,avisos_agenda,avisos_emergencia,avisos_mensagens) values (?,?,?,?)
            on conflict(usuario_id) do update set avisos_agenda=excluded.avisos_agenda, avisos_emergencia=excluded.avisos_emergencia, avisos_mensagens=excluded.avisos_mensagens""", id,p.avisosAgenda,p.avisosEmergencia,p.avisosMensagens)
        return p
    }
    @Transactional
    fun solicitarExclusao(authorization: String?, request: SolicitarExclusao): Map<String,String> {
        val id = auth.usuarioAtual(authorization).id
        val u = em.find(Usuario::class.java,id,LockModeType.PESSIMISTIC_WRITE)
        em.refresh(u,LockModeType.PESSIMISTIC_WRITE)
        if (request.senhaAtual.toByteArray().size > 72 || !BCryptPasswordEncoder().matches(request.senhaAtual,u.senhaHash))
            throw ResponseStatusException(HttpStatus.BAD_REQUEST,"A senha atual está incorreta.")
        if (jdbc.queryForObject("select exists(select 1 from api_vinculos_equipe where usuario_id=? and administrador_principal=true)",Boolean::class.java,id)==true)
            throw ResponseStatusException(HttpStatus.CONFLICT,"Transfira a função de administrador principal antes de solicitar a exclusão.")
        if (jdbc.queryForObject("select exists(select 1 from api_agendamentos a join api_horarios_doacao h on h.id=a.horario_id where a.doador_id=? and (a.status in ('EM_ATENDIMENTO','APROVADO_TRIAGEM') or (a.status='AGENDADO' and h.inicio>=current_timestamp at time zone 'America/Sao_Paulo')))",Boolean::class.java,id)==true)
            throw ResponseStatusException(HttpStatus.CONFLICT,"Cancele seus agendamentos ou finalize o atendimento em andamento antes de solicitar a exclusão.")
        jdbc.update("insert into api_solicitacoes_exclusao(usuario_id) values (?) on conflict do nothing",id)
        jdbc.update("delete from sessoes where usuario_id=?",id)
        return mapOf("mensagem" to "Solicitação registrada. O acesso foi bloqueado. Os dados ainda não foram apagados; o tratamento dos registros permanece pendente.")
    }
}