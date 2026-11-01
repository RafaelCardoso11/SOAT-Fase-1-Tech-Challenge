package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ExpirarOrcamentosUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/orcamentos")
class ExpiracaoController(
    private val expirarOrcamentos: ExpirarOrcamentosUseCase,
) {

    @PostMapping("/expirar")
    fun expirar() = mapOf("expirados" to expirarOrcamentos.executar())
}
