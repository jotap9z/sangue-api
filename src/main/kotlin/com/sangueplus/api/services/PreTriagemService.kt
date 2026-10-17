package com.sangueplus.api.services

import com.sangueplus.api.dtos.PreTriagemRequest
import com.sangueplus.api.dtos.PreTriagemResponse
import com.sangueplus.api.dtos.ResultadoPreTriagem
import com.sangueplus.api.models.PreTriagem
import com.sangueplus.api.repositories.PreTriagemRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

@Service
class PreTriagemService(
    private val repository: PreTriagemRepository,
    private val authService: AuthService
) {
    @Transactional
    fun responder(
        authorization: String?,
        request: PreTriagemRequest
    ): PreTriagemResponse {
        val doador = authService.exigirDoador(authorization)

        val nascimento = doador.dataNascimento
            ?: invalido(
                "Complete sua data de nascimento antes de responder."
            )

        val hoje = LocalDate.now(
            ZoneId.of("America/Sao_Paulo")
        )

        validar(
            !nascimento.isAfter(hoje),
            "A data de nascimento cadastrada é inválida."
        )

        val idade = Period.between(nascimento, hoje).years

        val peso = request.pesoKg
            ?: invalido("Informe seu peso.")

        validar(
            peso > BigDecimal.ZERO &&
                    peso <= BigDecimal("500"),
            "Informe um peso válido, maior que zero e até 500 kg."
        )

        validar(
            peso.stripTrailingZeros().scale() <= 2,
            "Informe o peso com no máximo duas casas decimais."
        )

        val bemDeSaude = resposta(
            request.bemDeSaude,
            "Você está bem de saúde?"
        )

        val sintomas = resposta(
            request.sintomasRecentes,
            "Você teve febre, gripe ou outros sintomas recentes?"
        )

        val cirurgia = resposta(
            request.cirurgiaRecente,
            "Você realizou cirurgia nos últimos 12 meses?"
        )

        val procedimento = resposta(
            request.tatuagemOuPiercingRecente,
            "Você fez tatuagem ou colocou piercing nos últimos 12 meses?"
        )

        val gestacao = resposta(
            request.gravidaOuAmamentando,
            "Você está grávida ou amamentando?"
        )

        val condicao = resposta(
            request.condicaoDeSaudeOuMedicamento,
            "Você possui condição de saúde ou usa medicamento?"
        )

        val preparo = resposta(
            request.descansadoEAlimentado,
            "Você está descansado e alimentado?"
        )

        if (idade < 18) {
            resposta(
                request.autorizacaoResponsavel,
                "Você possui autorização formal do responsável legal?"
            )
        }

        val observacoes = request.observacoes
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        validar(
            observacoes == null || observacoes.length <= 500,
            "As observações devem ter no máximo 500 caracteres."
        )

        val motivos = mutableListOf<String>()

        if (idade < 16) {
            motivos.add(
                "A idade informada está abaixo da idade mínima " +
                        "para candidatura à doação."
            )
        }

        if (idade < 18 && request.autorizacaoResponsavel != true) {
            motivos.add(
                "Menores de 18 anos precisam de autorização formal " +
                        "do responsável legal."
            )
        }

        if (idade >= 60) {
            motivos.add(
                "O hemocentro precisa avaliar seu histórico de " +
                        "doações e os critérios aplicáveis à sua faixa etária."
            )
        }

        if (peso < BigDecimal("50")) {
            motivos.add(
                "Seu peso está abaixo do requisito geral de 50 kg. " +
                        "Procure orientação do hemocentro."
            )
        }

        if (!bemDeSaude) {
            motivos.add(
                "Você informou não estar bem de saúde."
            )
        }

        if (sintomas) {
            motivos.add(
                "Sintomas recentes precisam ser avaliados. " +
                        "O prazo de espera depende da condição e da recuperação."
            )
        }

        if (cirurgia) {
            motivos.add(
                "Informe ao hemocentro o tipo e a data da cirurgia. " +
                        "Não há um prazo único para todos os procedimentos."
            )
        }

        if (procedimento) {
            motivos.add(
                "Informe o tipo, a data e as condições de realização " +
                        "da tatuagem ou piercing para avaliação do hemocentro."
            )
        }

        if (gestacao) {
            motivos.add(
                "Gestação ou amamentação exigem avaliação específica " +
                        "antes da doação."
            )
        }

        if (condicao) {
            motivos.add(
                "Informe ao hemocentro sua condição de saúde e os " +
                        "medicamentos utilizados. Não interrompa tratamento " +
                        "para tentar doar."
            )
        }

        if (!preparo) {
            motivos.add(
                "É necessário estar descansado e alimentado. " +
                        "Consulte as orientações de preparo do hemocentro."
            )
        }

        if (observacoes != null) {
            motivos.add(
                "Você adicionou observações que precisam ser " +
                        "conversadas com a equipe do hemocentro."
            )
        }

        val resultado = if (motivos.isEmpty()) {
            ResultadoPreTriagem.SEM_ALERTAS_INICIAIS
        } else {
            ResultadoPreTriagem.NECESSITA_AVALIACAO
        }

        val triagem = PreTriagem(
            doador = doador,
            pesoKg = peso,
            bemDeSaude = bemDeSaude,
            sintomasRecentes = sintomas,
            cirurgiaRecente = cirurgia,
            tatuagemOuPiercingRecente = procedimento,
            gravidaOuAmamentando = gestacao,
            condicaoDeSaudeOuMedicamento = condicao,
            descansadoEAlimentado = preparo,
            autorizacaoResponsavel = request.autorizacaoResponsavel,
            observacoes = observacoes,
            resultado = resultado,
            motivos = motivos
        )

        return PreTriagemResponse.from(
            repository.saveAndFlush(triagem)
        )
    }

    @Transactional(readOnly = true)
    fun consultarUltima(
        authorization: String?
    ): PreTriagemResponse {
        val doador = authService.exigirDoador(authorization)

        val triagem =
            repository.findFirstByDoador_IdOrderByAvaliadaEmDescIdDesc(
                doador.id
            ) ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Você ainda não respondeu à pré-triagem."
            )

        return PreTriagemResponse.from(triagem)
    }

    private fun resposta(
        valor: Boolean?,
        pergunta: String
    ): Boolean {
        return valor ?: invalido(
            "Responda à pergunta: $pergunta"
        )
    }

    private fun validar(
        condicao: Boolean,
        mensagem: String
    ) {
        if (!condicao) {
            invalido(mensagem)
        }
    }

    private fun invalido(mensagem: String): Nothing {
        throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            mensagem
        )
    }
}