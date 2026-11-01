package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface RealizarDiagnosticoUseCase {
    fun iniciar(ordemServicoId: Long): OrdemServico
    fun registrarInspecao(ordemServicoId: Long, observacoes: String): OrdemServico
    fun finalizar(ordemServicoId: Long, pecaIdentificada: Boolean): OrdemServico
}
