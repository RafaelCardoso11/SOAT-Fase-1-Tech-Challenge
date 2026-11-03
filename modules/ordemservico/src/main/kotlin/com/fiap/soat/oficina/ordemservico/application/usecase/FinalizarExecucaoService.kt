package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.event.ExecucaoFinalizada
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.FinalizarExecucaoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import org.springframework.transaction.annotation.Transactional

@Transactional
class FinalizarExecucaoService(
    private val repositorio: OrdemServicoRepository,
    private val eventos: PublicadorDeEventos,
    private val estoque: EstoquePort,
    private val notificacao: NotificacaoPort,
) : FinalizarExecucaoUseCase {

    override fun finalizar(ordemServicoId: Long, todosAplicados: Boolean): OrdemServico {
        val ordem = repositorio.buscarPorId(ordemServicoId)
            ?: throw NoSuchElementException("Ordem de serviço não encontrada")

        ordem.finalizarExecucao(todosAplicados)
        repositorio.salvar(ordem)
        estoque.baixarPecas(ordemServicoId)
        eventos.publicar(ExecucaoFinalizada(ordemServicoId))
        notificacao.notificarVeiculoPronto(ordemServicoId, ordem.documento)
        return ordem
    }
}
