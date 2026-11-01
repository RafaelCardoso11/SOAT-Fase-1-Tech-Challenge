package com.fiap.soat.oficina.estoque.adapter.outbound.persistence

import com.fiap.soat.oficina.estoque.domain.error.DuplicidadeException
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLException

private const val UNIQUE_VIOLATION = "23505"

internal inline fun <T> traduzindoUnicidade(campo: String, mensagem: String, bloco: () -> T): T =
    try {
        bloco()
    } catch (e: DataIntegrityViolationException) {
        if ((e.rootCause as? SQLException)?.sqlState == UNIQUE_VIOLATION) {
            throw DuplicidadeException(campo, mensagem)
        }
        throw e
    }
