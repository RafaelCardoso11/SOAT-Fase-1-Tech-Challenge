package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.event.DiagnosticoFinalizado
import com.fiap.soat.oficina.ordemservico.domain.event.DiagnosticoIniciado
import com.fiap.soat.oficina.ordemservico.domain.event.DiagnosticoRealizado
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RealizarDiagnosticoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.event.DomainEvent
import org.springframework.transaction.annotation.Transactional

@Transactional
class RealizarDiagnosticoService(
    private val repositorio: OrdemServicoRepository,
    private val eventos: PublicadorDeEventos,
) : RealizarDiagnosticoUseCase {

    override fun iniciar(ordemServicoId: Long) = aplicar(ordemServicoId) {
        it.iniciarDiagnostico()
        DiagnosticoIniciado(ordemServicoId)
    }

    override fun registrarInspecao(ordemServicoId: Long, observacoes: String) = aplicar(ordemServicoId) {
        it.registrarInspecao(observacoes)
        DiagnosticoRealizado(ordemServicoId)
    }

    override fun finalizar(ordemServicoId: Long, pecaIdentificada: Boolean) = aplicar(ordemServicoId) {
        it.finalizarDiagnostico(pecaIdentificada)
        if (pecaIdentificada) DiagnosticoFinalizado(ordemServicoId) else OsEncerrada(ordemServicoId)
    }

    private fun aplicar(ordemServicoId: Long, acao: (OrdemServico) -> DomainEvent): OrdemServico {
        val ordem = repositorio.buscarPorId(ordemServicoId)
            ?: throw NoSuchElementException("Ordem de serviço não encontrada")
        val evento = acao(ordem)
        repositorio.salvar(ordem)
        eventos.publicar(evento)
        return ordem
    }
}
