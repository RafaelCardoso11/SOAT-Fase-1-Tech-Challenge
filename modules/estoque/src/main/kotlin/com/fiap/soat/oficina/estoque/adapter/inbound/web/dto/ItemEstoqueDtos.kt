package com.fiap.soat.oficina.estoque.adapter.inbound.web.dto

import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Dados para cadastrar um item de estoque (peça ou insumo)")
data class ItemEstoqueRequest(
    @field:Schema(description = "Nome do item (único)", example = "Pastilha de freio")
    val nome: String? = null,

    @field:Schema(description = "Descrição opcional", example = "Dianteira, linha popular")
    val descricao: String? = null,

    @field:Schema(description = "Tipo do item", example = "PECA", allowableValues = ["PECA", "INSUMO"])
    val tipo: String? = null,

    @field:Schema(description = "Preço de venda em reais, não negativo", example = "200.00")
    val preco: BigDecimal? = null,

    @field:Schema(description = "Saldo inicial opcional (padrão 0)", example = "10")
    val saldoInicial: Int? = null,
)

@Schema(description = "Dados alteráveis de um item (saldo e reservas não mudam por aqui)")
data class ItemEstoqueAlteracaoRequest(
    @field:Schema(description = "Nome do item (único)", example = "Pastilha de freio")
    val nome: String? = null,

    @field:Schema(description = "Descrição opcional", example = "Dianteira, linha popular")
    val descricao: String? = null,

    @field:Schema(description = "Tipo do item", example = "PECA", allowableValues = ["PECA", "INSUMO"])
    val tipo: String? = null,

    @field:Schema(description = "Preço de venda em reais, não negativo", example = "200.00")
    val preco: BigDecimal? = null,
)

@Schema(description = "Entrada (reposição) de estoque")
data class EntradaEstoqueRequest(
    @field:Schema(description = "Quantidade inteira maior que zero", example = "10")
    val quantidade: Int? = null,
)

@Schema(description = "Item de estoque")
data class ItemEstoqueResponse(
    val id: Long,
    val nome: String,
    val descricao: String?,
    @field:Schema(example = "PECA")
    val tipo: String,
    @field:Schema(description = "Preço em reais com duas casas decimais", example = "200.00")
    val preco: BigDecimal,
    @field:Schema(description = "Quantidade física em estoque", example = "10")
    val saldo: Int,
    @field:Schema(description = "Quantidade reservada para OS", example = "3")
    val reservado: Int,
    @field:Schema(description = "Saldo menos reservado", example = "7")
    val disponivel: Int,
    val ativo: Boolean,
) {
    companion object {
        fun de(item: ItemEstoque) = ItemEstoqueResponse(
            id = requireNotNull(item.id),
            nome = item.nome,
            descricao = item.descricao,
            tipo = item.tipo.name,
            preco = item.preco.valor,
            saldo = item.saldo,
            reservado = item.reservado,
            disponivel = item.disponivel,
            ativo = item.ativo,
        )
    }
}
