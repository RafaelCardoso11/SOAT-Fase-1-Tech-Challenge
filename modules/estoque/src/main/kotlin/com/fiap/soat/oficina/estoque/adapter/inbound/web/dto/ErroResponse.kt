package com.fiap.soat.oficina.estoque.adapter.inbound.web.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Erro de negócio devolvido pela API")
data class ErroResponse(
    @field:Schema(description = "Mensagem do erro", example = "Quantidade deve ser maior que zero")
    val erro: String,

    @field:Schema(description = "Campo relacionado ao erro, quando houver", example = "quantidade", nullable = true)
    val campo: String? = null,
)
