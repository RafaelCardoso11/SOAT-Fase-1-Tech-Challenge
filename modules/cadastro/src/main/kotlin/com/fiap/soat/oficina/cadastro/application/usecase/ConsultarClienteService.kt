package com.fiap.soat.oficina.cadastro.application.usecase

import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Cliente
import com.fiap.soat.oficina.cadastro.domain.model.validando
import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarClienteUseCase
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ClienteRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento

class ConsultarClienteService(
    private val clientes: ClienteRepository,
) : ConsultarClienteUseCase {

    override fun buscarPorId(id: Long, somenteAtivos: Boolean): Cliente =
        encontrado(clientes.buscarPorId(id), somenteAtivos)

    override fun buscarPorDocumento(documento: String, somenteAtivos: Boolean): Cliente {
        val valor = validando("documento") { Documento.of(documento) }
        return encontrado(clientes.buscarPorDocumento(valor), somenteAtivos)
    }

    override fun listar(ativo: Boolean?): List<Cliente> = clientes.listar(ativo)

    private fun encontrado(cliente: Cliente?, somenteAtivos: Boolean): Cliente =
        cliente?.takeIf { !somenteAtivos || it.ativo }
            ?: throw RegistroNaoEncontradoException("Cliente não encontrado")
}
