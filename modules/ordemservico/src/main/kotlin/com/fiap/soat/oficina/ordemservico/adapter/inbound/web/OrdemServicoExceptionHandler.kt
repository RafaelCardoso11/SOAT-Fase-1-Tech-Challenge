package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class OrdemServicoExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun tratarDadosInvalidos(e: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("erro" to (e.message ?: "Dados inválidos")))

    @ExceptionHandler(NoSuchElementException::class)
    fun tratarNaoEncontrado(e: NoSuchElementException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("erro" to (e.message ?: "Não encontrado")))

    @ExceptionHandler(IllegalStateException::class)
    fun tratarEstadoInvalido(e: IllegalStateException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(mapOf("erro" to (e.message ?: "Operação incompatível com o status atual")))

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun tratarConcorrencia(e: ObjectOptimisticLockingFailureException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(mapOf("erro" to "A ordem foi alterada por outra operação; tente novamente"))
}
