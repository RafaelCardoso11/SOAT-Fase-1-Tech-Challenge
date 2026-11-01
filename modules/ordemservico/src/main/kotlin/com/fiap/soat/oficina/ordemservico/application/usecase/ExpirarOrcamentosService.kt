package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoExpirado
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.event.ReservaLiberada
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ExpirarOrcamentosUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
class ExpirarOrcamentosService(
    private val repositorio: OrdemServicoRepository,
    private val eventos: PublicadorDeEventos,
    private val estoque: EstoquePort,
    private val notificacao: NotificacaoPort,
) : ExpirarOrcamentosUseCase {

    override fun executar(): Int {
        val agora = Instant.now()
        val vencidas = repositorio.buscarExpiraveis(agora)

        vencidas.forEach { ordem ->
            val id = ordem.id!!
            ordem.expirarOrcamento(agora)
            repositorio.salvar(ordem)
            ordem.orcamento?.itens?.filter { it.tipo == TipoItem.PECA }?.forEach { estoque.liberarReserva(it.descricao) }
            eventos.publicar(OrcamentoExpirado(id))
            eventos.publicar(OsEncerrada(id))
            eventos.publicar(ReservaLiberada(id))
            notificacao.notificarOrcamentoExpirado(id)
        }
        return vencidas.size
    }
}
