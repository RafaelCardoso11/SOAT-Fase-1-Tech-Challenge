package com.fiap.soat.oficina.ordemservico.adapter.inbound.scheduler

import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ExpirarOrcamentosUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ExpiracaoScheduler(
    private val expirarOrcamentos: ExpirarOrcamentosUseCase,
) {

    @Scheduled(cron = "\${orcamento.expiracao-cron:0 0 * * * *}")
    fun verificar() {
        expirarOrcamentos.executar()
    }
}
