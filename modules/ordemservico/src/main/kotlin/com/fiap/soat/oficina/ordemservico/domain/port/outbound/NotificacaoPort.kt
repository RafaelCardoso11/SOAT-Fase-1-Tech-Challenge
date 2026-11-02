package com.fiap.soat.oficina.ordemservico.domain.port.outbound

import com.fiap.soat.oficina.shared.domain.vo.Dinheiro

interface NotificacaoPort {
    fun notificarOrcamentoGerado(ordemServicoId: Long, total: Dinheiro)
    fun notificarOrcamentoRejeitado(ordemServicoId: Long, motivo: String?)
    fun notificarOrcamentoExpirado(ordemServicoId: Long)
    fun notificarVeiculoPronto(ordemServicoId: Long)
}
