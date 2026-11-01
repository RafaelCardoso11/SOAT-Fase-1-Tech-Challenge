package com.fiap.soat.oficina.estoque.domain.port.inbound

import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import java.math.BigDecimal

interface GerenciarItemUseCase {
    fun cadastrar(
        nome: String?,
        descricao: String?,
        tipo: String?,
        preco: BigDecimal?,
        saldoInicial: Int?,
    ): ItemEstoque

    fun alterar(id: Long, nome: String?, descricao: String?, tipo: String?, preco: BigDecimal?): ItemEstoque
    fun registrarEntrada(id: Long, quantidade: Int?): ItemEstoque
    fun inativar(id: Long)
}
