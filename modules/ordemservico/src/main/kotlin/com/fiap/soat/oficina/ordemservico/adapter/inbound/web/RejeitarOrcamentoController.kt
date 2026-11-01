package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.OrcamentoResponse
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.RejeitarOrcamentoRequest
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RejeitarOrcamentoUseCase
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ordens/{id}/orcamento")
class RejeitarOrcamentoController(
    private val rejeitarOrcamento: RejeitarOrcamentoUseCase,
) {

    @PostMapping("/rejeitar")
    fun rejeitar(
        @PathVariable id: Long,
        @RequestBody(required = false) request: RejeitarOrcamentoRequest?,
    ) = OrcamentoResponse.de(rejeitarOrcamento.rejeitar(id, request?.motivo))
}
