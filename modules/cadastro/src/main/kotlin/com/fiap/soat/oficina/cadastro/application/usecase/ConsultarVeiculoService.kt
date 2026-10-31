package com.fiap.soat.oficina.cadastro.application.usecase

import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Veiculo
import com.fiap.soat.oficina.cadastro.domain.model.validando
import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarVeiculoUseCase
import com.fiap.soat.oficina.cadastro.domain.port.outbound.VeiculoRepository
import com.fiap.soat.oficina.shared.domain.vo.Placa

class ConsultarVeiculoService(
    private val veiculos: VeiculoRepository,
) : ConsultarVeiculoUseCase {

    override fun buscarPorId(id: Long, somenteAtivos: Boolean): Veiculo =
        encontrado(veiculos.buscarPorId(id), somenteAtivos)

    override fun buscarPorPlaca(placa: String, somenteAtivos: Boolean): Veiculo {
        val valor = validando("placa") { Placa.of(placa) }
        return encontrado(veiculos.buscarPorPlaca(valor), somenteAtivos)
    }

    override fun listar(ativo: Boolean?, clienteId: Long?): List<Veiculo> = veiculos.listar(ativo, clienteId)

    private fun encontrado(veiculo: Veiculo?, somenteAtivos: Boolean): Veiculo =
        veiculo?.takeIf { !somenteAtivos || it.ativo }
            ?: throw RegistroNaoEncontradoException("Veículo não encontrado")
}
