package com.fiap.soat.oficina.cadastro.domain.port.inbound

import com.fiap.soat.oficina.cadastro.domain.model.Veiculo

interface GerenciarVeiculoUseCase {
    fun cadastrar(placa: String?, marca: String?, modelo: String?, ano: Int?, clienteId: Long?): Veiculo
    fun alterar(id: Long, marca: String?, modelo: String?, ano: Int?, clienteId: Long?): Veiculo
    fun inativar(id: Long)
}
