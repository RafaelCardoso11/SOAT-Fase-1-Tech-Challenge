package com.fiap.soat.oficina.cadastro.domain.port.outbound

import com.fiap.soat.oficina.cadastro.domain.model.Veiculo
import com.fiap.soat.oficina.shared.domain.vo.Placa

interface VeiculoRepository {
    fun salvar(veiculo: Veiculo): Veiculo
    fun buscarPorId(id: Long): Veiculo?
    fun buscarPorPlaca(placa: Placa): Veiculo?
    fun existePorPlaca(placa: Placa): Boolean
    fun existeAtivoDoCliente(clienteId: Long): Boolean
    fun listar(ativo: Boolean?, clienteId: Long?): List<Veiculo>
}
