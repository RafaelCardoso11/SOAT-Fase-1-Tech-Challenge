package com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import java.math.BigDecimal

data class GerarOrcamentoRequest(val itens: List<ItemOrcamentoRequest>)

data class ItemOrcamentoRequest(val tipo: String, val descricao: String, val valor: BigDecimal)

data class OrcamentoResponse(val status: String, val total: BigDecimal) {
    companion object {
        fun de(os: OrdemServico) = OrcamentoResponse(os.status.name, os.orcamento!!.total.valor)
    }
}
