package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.GerarOrcamentoRequest
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.OrcamentoResponse
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RevisarOrcamentoUseCase
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ordens/{id}/orcamento")
class RevisarOrcamentoController(
    private val revisarOrcamento: RevisarOrcamentoUseCase,
) {

    @PostMapping("/revisar")
    fun revisar(@PathVariable id: Long, @RequestBody request: GerarOrcamentoRequest): OrcamentoResponse =
        OrcamentoResponse.de(revisarOrcamento.revisar(id, request.paraItensSolicitados()))
}
