package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class OrdemServicoExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun tratarDadosInvalidos(e: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("erro" to (e.message ?: "Dados inválidos")))
}
