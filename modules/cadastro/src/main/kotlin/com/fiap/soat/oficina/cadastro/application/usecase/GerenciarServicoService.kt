package com.fiap.soat.oficina.cadastro.application.usecase

import com.fiap.soat.oficina.cadastro.domain.error.DuplicidadeException
import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Servico
import com.fiap.soat.oficina.cadastro.domain.port.inbound.GerenciarServicoUseCase
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ServicoRepository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Transactional
class GerenciarServicoService(
    private val servicos: ServicoRepository,
) : GerenciarServicoUseCase {

    override fun cadastrar(nome: String?, descricao: String?, preco: BigDecimal?): Servico {
        val servico = Servico.criar(nome, descricao, preco)
        if (servicos.existePorNome(servico.nome)) throw nomeDuplicado()
        return servicos.salvar(servico)
    }

    override fun alterar(id: Long, nome: String?, descricao: String?, preco: BigDecimal?): Servico {
        val atualizado = buscar(id).alterar(nome, descricao, preco)
        if (servicos.existePorNome(atualizado.nome, ignorandoId = id)) throw nomeDuplicado()
        return servicos.salvar(atualizado)
    }

    override fun inativar(id: Long) {
        val servico = buscar(id)
        if (servico.ativo) servicos.salvar(servico.inativar())
    }

    private fun buscar(id: Long): Servico =
        servicos.buscarPorId(id) ?: throw RegistroNaoEncontradoException("Serviço não encontrado")

    private fun nomeDuplicado() = DuplicidadeException("nome", "Já existe um serviço com este nome")
}
