package com.fiap.soat.oficina.cadastro.application.usecase

import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Servico
import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarServicoUseCase
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ServicoRepository

class ConsultarServicoService(
    private val servicos: ServicoRepository,
) : ConsultarServicoUseCase {

    override fun buscarPorId(id: Long, somenteAtivos: Boolean): Servico =
        servicos.buscarPorId(id)?.takeIf { !somenteAtivos || it.ativo }
            ?: throw RegistroNaoEncontradoException("Serviço não encontrado")

    override fun listar(ativo: Boolean?): List<Servico> = servicos.listar(ativo)
}
