package com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Erro de negócio devolvido pela API")
data class ErroResponse(
    @field:Schema(description = "Mensagem do erro", example = "CPF/CNPJ inválido")
    val erro: String,

    @field:Schema(description = "Campo relacionado ao erro, quando houver", example = "documento", nullable = true)
    val campo: String? = null,
)
