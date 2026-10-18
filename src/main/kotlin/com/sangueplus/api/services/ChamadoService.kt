package com.sangueplus.api.services

import com.sangueplus.api.dtos.*
import com.sangueplus.api.models.*
import com.sangueplus.api.repositories.ChamadoEmergenciaRepository
import com.sangueplus.api.repositories.RespostaChamadoRepository
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.Locale

@Service
class ChamadoService(
    private val chamados: ChamadoEmergenciaRepository,
    private val respostas: RespostaChamadoRepository,
    private val auth: AuthService,
    private val acesso: AcessoEquipeService,
    private val entityManager: EntityManager
) {

    private val tipos = setOf(
        "A+", "A-", "B+", "B-",
        "AB+", "AB-", "O+", "O-"
    )

    private val estados = setOf(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF",
        "ES", "GO", "MA", "MT", "MS", "MG", "PA",
        "PB", "PR", "PE", "PI", "RJ", "RN", "RS",
        "RO", "RR", "SC", "SP", "SE", "TO"
    )

    @Transactional
    fun criar(
        authorization: String?,
        request: CriarChamadoRequest
    ): ChamadoInstitucionalResponse {
        val membro = acesso.exigirAcessoChamados(
            authorization,
            escrita = true
        )

        val tipo = validarTipo(request.tipoSanguineo)
        val titulo = validarTitulo(request.titulo, tipo)
        val mensagem = validarMensagem(request.mensagem)

        validarMeta(request.metaDoadores)

        val chamado = chamados.saveAndFlush(
            ChamadoEmergencia(
                hemocentro = membro.hemocentro,
                criadoPor = membro.usuario,
                titulo = titulo,
                tipoSanguineo = tipo,
                urgencia = request.urgencia,
                metaDoadores = request.metaDoadores,
                mensagem = mensagem,
                atualizadoPor = membro.usuario
            )
        )

        return institucional(chamado)
    }

    @Transactional(readOnly = true)
    fun listarInstitucionais(
        authorization: String?,
        status: StatusChamado?,
        pagina: Int,
        tamanho: Int
    ): PaginaChamadosResponse<ChamadoInstitucionalResponse> {
        val membro = acesso.exigirAcessoChamados(
            authorization
        )

        val paginacao = paginacao(
            pagina,
            tamanho,
            "criadoEm"
        )

        val encontrados = if (status == null) {
            chamados.findByHemocentro_Id(
                membro.hemocentro.id,
                paginacao
            )
        } else {
            chamados.findByHemocentro_IdAndStatus(
                membro.hemocentro.id,
                status,
                paginacao
            )
        }

        return paginaResposta(encontrados) {
            institucional(it)
        }
    }

    @Transactional(readOnly = true)
    fun consultarInstitucional(
        authorization: String?,
        id: Long
    ): ChamadoInstitucionalResponse {
        val membro = acesso.exigirAcessoChamados(
            authorization
        )

        val chamado = encontrar(id)

        verificarInstituicao(
            chamado,
            membro.hemocentro.id
        )

        return institucional(chamado)
    }

    @Transactional
    fun atualizar(
        authorization: String?,
        id: Long,
        request: AtualizarChamadoRequest
    ): ChamadoInstitucionalResponse {
        val membro = acesso.exigirAcessoChamados(
            authorization,
            escrita = true
        )

        val chamado = bloquear(id)

        verificarInstituicao(
            chamado,
            membro.hemocentro.id
        )

        exigirAtivo(chamado)
        verificarVersao(chamado, request.versao)

        val tipo = validarTipo(request.tipoSanguineo)
        val titulo = validarTitulo(request.titulo, tipo)
        val mensagem = validarMensagem(request.mensagem)

        validarMeta(request.metaDoadores)

        if (
            tipo != chamado.tipoSanguineo &&
            respostas.countByChamado_IdAndAtivaTrue(id) > 0
        ) {
            conflito(
                "Não é possível trocar o tipo sanguíneo de um " +
                        "chamado com interessados ativos. " +
                        "Encerre este chamado e publique outro."
            )
        }

        chamado.titulo = titulo
        chamado.tipoSanguineo = tipo
        chamado.urgencia = request.urgencia
        chamado.metaDoadores = request.metaDoadores
        chamado.mensagem = mensagem
        chamado.atualizadoEm = Instant.now()
        chamado.atualizadoPor = membro.usuario

        chamados.saveAndFlush(chamado)

        return institucional(chamado)
    }

    @Transactional
    fun encerrar(
        authorization: String?,
        id: Long,
        request: EncerrarChamadoRequest
    ): ChamadoInstitucionalResponse {
        val membro = acesso.exigirAcessoChamados(
            authorization,
            escrita = true
        )

        val chamado = bloquear(id)

        verificarInstituicao(
            chamado,
            membro.hemocentro.id
        )

        exigirAtivo(chamado)
        verificarVersao(chamado, request.versao)

        val motivo = request.motivo.trim()

        validar(
            motivo.length in 3..500,
            "Informe um motivo entre 3 e 500 caracteres."
        )

        val agora = Instant.now()

        chamado.status = StatusChamado.ENCERRADO
        chamado.motivoEncerramento = motivo
        chamado.encerradoEm = agora
        chamado.encerradoPor = membro.usuario
        chamado.atualizadoEm = agora
        chamado.atualizadoPor = membro.usuario

        chamados.saveAndFlush(chamado)

        return institucional(chamado)
    }

    @Transactional(readOnly = true)
    fun listarPublicos(
        cidade: String,
        estado: String,
        tipoSanguineo: String,
        pagina: Int,
        tamanho: Int
    ): PaginaChamadosResponse<ChamadoResponse> {
        val cidadeNormalizada = cidade.trim()

        val estadoNormalizado = estado.trim()
            .uppercase(Locale.ROOT)

        val tipoNormalizado = tipoSanguineo.trim()
            .uppercase(Locale.ROOT)

        validar(
            cidadeNormalizada.length <= 100,
            "O filtro de cidade deve ter no máximo 100 caracteres."
        )

        validar(
            estadoNormalizado.isEmpty() ||
                    estadoNormalizado in estados,
            "Informe uma UF válida."
        )

        validar(
            tipoNormalizado.isEmpty() ||
                    tipoNormalizado in tipos,
            "Informe um tipo sanguíneo válido."
        )

        val encontrados = chamados.buscarPublicos(
            StatusChamado.ATIVO,
            cidadeNormalizada,
            estadoNormalizado,
            tipoNormalizado,
            paginacao(pagina, tamanho, "criadoEm")
        )

        return paginaResposta(encontrados) {
            publico(it)
        }
    }

    @Transactional(readOnly = true)
    fun consultarPublico(
        id: Long
    ): ChamadoResponse {
        return publico(encontrar(id))
    }

    @Transactional
    fun responder(
        authorization: String?,
        id: Long
    ): MinhaRespostaChamadoResponse {
        val doador = auth.exigirDoador(authorization)
        val chamado = bloquear(id)

        exigirAtivo(chamado)

        val existente =
            respostas.findByChamado_IdAndDoador_Id(
                id,
                doador.id
            )

        val resposta = if (existente == null) {
            respostas.saveAndFlush(
                RespostaChamado(
                    chamado = chamado,
                    doador = doador
                )
            )
        } else {
            if (!existente.ativa) {
                existente.ativa = true
                existente.canceladaEm = null
                existente.atualizadaEm = Instant.now()

                respostas.saveAndFlush(existente)
            } else {
                existente
            }
        }

        return MinhaRespostaChamadoResponse.from(resposta)
    }

    @Transactional
    fun cancelarResposta(
        authorization: String?,
        id: Long
    ): MinhaRespostaChamadoResponse {
        val doador = auth.exigirDoador(authorization)

        bloquear(id)

        val resposta =
            respostas.findByChamado_IdAndDoador_Id(
                id,
                doador.id
            ) ?: naoEncontrado(
                "Você não possui resposta para este chamado."
            )

        if (resposta.ativa) {
            val agora = Instant.now()

            resposta.ativa = false
            resposta.canceladaEm = agora
            resposta.atualizadaEm = agora

            respostas.saveAndFlush(resposta)
        }

        return MinhaRespostaChamadoResponse.from(resposta)
    }

    @Transactional(readOnly = true)
    fun minhasRespostas(
        authorization: String?,
        pagina: Int,
        tamanho: Int
    ): PaginaChamadosResponse<MinhaRespostaChamadoResponse> {
        val doador = auth.exigirDoador(authorization)

        val encontradas = respostas.findByDoador_Id(
            doador.id,
            paginacao(pagina, tamanho, "atualizadaEm")
        )

        return paginaResposta(encontradas) {
            MinhaRespostaChamadoResponse.from(it)
        }
    }

    @Transactional(readOnly = true)
    fun listarInteressados(
        authorization: String?,
        id: Long,
        pagina: Int,
        tamanho: Int
    ): PaginaChamadosResponse<InteressadoChamadoResponse> {
        val membro = acesso.exigirAcessoChamados(
            authorization
        )

        val chamado = encontrar(id)

        verificarInstituicao(
            chamado,
            membro.hemocentro.id
        )

        val encontradas =
            respostas.findByChamado_IdAndAtivaTrue(
                id,
                paginacao(pagina, tamanho, "atualizadaEm")
            )

        return paginaResposta(encontradas) {
            InteressadoChamadoResponse.from(it)
        }
    }

    private fun encontrar(
        id: Long
    ): ChamadoEmergencia {
        return chamados.findById(id).orElse(null)
            ?: naoEncontrado("Chamado não encontrado.")
    }

    private fun bloquear(
        id: Long
    ): ChamadoEmergencia {
        val chamado = chamados.buscarParaAlteracao(id)
            ?: naoEncontrado("Chamado não encontrado.")

        entityManager.refresh(chamado)

        return chamado
    }

    private fun verificarInstituicao(
        chamado: ChamadoEmergencia,
        hemocentroId: Long
    ) {
        if (chamado.hemocentro.id != hemocentroId) {
            naoEncontrado("Chamado não encontrado.")
        }
    }

    private fun exigirAtivo(
        chamado: ChamadoEmergencia
    ) {
        if (chamado.status != StatusChamado.ATIVO) {
            conflito("Este chamado já está encerrado.")
        }
    }

    private fun verificarVersao(
        chamado: ChamadoEmergencia,
        versao: Long
    ) {
        if (chamado.versao != versao) {
            conflito(
                "O chamado foi atualizado por outra operação. " +
                        "Consulte novamente antes de salvar."
            )
        }
    }

    private fun publico(
        chamado: ChamadoEmergencia
    ): ChamadoResponse {
        return ChamadoResponse.from(
            chamado,
            respostas.countByChamado_IdAndAtivaTrue(
                chamado.id
            )
        )
    }

    private fun institucional(
        chamado: ChamadoEmergencia
    ): ChamadoInstitucionalResponse {
        return ChamadoInstitucionalResponse.from(
            chamado,
            respostas.countByChamado_IdAndAtivaTrue(
                chamado.id
            )
        )
    }

    private fun validarTipo(
        valor: String
    ): String {
        val tipo = valor.trim().uppercase(Locale.ROOT)

        validar(
            tipo in tipos,
            "Informe A+, A-, B+, B-, AB+, AB-, O+ ou O-."
        )

        return tipo
    }

    private fun validarTitulo(
        valor: String?,
        tipo: String
    ): String {
        val titulo = valor?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "Urgência $tipo"

        validar(
            titulo.length in 3..150,
            "O título deve ter entre 3 e 150 caracteres."
        )

        return titulo
    }

    private fun validarMensagem(
        valor: String
    ): String {
        val mensagem = valor.trim()

        validar(
            mensagem.length in 10..2000,
            "A mensagem deve ter entre 10 e 2000 caracteres."
        )

        return mensagem
    }

    private fun validarMeta(meta: Int) {
        validar(
            meta in 1..100000,
            "A meta deve estar entre 1 e 100000 doadores."
        )
    }

    private fun paginacao(
        pagina: Int,
        tamanho: Int,
        campo: String
    ): PageRequest {
        validar(
            pagina >= 0 && tamanho in 1..100,
            "Informe página a partir de zero e tamanho entre 1 e 100."
        )

        return PageRequest.of(
            pagina,
            tamanho,
            Sort.by(
                Sort.Order.desc(campo),
                Sort.Order.desc("id")
            )
        )
    }

    private fun <E : Any, R : Any> paginaResposta(
        pagina: Page<E>,
        transformar: (E) -> R
    ): PaginaChamadosResponse<R> {
        return PaginaChamadosResponse(
            itens = pagina.content.map(transformar),
            pagina = pagina.number,
            tamanho = pagina.size,
            totalElementos = pagina.totalElements,
            totalPaginas = pagina.totalPages
        )
    }

    private fun validar(
        condicao: Boolean,
        mensagem: String
    ) {
        if (!condicao) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                mensagem
            )
        }
    }

    private fun conflito(
        mensagem: String
    ): Nothing {
        throw ResponseStatusException(
            HttpStatus.CONFLICT,
            mensagem
        )
    }

    private fun naoEncontrado(
        mensagem: String
    ): Nothing {
        throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            mensagem
        )
    }
}