package com.fiap.soat.oficina.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoint simples para confirmar que a aplicação está no ar.
 * A documentação completa da API fica no Swagger UI (/swagger-ui.html).
 */
@RestController
class HomeController {

    @GetMapping("/api/ping")
    fun ping(): Map<String, String> = mapOf(
        "aplicacao" to "oficina",
        "status" to "ok",
    )
}
