package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

interface OrdemServicoResumoView {
    val id: Long
    val status: String
    val documento: String
    val placa: String
    val marca: String
    val modelo: String
    val ano: Int
    val relato: String
}
