package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoRejeitado
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.event.ReservaLiberada
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RejeitarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import org.springframework.transaction.annotation.Transactional

@Transactional
class RejeitarOrcamentoService(
    private val repositorio: OrdemServicoRepository,
    private val eventos: PublicadorDeEventos,
    private val estoque: EstoquePort,
    private val notificacao: NotificacaoPort,
) : RejeitarOrcamentoUseCase {

    override fun rejeitar(ordemServicoId: Long, motivo: String?): OrdemServico {
        val ordem = repositorio.buscarPorId(ordemServicoId)
            ?: throw NoSuchElementException("Ordem de serviço não encontrada")

        ordem.rejeitarOrcamento(motivo)
        repositorio.salvar(ordem)

        estoque.liberarReservas(ordemServicoId)
        eventos.publicar(OrcamentoRejeitado(ordemServicoId))
        eventos.publicar(OsEncerrada(ordemServicoId))
        eventos.publicar(ReservaLiberada(ordemServicoId))
        notificacao.notificarOrcamentoRejeitado(ordemServicoId, motivo)
        return ordem
    }
}
