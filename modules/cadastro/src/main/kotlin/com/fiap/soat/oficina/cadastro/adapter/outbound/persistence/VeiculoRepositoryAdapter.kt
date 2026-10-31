package com.fiap.soat.oficina.cadastro.adapter.outbound.persistence

import com.fiap.soat.oficina.cadastro.domain.model.Veiculo
import com.fiap.soat.oficina.cadastro.domain.port.outbound.VeiculoRepository
import com.fiap.soat.oficina.shared.domain.vo.Placa
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class VeiculoRepositoryAdapter(
    private val springData: VeiculoSpringDataRepository,
) : VeiculoRepository {

    override fun salvar(veiculo: Veiculo): Veiculo =
        traduzindoUnicidade("placa", "Já existe um veículo com esta placa") {
            springData.saveAndFlush(veiculo.paraEntidade()).paraModelo()
        }

    override fun buscarPorId(id: Long): Veiculo? = springData.findByIdOrNull(id)?.paraModelo()

    override fun buscarPorPlaca(placa: Placa): Veiculo? = springData.findByPlaca(placa.valor)?.paraModelo()

    override fun existePorPlaca(placa: Placa): Boolean = springData.existsByPlaca(placa.valor)

    override fun existeAtivoDoCliente(clienteId: Long): Boolean =
        springData.existsByClienteIdAndAtivoTrue(clienteId)

    override fun listar(ativo: Boolean?, clienteId: Long?): List<Veiculo> =
        when {
            ativo == null && clienteId == null -> springData.findAllByOrderById()
            clienteId == null -> springData.findByAtivoOrderById(requireNotNull(ativo))
            ativo == null -> springData.findByClienteIdOrderById(clienteId)
            else -> springData.findByClienteIdAndAtivoOrderById(clienteId, ativo)
        }.map { it.paraModelo() }

    private fun Veiculo.paraEntidade() = VeiculoJpaEntity(id, placa.valor, marca, modelo, ano, clienteId, ativo)

    private fun VeiculoJpaEntity.paraModelo() =
        Veiculo.reconstituir(requireNotNull(id), placa, marca, modelo, ano, clienteId, ativo)
}
