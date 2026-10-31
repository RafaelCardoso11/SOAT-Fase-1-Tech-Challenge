package com.fiap.soat.oficina.cadastro.application.usecase

import com.fiap.soat.oficina.cadastro.domain.error.ConflitoException
import com.fiap.soat.oficina.cadastro.domain.error.DuplicidadeException
import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Veiculo
import com.fiap.soat.oficina.cadastro.domain.port.inbound.GerenciarVeiculoUseCase
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ClienteRepository
import com.fiap.soat.oficina.cadastro.domain.port.outbound.VeiculoRepository
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Year

@Transactional
class GerenciarVeiculoService(
    private val veiculos: VeiculoRepository,
    private val clientes: ClienteRepository,
    private val clock: Clock,
) : GerenciarVeiculoUseCase {

    override fun cadastrar(placa: String?, marca: String?, modelo: String?, ano: Int?, clienteId: Long?): Veiculo {
        val veiculo = Veiculo.criar(placa, marca, modelo, ano, clienteId, anoLimite())
        exigirClienteAtivo(veiculo.clienteId)
        if (veiculos.existePorPlaca(veiculo.placa)) {
            throw DuplicidadeException("placa", "Já existe um veículo com esta placa")
        }
        return veiculos.salvar(veiculo)
    }

    override fun alterar(id: Long, marca: String?, modelo: String?, ano: Int?, clienteId: Long?): Veiculo {
        val atualizado = buscar(id).alterar(marca, modelo, ano, clienteId, anoLimite())
        exigirClienteAtivo(atualizado.clienteId)
        return veiculos.salvar(atualizado)
    }

    override fun inativar(id: Long) {
        val veiculo = buscar(id)
        if (veiculo.ativo) veiculos.salvar(veiculo.inativar())
    }

    private fun buscar(id: Long): Veiculo =
        veiculos.buscarPorId(id) ?: throw RegistroNaoEncontradoException("Veículo não encontrado")

    private fun exigirClienteAtivo(clienteId: Long) {
        val cliente = clientes.buscarPorId(clienteId)
            ?: throw RegistroNaoEncontradoException("Cliente não encontrado")
        if (!cliente.ativo) throw ConflitoException("Cliente inativo não pode ter veículos")
    }

    private fun anoLimite(): Int = Year.now(clock).value + 1
}
