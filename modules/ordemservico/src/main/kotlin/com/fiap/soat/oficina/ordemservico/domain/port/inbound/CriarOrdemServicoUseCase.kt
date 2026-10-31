package com.fiap.soat.oficina.ordemservico.domain.port.inbound

interface CriarOrdemServicoUseCase {
    fun criar(comando: CriarOrdemServico): Long
}

class CriarOrdemServico(
    val documento: String,
    val placa: String,
    val marca: String,
    val modelo: String,
    val ano: Int,
    val relato: String,
)
