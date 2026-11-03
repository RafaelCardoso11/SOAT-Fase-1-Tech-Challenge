package com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto

import com.fiap.soat.oficina.ordemservico.domain.model.ItemSolicitado
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import java.math.BigDecimal

data class GerarOrcamentoRequest(val itens: List<ItemOrcamentoRequest>) {
    fun paraItensSolicitados(): List<ItemSolicitado> = itens.map { it.paraItemSolicitado() }
}

data class ItemOrcamentoRequest(
    val tipo: String,
    val descricao: String? = null,
    val valor: BigDecimal? = null,
    val servicoId: Long? = null,
    val itemEstoqueId: Long? = null,
    val quantidade: Int? = null,
) {
    fun paraItemSolicitado() = ItemSolicitado(tipoDe(tipo), descricao, valor, servicoId, itemEstoqueId, quantidade)

    private fun tipoDe(valor: String): TipoItem = when (valor.trim().lowercase()) {
        "servico", "serviço" -> TipoItem.SERVICO
        "peca", "peça" -> TipoItem.PECA
        else -> throw IllegalArgumentException("Tipo de item inválido: $valor")
    }
}

data class OrcamentoResponse(val status: String, val total: BigDecimal) {
    companion object {
        fun de(os: OrdemServico) = OrcamentoResponse(os.status.name, os.orcamento!!.total.valor)
    }
}
