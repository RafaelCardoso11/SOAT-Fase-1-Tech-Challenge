package com.fiap.soat.oficina.ordemservico.domain.model

import com.fiap.soat.oficina.shared.domain.vo.Dinheiro

data class ItemOrcamento(
    val tipo: TipoItem,
    val descricao: String,
    val valor: Dinheiro,
    val itemEstoqueId: Long? = null,
    val quantidade: Int? = null,
) {
    init {
        if (tipo == TipoItem.PECA) {
            requireNotNull(itemEstoqueId) { "Peça do orçamento deve referenciar um item do estoque" }
            require((quantidade ?: 0) > 0) { "Peça do orçamento deve ter quantidade positiva" }
        }
    }
}
