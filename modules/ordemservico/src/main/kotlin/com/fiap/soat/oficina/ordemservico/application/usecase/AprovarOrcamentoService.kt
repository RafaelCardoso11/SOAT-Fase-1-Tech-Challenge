package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoAprovado
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.AprovarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import org.springframework.transaction.annotation.Transactional

@Transactional
class AprovarOrcamentoService(
    private val repositorio: OrdemServicoRepository,
    private val eventos: PublicadorDeEventos,
) : AprovarOrcamentoUseCase {

    override fun aprovar(ordemServicoId: Long): OrdemServico {
        val ordem = repositorio.buscarPorId(ordemServicoId)
            ?: throw NoSuchElementException("Ordem de serviço não encontrada")

        ordem.aprovarOrcamento()
        repositorio.salvar(ordem)
        eventos.publicar(OrcamentoAprovado(ordemServicoId))
        return ordem
    }
}
