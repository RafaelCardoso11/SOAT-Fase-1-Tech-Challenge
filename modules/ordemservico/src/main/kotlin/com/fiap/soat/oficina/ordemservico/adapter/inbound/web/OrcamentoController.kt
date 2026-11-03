package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.GerarOrcamentoRequest
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.OrcamentoResponse
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.GerarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.MontarItensOrcamentoUseCase
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ordens/{id}/orcamento")
class OrcamentoController(
    private val gerarOrcamento: GerarOrcamentoUseCase,
    private val montarItens: MontarItensOrcamentoUseCase,
) {

    @PostMapping
    fun gerar(@PathVariable id: Long, @RequestBody request: GerarOrcamentoRequest): OrcamentoResponse =
        OrcamentoResponse.de(gerarOrcamento.gerar(id, montarItens.montar(request.itensSolicitados())))
}
