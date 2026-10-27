package com.fiap.soat.oficina.autenticacao.adapter.inbound.web

import com.fiap.soat.oficina.autenticacao.domain.error.CredenciaisInvalidasException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AutenticacaoExceptionHandler {

    @ExceptionHandler(CredenciaisInvalidasException::class)
    fun tratarCredenciaisInvalidas(
        e: CredenciaisInvalidasException,
    ): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("erro" to (e.message ?: "Credenciais inválidas")))
}
