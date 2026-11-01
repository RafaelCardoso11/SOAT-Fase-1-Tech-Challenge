package com.fiap.soat.oficina.ordemservico.domain.model

import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import java.math.BigDecimal
import java.time.Instant

class Orcamento(
    val itens: List<ItemOrcamento>,
    val validoAte: Instant,
    aprovadoEm: Instant? = null,
) {
    init {
        require(itens.isNotEmpty()) { "O orçamento deve ter ao menos um item" }
    }

    val total: Dinheiro = itens.fold(Dinheiro.of(BigDecimal.ZERO)) { acc, item -> acc + item.valor }

    var aprovadoEm: Instant? = aprovadoEm
        private set

    fun aprovar(momento: Instant) {
        check(momento <= validoAte) { "Orçamento expirado" }
        aprovadoEm = momento
    }
}
