package com.fiap.soat.oficina.cadastro.domain.port.inbound

import com.fiap.soat.oficina.cadastro.domain.model.Veiculo

/** Com [somenteAtivos], um veículo inativo é tratado como não encontrado (FR-020). */
interface ConsultarVeiculoUseCase {
    fun buscarPorId(id: Long, somenteAtivos: Boolean = false): Veiculo
    fun buscarPorPlaca(placa: String, somenteAtivos: Boolean = false): Veiculo
    fun listar(ativo: Boolean? = null, clienteId: Long? = null): List<Veiculo>
}
