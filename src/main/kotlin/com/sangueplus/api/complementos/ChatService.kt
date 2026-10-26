package com.sangueplus.api.complementos

import com.sangueplus.api.services.AuthService
import com.sangueplus.api.services.AcessoEquipeService
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import java.sql.ResultSet
import java.time.Instant

data class ConversaReal(val id: Long, val doadorId: Long, val hemocentroId: Long, val nomeDoador: String, val nomeHemocentro: String, val ultimaMensagem: String? = null, val ultimaEnviadaEm: Instant? = null)
data class MensagemReal(val id: Long, val conversaId: Long, val remetenteId: Long, val nomeRemetente: String, val conteudo: String, val enviadaEm: Instant, val chaveEnvio: String)
data class EnviarMensagem(val conteudo: String, val chaveEnvio: String)

@Service
class ChatService(private val jdbc: JdbcTemplate, private val auth: AuthService, private val acesso: AcessoEquipeService) {
    private fun negar(): Nothing = throw ResponseStatusException(HttpStatus.FORBIDDEN, "Você não participa desta conversa.")
    private fun conversa(rs: ResultSet): ConversaReal = ConversaReal(rs.getLong("id"), rs.getLong("doador_id"), rs.getLong("hemocentro_id"), rs.getString("doador"), rs.getString("centro"), rs.getString("ultima_mensagem"), rs.getTimestamp("ultima_enviada_em")?.toInstant())
    private val selecao = """
        select c.*, u.nome doador, h.nome centro,
          (select left(m.conteudo,160) from api_mensagens m where m.conversa_id=c.id order by m.id desc limit 1) ultima_mensagem,
          (select m.enviada_em from api_mensagens m where m.conversa_id=c.id order by m.id desc limit 1) ultima_enviada_em
        from api_conversas c join usuarios u on u.id=c.doador_id join hemocentros h on h.id=c.hemocentro_id
    """

    @Transactional
    fun iniciar(authorization: String?, centro: Long): ConversaReal {
        val u = auth.exigirDoador(authorization)
        if (jdbc.queryForObject("select count(*) from hemocentros where id=?", Long::class.java, centro) != 1L)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Hemocentro não encontrado.")
        jdbc.update("insert into api_conversas(doador_id,hemocentro_id) values (?,?) on conflict do nothing", u.id, centro)
        return jdbc.query("$selecao where c.doador_id=? and c.hemocentro_id=?", { rs, _ -> conversa(rs) }, u.id, centro).single()
    }

    @Transactional(readOnly = true)
    fun listar(authorization: String?, antes: Long? = null, nome: String = ""): List<ConversaReal> {
        if (nome.length > 100) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Use até 100 caracteres na busca.")
        val u = auth.usuarioAtual(authorization)
        return if (u.tipoUsuario == "doador") {
            jdbc.query("$selecao where c.doador_id=? and c.id<? and position(lower(?) in lower(h.nome))>0 order by c.id desc limit 50", { rs, _ -> conversa(rs) }, u.id, antes ?: Long.MAX_VALUE, nome.trim())
        } else {
            val m = acesso.exigirMembro(authorization)
            jdbc.query("$selecao where c.hemocentro_id=? and c.id<? and position(lower(?) in lower(u.nome))>0 order by c.id desc limit 50", { rs, _ -> conversa(rs) }, m.hemocentro.id, antes ?: Long.MAX_VALUE, nome.trim())
        }
    }

    @Transactional(readOnly = true)
    fun consultar(authorization: String?, id: Long): ConversaReal {
        autorizar(authorization, id)
        return jdbc.query("$selecao where c.id=?", {rs,_->conversa(rs)}, id).single()
    }

    private fun autorizar(authorization: String?, id: Long, escrita: Boolean = false): Long {
        val u = auth.usuarioAtual(authorization)
        val c = jdbc.query("select doador_id,hemocentro_id from api_conversas where id=?", { rs, _ -> rs.getLong(1) to rs.getLong(2) }, id).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Conversa não encontrada.")
        if (u.tipoUsuario == "doador") {
            if (u.id != c.first) negar()
        } else {
            val m = acesso.exigirMembro(authorization)
            if (m.hemocentro.id != c.second) negar()
            if (escrita && m.permissao.name == "SOMENTE_VISUALIZACAO") negar()
        }
        return u.id
    }

    @Transactional(readOnly = true)
    fun mensagens(authorization: String?, id: Long, antes: Long?): List<MensagemReal> {
        autorizar(authorization, id)
        return jdbc.query("""select m.*,u.nome from api_mensagens m join usuarios u on u.id=m.remetente_id
            where m.conversa_id=? and m.id<? order by m.id desc limit 50""", { rs, _ -> mensagem(rs) }, id, antes ?: Long.MAX_VALUE).reversed()
    }
    private fun mensagem(rs: ResultSet) = MensagemReal(rs.getLong("id"), rs.getLong("conversa_id"), rs.getLong("remetente_id"), rs.getString("nome"), rs.getString("conteudo"), rs.getTimestamp("enviada_em").toInstant(), rs.getString("chave_envio"))

    @Transactional
    fun enviar(authorization: String?, id: Long, request: EnviarMensagem): MensagemReal {
        val uid = autorizar(authorization, id, true)
        val texto = request.conteudo.trim()
        if (texto.length !in 1..2000 || runCatching { UUID.fromString(request.chaveEnvio).toString() == request.chaveEnvio }.getOrDefault(false).not())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma mensagem de até 2000 caracteres e uma chave de envio UUID válida.")
        jdbc.update("insert into api_mensagens(conversa_id,remetente_id,conteudo,chave_envio) values (?,?,?,?) on conflict do nothing", id, uid, texto, request.chaveEnvio)
        val salva = jdbc.query("select m.*,u.nome from api_mensagens m join usuarios u on u.id=m.remetente_id where m.remetente_id=? and m.chave_envio=?", { rs, _ -> mensagem(rs) }, uid, request.chaveEnvio).single()
        if (salva.conversaId != id || salva.conteudo != texto) throw ResponseStatusException(HttpStatus.CONFLICT, "Chave de envio já utilizada para outra mensagem.")
        return salva
    }
}