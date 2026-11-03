package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.OrcamentoClienteResponse
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.OrcamentoResponse
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.RejeitarOrcamentoRequest
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.DecidirOrcamentoPeloClienteUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/public/orcamentos/{token}")
class OrcamentoClienteController(
    private val decidir: DecidirOrcamentoPeloClienteUseCase,
) {

    @GetMapping
    fun consultar(@PathVariable token: String) = OrcamentoClienteResponse.de(decidir.consultar(token))

    @PostMapping("/aprovar")
    fun aprovar(@PathVariable token: String) = OrcamentoResponse.de(decidir.aprovar(token))

    @PostMapping("/rejeitar")
    fun rejeitar(
        @PathVariable token: String,
        @RequestBody(required = false) request: RejeitarOrcamentoRequest?,
    ) = OrcamentoResponse.de(decidir.rejeitar(token, request?.motivo))
}
