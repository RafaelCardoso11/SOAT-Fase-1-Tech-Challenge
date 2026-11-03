package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoRevisado
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.model.ItemSolicitado
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RevisarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.CatalogoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Transactional
class RevisarOrcamentoService(
    private val repositorio: OrdemServicoRepository,
    private val eventos: PublicadorDeEventos,
    private val estoque: EstoquePort,
    catalogo: CatalogoPort,
    private val notificacao: NotificacaoPort,
    private val validadeDias: Long,
) : RevisarOrcamentoUseCase {

    private val montador = MontadorDeItensOrcamento(catalogo)

    override fun revisar(ordemServicoId: Long, itens: List<ItemSolicitado>): OrdemServico {
        val ordem = repositorio.buscarPorId(ordemServicoId)
            ?: throw NoSuchElementException("Ordem de serviço não encontrada")

        val validoAte = Instant.now().plus(Duration.ofDays(validadeDias))
        val orcamento = ordem.revisarOrcamento(montador.montar(itens), validoAte)

        val reservouTudo = orcamento.itens.filter { it.tipo == TipoItem.PECA }
            .all { estoque.reservar(ordemServicoId, it.itemEstoqueId!!, it.quantidade!!) }
        if (!reservouTudo) {
            estoque.liberarReservas(ordemServicoId)
            ordem.encerrar()
            repositorio.salvar(ordem)
            eventos.publicar(OsEncerrada(ordemServicoId))
            return ordem
        }

        repositorio.salvar(ordem)
        eventos.publicar(OrcamentoRevisado(ordemServicoId))
        notificacao.notificarOrcamentoGerado(ordemServicoId, orcamento.total)
        return ordem
    }
}
