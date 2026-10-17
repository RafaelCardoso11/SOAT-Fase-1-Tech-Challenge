package com.fiap.soat.oficina

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Ponto de entrada do monolito modular. Faz o component scan de `com.fiap.soat.oficina`,
 * agregando os bounded contexts (autenticacao, cadastro, estoque, ordemservico) num único deployable.
 */
@SpringBootApplication
class OficinaApplication

fun main(args: Array<String>) {
    runApplication<OficinaApplication>(*args)
}
