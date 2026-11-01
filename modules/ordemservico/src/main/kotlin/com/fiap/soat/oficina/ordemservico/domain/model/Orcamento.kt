package com.fiap.soat.oficina.ordemservico.domain.model

import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import java.math.BigDecimal

class Orcamento(val itens: List<ItemOrcamento>) {

    init {
        require(itens.isNotEmpty()) { "O orçamento deve ter ao menos um item" }
    }

    val total: Dinheiro = itens.fold(Dinheiro.of(BigDecimal.ZERO)) { acc, item -> acc + item.valor }
}
