package com.fiap.soat.oficina.cadastro.application.usecase

import com.fiap.soat.oficina.cadastro.domain.error.ConflitoException
import com.fiap.soat.oficina.cadastro.domain.error.DuplicidadeException
import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Cliente
import com.fiap.soat.oficina.cadastro.domain.port.inbound.GerenciarClienteUseCase
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ClienteRepository
import com.fiap.soat.oficina.cadastro.domain.port.outbound.VeiculoRepository
import org.springframework.transaction.annotation.Transactional

@Transactional
class GerenciarClienteService(
    private val clientes: ClienteRepository,
    private val veiculos: VeiculoRepository,
) : GerenciarClienteUseCase {

    override fun cadastrar(documento: String?, nome: String?, email: String?): Cliente {
        val cliente = Cliente.criar(documento, nome, email)
        if (clientes.existePorDocumento(cliente.documento)) {
            throw DuplicidadeException("documento", "Já existe um cliente com este CPF/CNPJ")
        }
        return clientes.salvar(cliente)
    }

    override fun alterar(id: Long, nome: String?, email: String?): Cliente =
        clientes.salvar(buscar(id).alterar(nome, email))

    override fun inativar(id: Long) {
        val cliente = buscar(id)
        if (!cliente.ativo) return
        if (veiculos.existeAtivoDoCliente(id)) {
            throw ConflitoException("Cliente possui veículos ativos e não pode ser inativado")
        }
        clientes.salvar(cliente.inativar())
    }

    private fun buscar(id: Long): Cliente =
        clientes.buscarPorId(id) ?: throw RegistroNaoEncontradoException("Cliente não encontrado")
}
