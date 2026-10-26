package com.sangueplus.api.complementos

import com.sangueplus.api.services.AcessoEquipeService
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.core.task.TaskRejectedException
import jakarta.servlet.http.HttpServletRequest
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/api")
class ComplementosController(
    private val chat: ChatService, private val prefs: PreferenciasContaService,
    private val notas: NotificacoesService, private val recuperacao: RecuperacaoSenhaService,
    private val acesso: AcessoEquipeService, private val jdbc: JdbcTemplate
) {
    private val tentativas=ConcurrentHashMap<String,Pair<Long,Int>>()
    @PostMapping("/auth/recuperacao")
    fun recuperar(@RequestBody r: PedirRecuperacao, http: HttpServletRequest): Map<String,String> {
        val agora=System.currentTimeMillis()
        // Não confiar em X-Forwarded-For recebido diretamente do cliente.
        if(tentativas.size>10000) tentativas.entries.removeIf{agora-it.value.first>3600000}
        var permitido=false
        if(tentativas.size<20000 || tentativas.containsKey(http.remoteAddr)) {
            tentativas.compute(http.remoteAddr) { _,anterior ->
                val atual=anterior?.takeIf{agora-it.first<3600000} ?: (agora to 0)
                permitido=atual.second<10
                atual.first to (atual.second+1).coerceAtMost(11)
            }
        }
        if(permitido) try { recuperacao.solicitar(r.email) } catch(_: TaskRejectedException) { }
        return mapOf("mensagem" to "Se o e-mail estiver cadastrado e puder receber recuperação, enviaremos um código. Confira também a pasta de spam.")
    }
    @PostMapping("/auth/recuperacao/redefinir")
    fun redefinir(@RequestBody r: RedefinirSenha)=recuperacao.redefinir(r)

    @GetMapping("/conta/me/preferencias")
    fun preferencias(@RequestHeader(value="Authorization",required=false) a: String?)=prefs.consultar(a)
    @PutMapping("/conta/me/preferencias")
    fun salvarPreferencias(@RequestHeader(value="Authorization",required=false) a: String?,@RequestBody r: PreferenciasReais)=prefs.salvar(a,r)
    @PostMapping("/conta/me/solicitacao-exclusao")
    fun excluir(@RequestHeader(value="Authorization",required=false) a: String?,@RequestBody r: SolicitarExclusao)=prefs.solicitarExclusao(a,r)

    @GetMapping("/conta/me/notificacoes")
    fun notificacoes(@RequestHeader(value="Authorization",required=false) a: String?,@RequestParam(required=false) antes: Long?)=notas.listar(a,antes)
    @PostMapping("/conta/me/notificacoes/{id}/ler")
    fun ler(@RequestHeader(value="Authorization",required=false) a: String?,@PathVariable id: Long)=notas.ler(a,id)

    @PostMapping("/doadores/me/conversas/{hemocentroId}")
    fun iniciar(@RequestHeader(value="Authorization",required=false) a: String?,@PathVariable hemocentroId: Long)=chat.iniciar(a,hemocentroId)
    @GetMapping("/conta/me/conversas")
    fun conversas(@RequestHeader(value="Authorization",required=false) a: String?,@RequestParam(required=false) antes: Long?,@RequestParam(defaultValue="") nome: String)=chat.listar(a,antes,nome)
    @GetMapping("/conta/me/conversas/{id}")
    fun conversa(@RequestHeader(value="Authorization",required=false) a: String?,@PathVariable id: Long)=chat.consultar(a,id)
    @GetMapping("/conta/me/conversas/{id}/mensagens")
    fun mensagens(@RequestHeader(value="Authorization",required=false) a: String?,@PathVariable id: Long,@RequestParam(required=false) antes: Long?)=chat.mensagens(a,id,antes)
    @PostMapping("/conta/me/conversas/{id}/mensagens")
    fun enviar(@RequestHeader(value="Authorization",required=false) a: String?,@PathVariable id: Long,@RequestBody r: EnviarMensagem)=chat.enviar(a,id,r)

    @GetMapping("/hemocentros/me/relatorios")
    fun relatorio(@RequestHeader(value="Authorization",required=false) a: String?,@RequestParam de: LocalDate,@RequestParam ate: LocalDate): Map<String,Any> {
        val m=acesso.exigirAdministrador(a)
        if(ate<de || java.time.temporal.ChronoUnit.DAYS.between(de,ate)>365)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe um período de até 366 dias.")
        val estados=jdbc.query("""select a.status,count(*) total from api_agendamentos a join api_horarios_doacao h on h.id=a.horario_id
            where h.hemocentro_id=? and h.inicio>=? and h.inicio<? group by a.status order by a.status""",{rs,_->mapOf("status" to rs.getString(1),"total" to rs.getLong(2))},m.hemocentro.id,de.atStartOfDay(),ate.plusDays(1).atStartOfDay())
        val doacoes=jdbc.query("""select tipo_sanguineo_confirmado,count(*) total from api_doacoes where hemocentro_id=? and realizada_em>=? and realizada_em<?
            group by tipo_sanguineo_confirmado order by tipo_sanguineo_confirmado""",{rs,_->mapOf("tipoSanguineo" to rs.getString(1),"total" to rs.getLong(2))},m.hemocentro.id,java.sql.Timestamp.from(de.atStartOfDay(java.time.ZoneId.of("America/Sao_Paulo")).toInstant()),java.sql.Timestamp.from(ate.plusDays(1).atStartOfDay(java.time.ZoneId.of("America/Sao_Paulo")).toInstant()))
        return mapOf("hemocentroId" to m.hemocentro.id,"de" to de,"ate" to ate,"fusoHorario" to "America/Sao_Paulo","agendamentosPorStatus" to estados,"doacoesPorTipo" to doacoes)
    }
}