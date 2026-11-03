package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.ItemSolicitado
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.CatalogoPort
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import java.math.BigDecimal

/** `descricao` e `valor` informados prevalecem sobre o catálogo; `valor` é sempre o da linha. */
class MontadorDeItensOrcamento(private val catalogo: CatalogoPort) {

    fun montar(solicitados: List<ItemSolicitado>): List<ItemOrcamento> = solicitados.map {
        when (it.tipo) {
            TipoItem.SERVICO -> servico(it)
            TipoItem.PECA -> peca(it)
        }
    }

    private fun servico(item: ItemSolicitado): ItemOrcamento {
        val doCatalogo = item.servicoId?.let {
            catalogo.buscarServico(it) ?: throw NoSuchElementException("Serviço $it não encontrado ou inativo")
        }
        val descricao = item.descricao.preenchida() ?: doCatalogo?.nome
            ?: throw IllegalArgumentException("Serviço sem servicoId exige descrição")
        val valor = item.valor ?: doCatalogo?.preco?.valor
            ?: throw IllegalArgumentException("Serviço sem servicoId exige valor")
        return ItemOrcamento(TipoItem.SERVICO, descricao, Dinheiro.of(valor), itemEstoqueId = item.itemEstoqueId, quantidade = item.quantidade)
    }

    private fun peca(item: ItemSolicitado): ItemOrcamento {
        require(item.servicoId == null) { "servicoId não se aplica a peça" }
        val doEstoque = item.itemEstoqueId?.let {
            catalogo.buscarPeca(it) ?: throw NoSuchElementException("Item de estoque $it não encontrado ou inativo")
        }
        val descricao = item.descricao.preenchida() ?: doEstoque?.nome
            ?: throw IllegalArgumentException("Peça sem itemEstoqueId exige descrição")
        val valor = item.valor ?: doEstoque?.let { precoDaLinha(it.preco, item.quantidade) }
            ?: throw IllegalArgumentException("Peça sem itemEstoqueId exige valor")
        return ItemOrcamento(TipoItem.PECA, descricao, Dinheiro.of(valor), item.itemEstoqueId, item.quantidade)
    }

    private fun precoDaLinha(preco: Dinheiro, quantidade: Int?): BigDecimal {
        require((quantidade ?: 0) > 0) { "Peça do orçamento deve ter quantidade positiva" }
        return (preco * quantidade!!).valor
    }

    private fun String?.preenchida(): String? = this?.trim()?.ifEmpty { null }
}
