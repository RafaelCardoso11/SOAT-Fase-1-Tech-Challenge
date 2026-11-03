package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro

data class ItemSolicitado(
    val tipo: TipoItem,
    val servicoId: Long? = null,
    val itemEstoqueId: Long? = null,
    val quantidade: Int? = null,
    val descricao: String? = null,
    val valor: Dinheiro? = null,
)

interface MontarItensOrcamentoUseCase {
    fun montar(itens: List<ItemSolicitado>): List<ItemOrcamento>
}
