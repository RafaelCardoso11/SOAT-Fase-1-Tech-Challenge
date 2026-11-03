package com.fiap.soat.oficina.ordemservico.domain.port.outbound

import com.fiap.soat.oficina.shared.domain.vo.Dinheiro

data class ItemCatalogo(val nome: String, val preco: Dinheiro)

/** Devolve null quando o item não existe ou está inativo. */
interface CatalogoPort {
    fun buscarServico(id: Long): ItemCatalogo?
    fun buscarPeca(id: Long): ItemCatalogo?
}
