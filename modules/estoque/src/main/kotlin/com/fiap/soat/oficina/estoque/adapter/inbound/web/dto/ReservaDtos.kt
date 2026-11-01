package com.fiap.soat.oficina.estoque.adapter.inbound.web.dto

import com.fiap.soat.oficina.estoque.domain.model.Reserva
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Pedido de reserva de um item de estoque para uma OS")
data class ReservaRequest(
    @field:Schema(description = "Identificador do item de estoque (deve estar ativo)", example = "1")
    val itemId: Long? = null,

    @field:Schema(description = "Quantidade inteira maior que zero", example = "1")
    val quantidade: Int? = null,
)

@Schema(description = "Reserva de um item para uma OS")
data class ReservaResponse(
    val id: Long,
    @field:Schema(description = "Identificador da OS dona da reserva", example = "7")
    val osId: Long,
    val itemId: Long,
    val quantidade: Int,
    @field:Schema(description = "Situação da reserva", example = "ATIVA", allowableValues = ["ATIVA", "LIBERADA", "CONSUMIDA"])
    val situacao: String,
) {
    companion object {
        fun de(reserva: Reserva) = ReservaResponse(
            id = requireNotNull(reserva.id),
            osId = reserva.osId,
            itemId = reserva.itemId,
            quantidade = reserva.quantidade.valor,
            situacao = reserva.situacao.name,
        )
    }
}
