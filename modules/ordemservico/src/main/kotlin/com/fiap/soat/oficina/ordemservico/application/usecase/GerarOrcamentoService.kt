package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoGerado
import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.GerarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Transactional
class GerarOrcamentoService(
    private val repositorio: OrdemServicoRepository,
    private val eventos: PublicadorDeEventos,
    private val estoque: EstoquePort,
    private val notificacao: NotificacaoPort,
    private val validadeDias: Long,
) : GerarOrcamentoUseCase {

    override fun gerar(ordemServicoId: Long, itens: List<ItemOrcamento>): OrdemServico {
        val ordem = repositorio.buscarPorId(ordemServicoId)
            ?: throw NoSuchElementException("Ordem de serviço não encontrada")

        val validoAte = Instant.now().plus(Duration.ofDays(validadeDias))
        val orcamento = ordem.gerarOrcamento(itens, validoAte)
        repositorio.salvar(ordem)

        orcamento.itens.filter { it.tipo == TipoItem.PECA }.forEach { estoque.reservar(it.descricao) }
        eventos.publicar(OrcamentoGerado(ordemServicoId))
        notificacao.notificarOrcamentoGerado(ordemServicoId, orcamento.total)
        return ordem
    }
}
