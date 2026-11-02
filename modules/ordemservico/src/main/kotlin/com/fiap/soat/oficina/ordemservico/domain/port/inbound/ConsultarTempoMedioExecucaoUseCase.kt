package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import java.time.Duration

interface ConsultarTempoMedioExecucaoUseCase {
    /** Tempo médio entre início e fim da execução; null quando não há OS com execução completa. */
    fun consultar(): Duration?
}
