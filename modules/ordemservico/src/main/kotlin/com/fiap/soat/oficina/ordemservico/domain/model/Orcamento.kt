package com.fiap.soat.oficina.ordemservico.domain.model

import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class Orcamento(
    val itens: List<ItemOrcamento>,
    val validoAte: Instant,
    aprovadoEm: Instant? = null,
    motivoRejeicao: String? = null,
    val tokenAprovacao: String = UUID.randomUUID().toString(),
) {
    init {
        require(itens.isNotEmpty()) { "O orçamento deve ter ao menos um item" }
    }

    val total: Dinheiro = itens.fold(Dinheiro.of(BigDecimal.ZERO)) { acc, item -> acc + item.valor }

    var aprovadoEm: Instant? = aprovadoEm
        private set

    var motivoRejeicao: String? = motivoRejeicao
        private set

    fun aprovar(momento: Instant) {
        check(momento <= validoAte) { "Orçamento expirado" }
        aprovadoEm = momento
    }

    fun rejeitar(motivo: String?) {
        motivoRejeicao = motivo
    }
}
