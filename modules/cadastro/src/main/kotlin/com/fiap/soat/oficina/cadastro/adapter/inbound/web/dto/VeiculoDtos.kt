package com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto

import com.fiap.soat.oficina.cadastro.domain.model.Veiculo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Dados para cadastrar um veículo")
data class VeiculoRequest(
    @field:Schema(description = "Placa (padrão antigo ou Mercosul)", example = "ABC1D23")
    val placa: String? = null,

    @field:Schema(description = "Marca", example = "Fiat")
    val marca: String? = null,

    @field:Schema(description = "Modelo", example = "Uno")
    val modelo: String? = null,

    @field:Schema(description = "Ano do veículo", example = "2020")
    val ano: Int? = null,

    @field:Schema(description = "Identificador do cliente dono (deve estar ativo)", example = "1")
    val clienteId: Long? = null,
)

@Schema(description = "Dados alteráveis de um veículo (a placa não muda)")
data class VeiculoAlteracaoRequest(
    @field:Schema(description = "Marca", example = "Fiat")
    val marca: String? = null,

    @field:Schema(description = "Modelo", example = "Uno")
    val modelo: String? = null,

    @field:Schema(description = "Ano do veículo", example = "2020")
    val ano: Int? = null,

    @field:Schema(description = "Identificador do cliente dono; informe outro para trocar o dono", example = "1")
    val clienteId: Long? = null,
)

@Schema(description = "Veículo cadastrado")
data class VeiculoResponse(
    val id: Long,
    @field:Schema(description = "Placa normalizada", example = "ABC1D23")
    val placa: String,
    val marca: String,
    val modelo: String,
    val ano: Int,
    val clienteId: Long,
    val ativo: Boolean,
) {
    companion object {
        fun de(veiculo: Veiculo) = VeiculoResponse(
            id = requireNotNull(veiculo.id),
            placa = veiculo.placa.valor,
            marca = veiculo.marca,
            modelo = veiculo.modelo,
            ano = veiculo.ano,
            clienteId = veiculo.clienteId,
            ativo = veiculo.ativo,
        )
    }
}
