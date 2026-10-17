package com.sangueplus.api.services

import com.sangueplus.api.dtos.*
import com.sangueplus.api.models.EstoqueSangue
import com.sangueplus.api.models.Hemocentro
import com.sangueplus.api.repositories.EstoqueSangueRepository
import com.sangueplus.api.repositories.HemocentroRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class EstoqueService(
    private val repository: EstoqueSangueRepository,
    private val hemocentros: HemocentroRepository,
    private val auth: AuthService,
    private val entityManager: EntityManager
) {
    private val tipos = listOf(
        "O+", "O-",
        "A+", "A-",
        "B+", "B-",
        "AB+", "AB-"
    )

    @Transactional(readOnly = true)
    fun consultarPublico(
        hemocentroId: Long
    ): EstoqueHemocentroResponse {
        val hemocentro = hemocentros.findById(hemocentroId)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hemocentro não encontrado."
                )
            }

        return montarResposta(hemocentro)
    }

    @Transactional(readOnly = true)
    fun consultarMeu(
        authorization: String?
    ): EstoqueHemocentroResponse {
        val responsavel = auth.exigirHemocentro(authorization)

        val hemocentro = responsavel.hemocentro
            ?: falhar(
                HttpStatus.FORBIDDEN,
                "Sua conta não está vinculada a um hemocentro."
            )

        return montarResposta(hemocentro)
    }

    @Transactional
    fun atualizar(
        authorization: String?,
        tipoSanguineo: String,
        request: AtualizarEstoqueRequest
    ): EstoqueItemResponse {
        val responsavel = auth.exigirHemocentro(authorization)

        validar(
            tipoSanguineo in tipos,
            "Tipo sanguíneo inválido."
        )

        val quantidade = request.quantidadeDisponivel
            ?: falhar(
                HttpStatus.BAD_REQUEST,
                "Informe a quantidade disponível."
            )

        val minimo = request.nivelMinimo
            ?: falhar(
                HttpStatus.BAD_REQUEST,
                "Informe o nível mínimo."
            )

        val ideal = request.nivelIdeal
            ?: falhar(
                HttpStatus.BAD_REQUEST,
                "Informe o nível ideal."
            )

        validar(
            quantidade in 0..1_000_000,
            "A quantidade deve estar entre zero e 1000000."
        )

        validar(
            minimo in 1..1_000_000,
            "O nível mínimo deve estar entre 1 e 1000000."
        )

        validar(
            ideal in 1..1_000_000 && ideal > minimo,
            "O nível ideal deve ser maior que o mínimo " +
                    "e não pode ultrapassar 1000000."
        )

        validar(
            request.versao == null || request.versao >= 0,
            "A versão não pode ser negativa."
        )

        val hemocentroId = responsavel.hemocentro?.id
            ?: falhar(
                HttpStatus.FORBIDDEN,
                "Sua conta não está vinculada a um hemocentro."
            )

        // Serializa alterações da instituição, inclusive a criação
        // do primeiro registro de cada tipo sanguíneo.
        val hemocentro = entityManager.find(
            Hemocentro::class.java,
            hemocentroId,
            LockModeType.PESSIMISTIC_WRITE
        ) ?: falhar(
            HttpStatus.NOT_FOUND,
            "Hemocentro não encontrado."
        )

        val existente =
            repository.findByHemocentro_IdAndTipoSanguineo(
                hemocentroId,
                tipoSanguineo
            )

        val estoque: EstoqueSangue

        if (existente == null) {
            if (request.versao != null) {
                falhar(
                    HttpStatus.CONFLICT,
                    "O estoque mudou. Consulte novamente antes de salvar."
                )
            }

            estoque = EstoqueSangue(
                hemocentro = hemocentro,
                tipoSanguineo = tipoSanguineo,
                quantidadeDisponivel = quantidade,
                nivelMinimo = minimo,
                nivelIdeal = ideal,
                atualizadoPor = responsavel
            )
        } else {
            if (request.versao != existente.versao) {
                falhar(
                    HttpStatus.CONFLICT,
                    "O estoque foi atualizado por outra sessão. " +
                            "Consulte novamente antes de salvar."
                )
            }

            existente.quantidadeDisponivel = quantidade
            existente.nivelMinimo = minimo
            existente.nivelIdeal = ideal
            existente.atualizadoEm = Instant.now()
            existente.atualizadoPor = responsavel

            estoque = existente
        }

        return item(
            repository.saveAndFlush(estoque)
        )
    }

    private fun montarResposta(
        hemocentro: Hemocentro
    ): EstoqueHemocentroResponse {
        val registros = repository
            .findByHemocentro_Id(hemocentro.id)
            .associateBy { it.tipoSanguineo }

        return EstoqueHemocentroResponse(
            hemocentroId = hemocentro.id,
            nomeHemocentro = hemocentro.nome,
            itens = tipos.map { tipo ->
                val estoque = registros[tipo]

                if (estoque != null) {
                    item(estoque)
                } else {
                    EstoqueItemResponse(
                        tipoSanguineo = tipo,
                        quantidadeDisponivel = null,
                        nivelMinimo = null,
                        nivelIdeal = null,
                        situacao = SituacaoEstoque.NAO_INFORMADO,
                        atualizadoEm = null,
                        versao = null
                    )
                }
            }
        )
    }

    private fun item(
        estoque: EstoqueSangue
    ): EstoqueItemResponse {
        val situacao = when {
            estoque.quantidadeDisponivel < estoque.nivelMinimo ->
                SituacaoEstoque.CRITICO

            estoque.quantidadeDisponivel < estoque.nivelIdeal ->
                SituacaoEstoque.BAIXO

            else -> SituacaoEstoque.NORMAL
        }

        return EstoqueItemResponse(
            tipoSanguineo = estoque.tipoSanguineo,
            quantidadeDisponivel = estoque.quantidadeDisponivel,
            nivelMinimo = estoque.nivelMinimo,
            nivelIdeal = estoque.nivelIdeal,
            situacao = situacao,
            atualizadoEm = estoque.atualizadoEm,
            versao = estoque.versao
        )
    }

    private fun validar(
        condicao: Boolean,
        mensagem: String
    ) {
        if (!condicao) {
            falhar(HttpStatus.BAD_REQUEST, mensagem)
        }
    }

    private fun falhar(
        status: HttpStatus,
        mensagem: String
    ): Nothing {
        throw ResponseStatusException(status, mensagem)
    }
}