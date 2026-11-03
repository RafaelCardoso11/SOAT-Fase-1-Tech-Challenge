package com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import java.math.BigDecimal
import java.time.Instant

data class OrcamentoClienteResponse(
    val numero: Long,
    val status: String,
    val itens: List<ItemOrcamentoClienteResponse>,
    val total: BigDecimal,
    val validoAte: Instant,
) {
    companion object {
        fun de(os: OrdemServico): OrcamentoClienteResponse {
            val orcamento = os.orcamento!!
            return OrcamentoClienteResponse(
                numero = os.id!!,
                status = os.status.name,
                itens = orcamento.itens.map {
                    ItemOrcamentoClienteResponse(it.tipo.name, it.descricao, it.quantidade, it.valor.valor)
                },
                total = orcamento.total.valor,
                validoAte = orcamento.validoAte,
            )
        }
    }
}

data class ItemOrcamentoClienteResponse(
    val tipo: String,
    val descricao: String,
    val quantidade: Int?,
    val valor: BigDecimal,
)
