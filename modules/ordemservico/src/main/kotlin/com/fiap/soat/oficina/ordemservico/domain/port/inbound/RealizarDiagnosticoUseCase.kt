package com.fiap.soat.oficina.ordemservico.domain.port.inbound

interface RealizarDiagnosticoUseCase {
    fun iniciar(ordemServicoId: Long)
    fun registrarInspecao(ordemServicoId: Long, observacoes: String)
    fun finalizar(ordemServicoId: Long, pecaIdentificada: Boolean)
}
