package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.event.ExecucaoIniciada
import com.fiap.soat.oficina.ordemservico.domain.event.PecasBaixadas
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.IniciarExecucaoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import org.springframework.transaction.annotation.Transactional

@Transactional
class IniciarExecucaoService(
    private val repositorio: OrdemServicoRepository,
    private val eventos: PublicadorDeEventos,
    private val estoque: EstoquePort,
) : IniciarExecucaoUseCase {

    override fun iniciar(ordemServicoId: Long): OrdemServico {
        val ordem = repositorio.buscarPorId(ordemServicoId)
            ?: throw NoSuchElementException("Ordem de serviço não encontrada")

        ordem.iniciarExecucao()
        repositorio.salvar(ordem)
        estoque.baixarPecas(ordemServicoId)
        eventos.publicar(ExecucaoIniciada(ordemServicoId))
        eventos.publicar(PecasBaixadas(ordemServicoId))
        return ordem
    }
}
