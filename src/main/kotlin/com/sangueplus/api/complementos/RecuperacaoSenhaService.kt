package com.sangueplus.api.complementos

import com.sangueplus.api.models.Usuario
import com.sangueplus.api.repositories.UsuarioRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.sql.Timestamp
import java.util.Base64
import java.util.Locale

data class PedirRecuperacao(val email: String)
data class RedefinirSenha(val token: String, val novaSenha: String)

@Configuration
@EnableAsync
class ConfigRecuperacao {
    @Bean("recuperacaoExecutor")
    fun executor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize=1
        maxPoolSize=2
        queueCapacity=100
        setThreadNamePrefix("recuperacao-")
    }
}

@Service
class RecuperacaoSenhaService(
    private val usuarios: UsuarioRepository, private val jdbc: JdbcTemplate,
    private val mail: JavaMailSender, private val em: EntityManager,
    private val disponibilidade: ContaDisponivelService,
    private val transactionManager: org.springframework.transaction.PlatformTransactionManager,
    @Value("\${sangue.mail.from}") private val remetente: String
) {
    private fun hash(token: String): String = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }

    // O endpoint responde antes do processamento, igualmente para e-mails existentes e ausentes.
    @Async("recuperacaoExecutor")
    @Transactional
    fun solicitar(emailInformado: String) {
        val email=emailInformado.trim().lowercase(Locale.ROOT)
        if(email.length>100 || !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)) return
        val encontrado=usuarios.findByEmailIgnoreCase(email) ?: return
        val u=em.find(Usuario::class.java,encontrado.id,LockModeType.PESSIMISTIC_WRITE)
        em.refresh(u,LockModeType.PESSIMISTIC_WRITE)
        if(runCatching { disponibilidade.exigir(u.id) }.isFailure) return
        val recente=jdbc.queryForObject("select exists(select 1 from api_recuperacoes_senha where usuario_id=? and criada_em>current_timestamp-interval '2 minutes')",Boolean::class.java,u.id)==true
        if(recente) return
        val token=Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
        jdbc.update("insert into api_recuperacoes_senha(token_hash,usuario_id,senha_hash_origem,expira_em) values (?,?,?,?)",hash(token),u.id,u.senhaHash,Timestamp.from(Instant.now().plusSeconds(900)))
        // Nunca registrar o conteúdo desta mensagem, o token ou a senha em logs.
        val mensagem=SimpleMailMessage().apply {
            setFrom(remetente)
            setTo(u.email)
            subject="Sangue+ — recuperação de senha"
            text="Foi solicitada a recuperação da sua senha.\n\nNo aplicativo, toque em Esqueci minha senha e informe este código:\n\n$token\n\nVálido por 15 minutos e para um único uso. Se você não solicitou, ignore esta mensagem."
        }
        // Envia somente depois de confirmar o token no banco e liberar o bloqueio do usuário.
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            object : org.springframework.transaction.support.TransactionSynchronization {
                override fun afterCommit() {
                    try { mail.send(mensagem) } catch (_: Exception) {
                        org.springframework.transaction.support.TransactionTemplate(transactionManager).apply {
                            propagationBehavior = org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
                        }.executeWithoutResult {
                            jdbc.update("update api_recuperacoes_senha set utilizada_em=current_timestamp where token_hash=?",hash(token))
                        }
                    }
                }
            }
        )
    }

    @Transactional
    fun redefinir(request: RedefinirSenha): Map<String,String> {
        fun invalido(): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Código inválido, expirado ou já utilizado.")
        if(!Regex("^[A-Za-z0-9_-]{43}$").matches(request.token)) invalido()
        val tokenHash=hash(request.token)
        val id=jdbc.query("select usuario_id from api_recuperacoes_senha where token_hash=?",{rs,_->rs.getLong(1)},tokenHash).firstOrNull() ?: invalido()
        val u=em.find(Usuario::class.java,id,LockModeType.PESSIMISTIC_WRITE) ?: invalido()
        em.refresh(u,LockModeType.PESSIMISTIC_WRITE)
        val valido=jdbc.queryForObject("""select exists(select 1 from api_recuperacoes_senha where token_hash=? and usuario_id=? and utilizada_em is null
            and expira_em>current_timestamp and senha_hash_origem=?)""",Boolean::class.java,tokenHash,id,u.senhaHash)==true
        if(!valido) invalido()
        disponibilidade.exigir(id)
        val senha=request.novaSenha
        if(senha.length<6 || senha.toByteArray(Charsets.UTF_8).size>72 || senha.none{it.isUpperCase()} || senha.none{!it.isLetterOrDigit() && !it.isWhitespace()})
            throw ResponseStatusException(HttpStatus.BAD_REQUEST,"A senha deve ter pelo menos 6 caracteres, uma letra maiúscula e um caractere especial; máximo de 72 bytes.")
        val encoder=BCryptPasswordEncoder()
        if(encoder.matches(senha,u.senhaHash)) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Use uma senha diferente da atual.")
        u.senhaHash=requireNotNull(encoder.encode(senha))
        em.flush()
        jdbc.update("update api_recuperacoes_senha set utilizada_em=current_timestamp where usuario_id=? and utilizada_em is null",id)
        jdbc.update("delete from sessoes where usuario_id=?",id)
        return mapOf("mensagem" to "Senha redefinida. Entre com a nova senha.")
    }
}