package com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ItemSolicitado
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import java.math.BigDecimal

data class GerarOrcamentoRequest(val itens: List<ItemOrcamentoRequest>) {
    fun itensSolicitados() = itens.map { it.paraItemSolicitado() }
}

data class ItemOrcamentoRequest(
    val tipo: String,
    val servicoId: Long? = null,
    val itemEstoqueId: Long? = null,
    val quantidade: Int? = null,
    val descricao: String? = null,
    val valor: BigDecimal? = null,
) {
    fun paraItemSolicitado() = ItemSolicitado(
        tipo = tipoDe(tipo),
        servicoId = servicoId,
        itemEstoqueId = itemEstoqueId,
        quantidade = quantidade,
        descricao = descricao,
        valor = valor?.let(Dinheiro::of),
    )

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
