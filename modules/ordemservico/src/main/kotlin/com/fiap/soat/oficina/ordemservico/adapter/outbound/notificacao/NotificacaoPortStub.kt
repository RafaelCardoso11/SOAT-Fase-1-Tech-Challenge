package com.fiap.soat.oficina.ordemservico.adapter.outbound.notificacao

import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "notificacao.email", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class NotificacaoPortStub : NotificacaoPort {
    override fun notificarOrcamentoGerado(ordemServicoId: Long, total: Dinheiro, tokenAprovacao: String) = Unit

    override fun notificarOrcamentoRejeitado(ordemServicoId: Long, motivo: String?) = Unit

    override fun notificarOrcamentoExpirado(ordemServicoId: Long) = Unit

    override fun notificarVeiculoPronto(ordemServicoId: Long) = Unit
}
