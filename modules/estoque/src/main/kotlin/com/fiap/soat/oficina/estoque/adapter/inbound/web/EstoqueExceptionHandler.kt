package com.fiap.soat.oficina.estoque.adapter.inbound.web

import com.fiap.soat.oficina.estoque.adapter.inbound.web.dto.ErroResponse
import com.fiap.soat.oficina.estoque.domain.error.ConflitoException
import com.fiap.soat.oficina.estoque.domain.error.DadoInvalidoException
import com.fiap.soat.oficina.estoque.domain.error.DuplicidadeException
import com.fiap.soat.oficina.estoque.domain.error.EstoqueException
import com.fiap.soat.oficina.estoque.domain.error.RegistroNaoEncontradoException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class EstoqueExceptionHandler {

    @ExceptionHandler(EstoqueException::class)
    fun tratar(e: EstoqueException): ResponseEntity<ErroResponse> {
        val (status, campo) = when (e) {
            is DadoInvalidoException -> HttpStatus.BAD_REQUEST to e.campo
            is RegistroNaoEncontradoException -> HttpStatus.NOT_FOUND to null
            is DuplicidadeException -> HttpStatus.CONFLICT to e.campo
            is ConflitoException -> HttpStatus.CONFLICT to null
        }
        return ResponseEntity.status(status).body(ErroResponse(e.message ?: "Erro no estoque", campo))
    }
}
