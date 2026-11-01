package com.fiap.soat.oficina.estoque.application.usecase

import com.fiap.soat.oficina.estoque.domain.error.DuplicidadeException
import com.fiap.soat.oficina.estoque.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import com.fiap.soat.oficina.estoque.domain.model.Quantidade
import com.fiap.soat.oficina.estoque.domain.port.inbound.GerenciarItemUseCase
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Transactional
class GerenciarItemService(
    private val itens: ItemEstoqueRepository,
) : GerenciarItemUseCase {

    override fun cadastrar(
        nome: String?,
        descricao: String?,
        tipo: String?,
        preco: BigDecimal?,
        saldoInicial: Int?,
    ): ItemEstoque {
        val item = ItemEstoque.criar(nome, descricao, tipo, preco, saldoInicial)
        if (itens.existePorNome(item.nome)) throw nomeDuplicado()
        return itens.salvar(item)
    }

    override fun alterar(id: Long, nome: String?, descricao: String?, tipo: String?, preco: BigDecimal?): ItemEstoque {
        val atualizado = buscarParaAtualizar(id).alterar(nome, descricao, tipo, preco)
        if (itens.existePorNome(atualizado.nome, ignorandoId = id)) throw nomeDuplicado()
        return itens.salvar(atualizado)
    }

    override fun registrarEntrada(id: Long, quantidade: Int?): ItemEstoque {
        val entrada = Quantidade.of(quantidade)
        return itens.salvar(buscarParaAtualizar(id).registrarEntrada(entrada))
    }

    override fun inativar(id: Long) {
        val item = buscarParaAtualizar(id)
        if (item.ativo) itens.salvar(item.inativar())
    }

    // O lock vale também para alterar e inativar: salvar grava saldo e reservado, e uma reserva concorrente seria sobrescrita.
    private fun buscarParaAtualizar(id: Long): ItemEstoque =
        itens.buscarPorIdParaAtualizar(id) ?: throw RegistroNaoEncontradoException("Item de estoque não encontrado")

    private fun nomeDuplicado() = DuplicidadeException("nome", "Já existe um item de estoque com este nome")
}
