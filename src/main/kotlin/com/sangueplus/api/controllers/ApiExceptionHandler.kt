package com.sangueplus.api.controllers

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException::class)
    fun tratarRegra(
        exception: ResponseStatusException
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(exception.statusCode)
            .body(
                mapOf(
                    "erro" to (
                            exception.reason
                                ?: "Requisição inválida."
                            )
                )
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun tratarJson(): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                mapOf(
                    "erro" to
                            "Dados inválidos ou incompletos. " +
                            "Use AAAA-MM-DD para datas."
                )
            )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun tratarConflito(): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                mapOf(
                    "erro" to
                            "Os dados conflitam com um registro existente."
                )
            )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun tratarMetodo(): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(
                mapOf(
                    "erro" to
                            "Método HTTP não permitido para este endereço."
                )
            )
    }
}