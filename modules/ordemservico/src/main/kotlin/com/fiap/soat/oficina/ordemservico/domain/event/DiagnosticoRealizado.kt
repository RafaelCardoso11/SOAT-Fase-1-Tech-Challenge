package com.fiap.soat.oficina.ordemservico.domain.event

import com.fiap.soat.oficina.shared.domain.event.DomainEvent

data class DiagnosticoRealizado(val ordemServicoId: Long) : DomainEvent
