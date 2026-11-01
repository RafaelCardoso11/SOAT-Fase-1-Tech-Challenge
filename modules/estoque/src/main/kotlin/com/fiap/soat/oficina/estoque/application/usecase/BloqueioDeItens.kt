package com.fiap.soat.oficina.estoque.application.usecase

import com.fiap.soat.oficina.estoque.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import com.fiap.soat.oficina.estoque.domain.model.Reserva
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository

internal fun ItemEstoqueRepository.obterParaAtualizar(id: Long): ItemEstoque =
    buscarPorIdParaAtualizar(id) ?: throw RegistroNaoEncontradoException("Item de estoque não encontrado")

// Ordem crescente de itemId em toda operação com vários itens: evita deadlock entre OS que disputam os mesmos itens.
internal fun List<Reserva>.emOrdemDeBloqueio(): List<Reserva> = sortedBy { it.itemId }
