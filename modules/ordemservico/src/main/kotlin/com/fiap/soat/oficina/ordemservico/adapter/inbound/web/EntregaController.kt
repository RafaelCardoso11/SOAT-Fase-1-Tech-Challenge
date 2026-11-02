package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.ComprovanteResponse
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.EntregarVeiculoUseCase
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ordens/{id}/entrega")
class EntregaController(
    private val entregarVeiculo: EntregarVeiculoUseCase,
) {

    @PostMapping
    fun entregar(@PathVariable id: Long) = ComprovanteResponse.de(entregarVeiculo.entregar(id))
}
