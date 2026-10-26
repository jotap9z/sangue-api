package com.sangueplus.api.complementos

import com.sangueplus.api.services.AuthService
import com.sangueplus.api.services.AcessoEquipeService
import com.sangueplus.api.services.IntervaloService
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

data class NotificacaoReal(val id: Long, val titulo: String, val texto: String, val destino: String, val referenciaId: Long?, val criadaEm: Instant, val lidaEm: Instant?)

@Service
class NotificacoesService(private val jdbc: JdbcTemplate, private val auth: AuthService, private val acesso: AcessoEquipeService, private val preferencias: PreferenciasContaService, private val intervalo: IntervaloService) {
    private fun criar(uid: Long, evento: String, titulo: String, texto: String, destino: String, referencia: Long?) {
        jdbc.update("insert into api_notificacoes(usuario_id,evento,titulo,texto,destino,referencia_id) values (?,?,?,?,?,?) on conflict do nothing",uid,evento,titulo,texto.take(500),destino,referencia)
    }
    // Reconciliação ao abrir/atualizar a caixa: eventos persistidos, sem push ou alegação de entrega em segundo plano.
    @Transactional
    fun listar(authorization: String?, antes: Long?): List<NotificacaoReal> {
        val u=auth.usuarioAtual(authorization)
        val p=preferencias.consultar(authorization)
        if(u.tipoUsuario=="doador") {
            if(p.avisosAgenda) {
                jdbc.query("""select a.id,a.status,h.inicio from api_agendamentos a join api_horarios_doacao h on h.id=a.horario_id
                    where a.doador_id=? order by a.id desc limit 30""",{rs,_->Triple(rs.getLong(1),rs.getString(2),rs.getTimestamp(3).toLocalDateTime())},u.id).forEach { (id,status,data) ->
                    criar(u.id,"reserva:$id:$status","Agendamento #$id","Situação: $status. Horário: $data (horário do hemocentro: America/Sao_Paulo).","checkin",id)
                }
                val i=intervalo.consultarMeu(authorization)
                if(i.situacao.name=="INTERVALO_CUMPRIDO" && i.ultimaDoacao!=null)
                    criar(u.id,"intervalo:${i.ultimaDoacao}:${i.proximaDataPeloHistorico}","Previsão de intervalo cumprido","O intervalo pelo histórico foi cumprido. A avaliação presencial continua obrigatória.","home",null)
            }
            if(p.avisosEmergencia) {
                jdbc.query("""select c.id,c.titulo,h.nome from api_chamados_emergencia c join hemocentros h on h.id=c.hemocentro_id
                    where c.status='ATIVO' and lower(h.cidade)=lower(?) and (c.tipo_sanguineo=? or ?='Nao_Sei') order by c.id desc limit 30""",{rs,_->Triple(rs.getLong(1),rs.getString(2),rs.getString(3))},u.cidade.orEmpty(),u.tipoSanguineo,u.tipoSanguineo).forEach { (id,titulo,nome) ->
                    criar(u.id,"chamado:$id",titulo,"$nome publicou um chamado. Responder indica interesse; não registra doação.","emergencia",id)
                }
            }
        }
        if(u.tipoUsuario=="hemocentro" && p.avisosEmergencia) {
            val m=acesso.exigirMembro(authorization)
            if(m.permissao.name=="ADMINISTRADOR" || m.funcao.name=="ESTOQUE") {
                jdbc.query("select id,tipo_sanguineo,versao from api_estoques_sangue where hemocentro_id=? and quantidade_disponivel<nivel_minimo order by id limit 8",
                    {rs,_->Triple(rs.getLong(1),rs.getString(2),rs.getLong(3))},m.hemocentro.id).forEach { (id,tipo,versao) ->
                    criar(u.id,"estoque:$id:$versao","Estoque crítico","O tipo $tipo está abaixo do mínimo registrado pela instituição.","estoque",null)
                }
            }
        }
        if(p.avisosMensagens) {
            val centro=if(u.tipoUsuario=="hemocentro") acesso.exigirMembro(authorization).hemocentro.id else null
            jdbc.query("""select m.id,m.conversa_id from api_mensagens m join api_conversas c on c.id=m.conversa_id
                where m.remetente_id<>? and ((?=true and c.doador_id=?) or (?=false and c.hemocentro_id=?))
                order by m.id desc limit 50""",{rs,_->rs.getLong(1) to rs.getLong(2)},u.id,u.tipoUsuario=="doador",u.id,u.tipoUsuario=="doador",centro ?: 0L).forEach { (id,conversa) ->
                criar(u.id,"mensagem:$id","Nova mensagem","Há uma mensagem na sua conversa.","chat",conversa)
            }
        }
        return jdbc.query("select * from api_notificacoes where usuario_id=? and id<? order by id desc limit 50",{rs,_->
            NotificacaoReal(rs.getLong("id"),rs.getString("titulo"),rs.getString("texto"),rs.getString("destino"),rs.getObject("referencia_id")?.let{(it as Number).toLong()},rs.getTimestamp("criada_em").toInstant(),rs.getTimestamp("lida_em")?.toInstant())
        },u.id,antes ?: Long.MAX_VALUE)
    }
    @Transactional
    fun ler(authorization: String?, id: Long): Map<String,String> {
        val uid=auth.usuarioAtual(authorization).id
        if(jdbc.update("update api_notificacoes set lida_em=coalesce(lida_em,current_timestamp) where id=? and usuario_id=?",id,uid)!=1)
            throw ResponseStatusException(HttpStatus.NOT_FOUND,"Notificação não encontrada.")
        return mapOf("mensagem" to "Notificação marcada como lida.")
    }
}