package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ItemSolicitado
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.MontarItensOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.CatalogoPort
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro

class MontarItensOrcamentoService(
    private val catalogo: CatalogoPort,
) : MontarItensOrcamentoUseCase {

    override fun montar(itens: List<ItemSolicitado>): List<ItemOrcamento> = itens.map {
        when (it.tipo) {
            TipoItem.SERVICO -> servico(it)
            TipoItem.PECA -> peca(it)
        }
    }

    private fun servico(item: ItemSolicitado): ItemOrcamento {
        val cadastrado = item.servicoId?.let(catalogo::servico)
        return montar(item, cadastrado?.nome, cadastrado?.preco)
    }

    private fun peca(item: ItemSolicitado): ItemOrcamento {
        val cadastrado = item.itemEstoqueId?.let(catalogo::itemEstoque)
        val quantidade = (item.quantidade ?: 0).coerceAtLeast(0)
        val valorDaLinha = cadastrado?.let { Dinheiro.of(it.preco.valor * quantidade.toBigDecimal()) }
        return montar(item, cadastrado?.nome, valorDaLinha)
    }

    private fun montar(item: ItemSolicitado, nomeCadastrado: String?, valorCadastrado: Dinheiro?): ItemOrcamento {
        val descricao = item.descricao?.takeIf { it.isNotBlank() } ?: nomeCadastrado
        return ItemOrcamento(
            tipo = item.tipo,
            descricao = requireNotNull(descricao) { "Item sem cadastro deve informar a descrição" },
            valor = requireNotNull(item.valor ?: valorCadastrado) { "Item sem cadastro deve informar o valor" },
            itemEstoqueId = item.itemEstoqueId,
            quantidade = item.quantidade,
        )
    }
}
