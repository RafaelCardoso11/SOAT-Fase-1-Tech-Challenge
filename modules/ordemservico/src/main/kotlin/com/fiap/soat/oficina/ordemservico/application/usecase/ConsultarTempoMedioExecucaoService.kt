package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ConsultarTempoMedioExecucaoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import java.time.Duration

class ConsultarTempoMedioExecucaoService(
    private val repositorio: OrdemServicoRepository,
) : ConsultarTempoMedioExecucaoUseCase {

    override fun consultar(): Duration? {
        val executadas = repositorio.buscarComExecucaoConcluida()
            .filter { it.execucaoIniciadaEm != null && it.execucaoFinalizadaEm != null }
        if (executadas.isEmpty()) return null

        val totalSegundos = executadas.sumOf {
            Duration.between(it.execucaoIniciadaEm, it.execucaoFinalizadaEm).seconds
        }
        return Duration.ofSeconds(totalSegundos / executadas.size)
    }
}
