package com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto

data class CriarOrdemServicoRequest(
    val documento: String,
    val placa: String,
    val marca: String,
    val modelo: String,
    val ano: Int,
    val relato: String,
)
