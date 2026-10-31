package com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto

import com.fiap.soat.oficina.cadastro.domain.model.Servico
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Dados para cadastrar ou alterar um serviço")
data class ServicoRequest(
    @field:Schema(description = "Nome do serviço (único)", example = "Troca de óleo")
    val nome: String? = null,

    @field:Schema(description = "Descrição opcional", example = "Troca de óleo e filtro do motor")
    val descricao: String? = null,

    @field:Schema(description = "Preço em reais, não negativo", example = "150.00")
    val preco: BigDecimal? = null,
)

@Schema(description = "Serviço do catálogo")
data class ServicoResponse(
    val id: Long,
    val nome: String,
    val descricao: String?,
    @field:Schema(description = "Preço em reais com duas casas decimais", example = "150.00")
    val preco: BigDecimal,
    val ativo: Boolean,
) {
    companion object {
        fun de(servico: Servico) = ServicoResponse(
            id = requireNotNull(servico.id),
            nome = servico.nome,
            descricao = servico.descricao,
            preco = servico.preco.valor,
            ativo = servico.ativo,
        )
    }
}
