package com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto

import com.fiap.soat.oficina.cadastro.domain.model.Cliente
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Dados para cadastrar um cliente")
data class ClienteRequest(
    @field:Schema(description = "CPF ou CNPJ, com ou sem pontuação", example = "123.456.789-09")
    val documento: String? = null,

    @field:Schema(description = "Nome do cliente", example = "Maria Silva")
    val nome: String? = null,

    @field:Schema(description = "E-mail do cliente", example = "maria@email.com")
    val email: String? = null,
)

@Schema(description = "Dados alteráveis de um cliente (o documento não muda)")
data class ClienteAlteracaoRequest(
    @field:Schema(description = "Nome do cliente", example = "Maria Silva")
    val nome: String? = null,

    @field:Schema(description = "E-mail do cliente", example = "maria@email.com")
    val email: String? = null,
)

@Schema(description = "Cliente cadastrado")
data class ClienteResponse(
    val id: Long,
    @field:Schema(description = "CPF/CNPJ somente com dígitos", example = "12345678909")
    val documento: String,
    val nome: String,
    val email: String,
    val ativo: Boolean,
) {
    companion object {
        fun de(cliente: Cliente) = ClienteResponse(
            id = requireNotNull(cliente.id),
            documento = cliente.documento.valor,
            nome = cliente.nome,
            email = cliente.email.valor,
            ativo = cliente.ativo,
        )
    }
}
