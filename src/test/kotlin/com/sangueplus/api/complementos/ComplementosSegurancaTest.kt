package com.sangueplus.api.complementos

import com.sangueplus.api.dtos.UsuarioResponse
import com.sangueplus.api.models.Usuario
import com.sangueplus.api.repositories.UsuarioRepository
import com.sangueplus.api.services.AuthService
import com.sangueplus.api.services.AcessoEquipeService
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.web.server.ResponseStatusException

class ComplementosSegurancaTest {
    private val jdbc = mock(JdbcTemplate::class.java)
    private val auth = mock(AuthService::class.java)
    private val acesso = mock(AcessoEquipeService::class.java)
    private fun doador() = UsuarioResponse(1,"Doador","doador@example.test","doador","O+",null,null,null)

    @Test
    fun `doador nao consulta mensagens de outra pessoa`() {
        `when`(auth.usuarioAtual("Bearer teste")).thenReturn(doador())
        `when`(jdbc.query(anyString(), any<RowMapper<Pair<Long,Long>>>(), anyLong())).thenReturn(listOf(2L to 7L))
        val erro = assertThrows(ResponseStatusException::class.java) {
            ChatService(jdbc,auth,acesso).mensagens("Bearer teste",10,null)
        }
        assertEquals(403,erro.statusCode.value())
        assertTrue(mockingDetails(jdbc).invocations.none { it.method.name == "update" })
    }

    @Test
    fun `resumo da conversa tambem protege dados de outros doadores`() {
        `when`(auth.usuarioAtual("Bearer teste")).thenReturn(doador())
        `when`(jdbc.query(anyString(), any<RowMapper<Pair<Long,Long>>>(), anyLong())).thenReturn(listOf(2L to 7L))
        val erro = assertThrows(ResponseStatusException::class.java) {
            ChatService(jdbc, auth, acesso).consultar("Bearer teste", 10)
        }
        assertEquals(403, erro.statusCode.value())
        assertEquals(1, mockingDetails(jdbc).invocations.count { it.method.name == "query" })
    }

    @Test
    fun `doador nao envia mensagem para conversa alheia`() {
        `when`(auth.usuarioAtual("Bearer teste")).thenReturn(doador())
        `when`(jdbc.query(anyString(), any<RowMapper<Pair<Long,Long>>>(), anyLong())).thenReturn(listOf(2L to 7L))
        val erro = assertThrows(ResponseStatusException::class.java) {
            ChatService(jdbc,auth,acesso).enviar("Bearer teste",10,EnviarMensagem("Mensagem","f1300f2b-287d-4ab7-a432-d4f07ab9752c"))
        }
        assertEquals(403,erro.statusCode.value())
        assertTrue(mockingDetails(jdbc).invocations.none { it.method.name == "update" })
    }

    @Test
    fun `codigo expirado ou utilizado nao altera senha nem sessoes`() {
        val em=mock(EntityManager::class.java)
        val usuarios=mock(UsuarioRepository::class.java)
        val mail=mock(JavaMailSender::class.java)
        val disponibilidade=mock(ContaDisponivelService::class.java)
        val u=Usuario(id=9,nome="Doador",email="doador@example.test",senhaHash="hash anterior")
        `when`(jdbc.query(anyString(),any<RowMapper<Long>>(),anyString())).thenReturn(listOf(9L))
        `when`(em.find(Usuario::class.java,9L,LockModeType.PESSIMISTIC_WRITE)).thenReturn(u)
        `when`(jdbc.queryForObject(anyString(),eq(Boolean::class.java),anyString(),anyLong(),anyString())).thenReturn(false)
        val erro=assertThrows(ResponseStatusException::class.java) {
            RecuperacaoSenhaService(usuarios,jdbc,mail,em,disponibilidade,mock(org.springframework.transaction.PlatformTransactionManager::class.java),"noreply@example.test")
                .redefinir(RedefinirSenha("A".repeat(43),"Nova@123"))
        }
        assertEquals(400,erro.statusCode.value())
        assertEquals("hash anterior",u.senhaHash)
        assertTrue(mockingDetails(jdbc).invocations.none { it.method.name == "update" })
        verify(em,never()).flush()
    }

    @Test
    fun `token malformado e recusado antes de consultar banco`() {
        val service=RecuperacaoSenhaService(mock(UsuarioRepository::class.java),jdbc,mock(JavaMailSender::class.java),mock(EntityManager::class.java),mock(ContaDisponivelService::class.java),mock(org.springframework.transaction.PlatformTransactionManager::class.java),"noreply@example.test")
        assertThrows(ResponseStatusException::class.java) { service.redefinir(RedefinirSenha("curto","Nova@123")) }
        verifyNoInteractions(jdbc)
    }
}