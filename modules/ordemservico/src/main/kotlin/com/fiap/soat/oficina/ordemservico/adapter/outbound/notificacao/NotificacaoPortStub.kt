package com.fiap.soat.oficina.ordemservico.adapter.outbound.notificacao

import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import org.springframework.stereotype.Component

@Component
class NotificacaoPortStub : NotificacaoPort {
    // shortcut: notificação real (e-mail/SMTP) é um serviço externo; no-op até a integração.
    override fun notificarOrcamentoGerado(ordemServicoId: Long, total: Dinheiro) = Unit
}
