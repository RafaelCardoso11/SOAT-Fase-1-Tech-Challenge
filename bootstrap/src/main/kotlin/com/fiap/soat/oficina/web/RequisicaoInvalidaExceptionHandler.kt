package com.fiap.soat.oficina.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class RequisicaoInvalidaExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun tratarCorpoInvalido(e: HttpMessageNotReadableException): ResponseEntity<Map<String, String>> =
        erro("Corpo da requisição inválido: JSON malformado, campo obrigatório ausente ou tipo incorreto")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun tratarParametroInvalido(e: MethodArgumentTypeMismatchException): ResponseEntity<Map<String, String>> =
        erro("Parâmetro inválido: ${e.name}")

    private fun erro(mensagem: String) = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("erro" to mensagem))
}
