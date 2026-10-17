package com.sangueplus.api.services

import com.sangueplus.api.dtos.*
import com.sangueplus.api.models.*
import com.sangueplus.api.repositories.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class AgendamentoService(
    private val horarios: HorarioDoacaoRepository,
    private val agendamentos: AgendamentoRepository,
    private val usuarios: UsuarioRepository,
    private val hemocentros: HemocentroRepository,
    private val preTriagens: PreTriagemRepository,
    private val auth: AuthService
) {
    private val zona = ZoneId.of("America/Sao_Paulo")

    @Transactional
    fun criarHorario(
        authorization: String?,
        request: CriarHorarioRequest
    ): HorarioResponse {
        val responsavel = auth.exigirHemocentro(authorization)

        val hemocentro = responsavel.hemocentro
            ?: falhar(HttpStatus.FORBIDDEN, "Hemocentro não vinculado.")

        validar(
            request.inicio.isAfter(agora()),
            "O horário deve estar no futuro."
        )

        validar(
            request.inicio.second == 0 && request.inicio.nano == 0,
            "Informe o horário com precisão de minutos, sem segundos."
        )

        validar(
            request.capacidade in 1..100,
            "A capacidade deve estar entre 1 e 100 vagas."
        )

        if (
            horarios.existsByHemocentro_IdAndInicio(
                hemocentro.id,
                request.inicio
            )
        ) {
            falhar(
                HttpStatus.CONFLICT,
                "Já existe um horário cadastrado para esse início."
            )
        }

        val horario = horarios.saveAndFlush(
            HorarioDoacao(
                hemocentro = hemocentro,
                inicio = request.inicio,
                capacidade = request.capacidade
            )
        )

        return respostaHorario(horario)
    }

    @Transactional(readOnly = true)
    fun listarHorarios(
        hemocentroId: Long,
        de: LocalDate,
        ate: LocalDate
    ): List<HorarioResponse> {
        validarPeriodo(de, ate)

        if (!hemocentros.existsById(hemocentroId)) {
            falhar(HttpStatus.NOT_FOUND, "Hemocentro não encontrado.")
        }

        val instanteAtual = agora()

        return horarios
            .findByHemocentro_IdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                hemocentroId,
                de.atStartOfDay(),
                ate.plusDays(1).atStartOfDay()
            )
            .filter {
                it.ativo && it.inicio.isAfter(instanteAtual)
            }
            .map { respostaHorario(it) }
    }

    @Transactional
    fun reservar(
        authorization: String?,
        request: CriarAgendamentoRequest
    ): AgendamentoResponse {
        val autenticado = auth.exigirDoador(authorization)

        // Serializa reservas concorrentes feitas pelo mesmo doador.
        val doador = usuarios.buscarParaAgendamento(autenticado.id)
            ?: falhar(HttpStatus.NOT_FOUND, "Doador não encontrado.")

        // Serializa reservas concorrentes para a mesma capacidade.
        val horario = horarios.buscarParaReserva(request.horarioId)
            ?: falhar(HttpStatus.NOT_FOUND, "Horário não encontrado.")

        validar(
            horario.ativo,
            "Esse horário não está disponível para reservas."
        )

        validar(
            horario.inicio.isAfter(agora()),
            "Não é possível reservar um horário passado."
        )

        val triagem =
            preTriagens.findFirstByDoador_IdOrderByAvaliadaEmDescIdDesc(
                doador.id
            ) ?: falhar(
                HttpStatus.BAD_REQUEST,
                "Responda à pré-triagem antes de agendar."
            )

        if (
            agendamentos.existsByDoador_IdAndHorario_InicioAndStatus(
                doador.id,
                horario.inicio,
                StatusAgendamento.AGENDADO
            )
        ) {
            falhar(
                HttpStatus.CONFLICT,
                "Você já possui um agendamento ativo nesse horário."
            )
        }

        val ocupadas = agendamentos.countByHorario_IdAndStatus(
            horario.id,
            StatusAgendamento.AGENDADO
        )

        if (ocupadas >= horario.capacidade) {
            falhar(
                HttpStatus.CONFLICT,
                "Esse horário está sem vagas. Escolha outro."
            )
        }

        val agendamento = agendamentos.saveAndFlush(
            Agendamento(
                doador = doador,
                horario = horario,
                preTriagem = triagem
            )
        )

        return AgendamentoResponse.from(agendamento)
    }

    @Transactional(readOnly = true)
    fun meusAgendamentos(
        authorization: String?,
        de: LocalDate,
        ate: LocalDate
    ): List<AgendamentoResponse> {
        validarPeriodo(de, ate)

        val doador = auth.exigirDoador(authorization)

        return agendamentos
            .findByDoador_IdAndHorario_InicioGreaterThanEqualAndHorario_InicioLessThanOrderByHorario_InicioAsc(
                doador.id,
                de.atStartOfDay(),
                ate.plusDays(1).atStartOfDay()
            )
            .map { AgendamentoResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun agendaHemocentro(
        authorization: String?,
        de: LocalDate,
        ate: LocalDate
    ): List<AgendamentoResponse> {
        validarPeriodo(de, ate)

        val responsavel = auth.exigirHemocentro(authorization)

        val hemocentro = responsavel.hemocentro
            ?: falhar(HttpStatus.FORBIDDEN, "Hemocentro não vinculado.")

        return agendamentos
            .findByHorario_Hemocentro_IdAndHorario_InicioGreaterThanEqualAndHorario_InicioLessThanOrderByHorario_InicioAsc(
                hemocentro.id,
                de.atStartOfDay(),
                ate.plusDays(1).atStartOfDay()
            )
            .map { AgendamentoResponse.from(it) }
    }

    @Transactional
    fun cancelarDoador(
        authorization: String?,
        id: Long,
        request: CancelarAgendamentoRequest
    ): AgendamentoResponse {
        val doador = auth.exigirDoador(authorization)
        val agendamento = obterParaAlteracao(id)

        if (agendamento.doador.id != doador.id) {
            falhar(HttpStatus.NOT_FOUND, "Agendamento não encontrado.")
        }

        return cancelar(
            agendamento,
            StatusAgendamento.CANCELADO_DOADOR,
            request
        )
    }

    @Transactional
    fun cancelarHemocentro(
        authorization: String?,
        id: Long,
        request: CancelarAgendamentoRequest
    ): AgendamentoResponse {
        val responsavel = auth.exigirHemocentro(authorization)
        val agendamento = obterParaAlteracao(id)

        if (
            agendamento.horario.hemocentro.id !=
            responsavel.hemocentro?.id
        ) {
            falhar(HttpStatus.NOT_FOUND, "Agendamento não encontrado.")
        }

        return cancelar(
            agendamento,
            StatusAgendamento.CANCELADO_HEMOCENTRO,
            request
        )
    }

    private fun cancelar(
        agendamento: Agendamento,
        status: StatusAgendamento,
        request: CancelarAgendamentoRequest
    ): AgendamentoResponse {
        val motivo = request.motivo
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        validar(
            motivo == null || motivo.length <= 250,
            "O motivo deve ter no máximo 250 caracteres."
        )

        if (agendamento.status != StatusAgendamento.AGENDADO) {
            falhar(
                HttpStatus.CONFLICT,
                "Esse agendamento já foi cancelado."
            )
        }

        validar(
            agendamento.horario.inicio.isAfter(agora()),
            "Não é possível cancelar um horário passado."
        )

        agendamento.status = status
        agendamento.canceladoEm = Instant.now()
        agendamento.motivoCancelamento = motivo

        return AgendamentoResponse.from(
            agendamentos.saveAndFlush(agendamento)
        )
    }

    private fun obterParaAlteracao(id: Long): Agendamento {
        return agendamentos.buscarParaAlteracao(id)
            ?: falhar(
                HttpStatus.NOT_FOUND,
                "Agendamento não encontrado."
            )
    }

    private fun respostaHorario(
        horario: HorarioDoacao
    ): HorarioResponse {
        val ocupadas = agendamentos.countByHorario_IdAndStatus(
            horario.id,
            StatusAgendamento.AGENDADO
        )

        return HorarioResponse(
            id = horario.id,
            hemocentroId = horario.hemocentro.id,
            inicio = horario.inicio,
            capacidade = horario.capacidade,
            vagasDisponiveis =
                (horario.capacidade.toLong() - ocupadas).coerceAtLeast(0),
            ativo = horario.ativo
        )
    }

    private fun validarPeriodo(
        de: LocalDate,
        ate: LocalDate
    ) {
        validar(
            !ate.isBefore(de),
            "A data final não pode ser anterior à inicial."
        )

        validar(
            ChronoUnit.DAYS.between(de, ate) <= 30,
            "Consulte um período de no máximo 31 dias."
        )
    }

    private fun agora(): LocalDateTime =
        LocalDateTime.now(zona)

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