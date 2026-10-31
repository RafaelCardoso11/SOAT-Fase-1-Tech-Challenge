package com.fiap.soat.oficina.cadastro.domain.port.inbound

import com.fiap.soat.oficina.cadastro.domain.model.Servico
import java.math.BigDecimal

interface GerenciarServicoUseCase {
    fun cadastrar(nome: String?, descricao: String?, preco: BigDecimal?): Servico
    fun alterar(id: Long, nome: String?, descricao: String?, preco: BigDecimal?): Servico
    fun inativar(id: Long)
}
