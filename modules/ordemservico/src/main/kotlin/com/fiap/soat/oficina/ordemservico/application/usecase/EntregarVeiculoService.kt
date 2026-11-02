package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.event.VeiculoEntregue
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.EntregarVeiculoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import org.springframework.transaction.annotation.Transactional

@Transactional
class EntregarVeiculoService(
    private val repositorio: OrdemServicoRepository,
    private val eventos: PublicadorDeEventos,
) : EntregarVeiculoUseCase {

    override fun entregar(ordemServicoId: Long): OrdemServico {
        val ordem = repositorio.buscarPorId(ordemServicoId)
            ?: throw NoSuchElementException("Ordem de serviço não encontrada")

        ordem.entregar()
        repositorio.salvar(ordem)
        eventos.publicar(VeiculoEntregue(ordemServicoId))
        eventos.publicar(OsEncerrada(ordemServicoId))
        return ordem
    }
}
