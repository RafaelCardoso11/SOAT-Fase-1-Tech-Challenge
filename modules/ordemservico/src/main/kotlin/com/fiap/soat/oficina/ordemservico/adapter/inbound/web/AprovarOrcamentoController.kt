package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.OrcamentoResponse
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.AprovarOrcamentoUseCase
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ordens/{id}/orcamento")
class AprovarOrcamentoController(
    private val aprovarOrcamento: AprovarOrcamentoUseCase,
) {

    @PostMapping("/aprovar")
    fun aprovar(@PathVariable id: Long) = OrcamentoResponse.de(aprovarOrcamento.aprovar(id))
}
