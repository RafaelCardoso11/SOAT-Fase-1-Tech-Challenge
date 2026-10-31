package com.fiap.soat.oficina.cadastro.domain.model

import com.fiap.soat.oficina.cadastro.domain.error.DadoInvalidoException
import com.fiap.soat.oficina.shared.domain.vo.Placa

class Veiculo private constructor(
    val id: Long?,
    val placa: Placa,
    val marca: String,
    val modelo: String,
    val ano: Int,
    val clienteId: Long,
    val ativo: Boolean,
) {

    fun alterar(marca: String?, modelo: String?, ano: Int?, clienteId: Long?, anoLimite: Int): Veiculo {
        exigirAtivo(ativo, "Veículo inativo não pode ser alterado")
        return Veiculo(
            id, placa, marcaValida(marca), modeloValido(modelo),
            anoValido(ano, anoLimite), clienteValido(clienteId), ativo,
        )
    }

    fun inativar(): Veiculo = if (ativo) Veiculo(id, placa, marca, modelo, ano, clienteId, false) else this

    companion object {
        private const val TEXTO_MAXIMO = 60
        private const val ANO_MINIMO = 1900

        fun criar(
            placa: String?,
            marca: String?,
            modelo: String?,
            ano: Int?,
            clienteId: Long?,
            anoLimite: Int,
        ): Veiculo = Veiculo(
            null, placaValida(placa), marcaValida(marca), modeloValido(modelo),
            anoValido(ano, anoLimite), clienteValido(clienteId), true,
        )

        fun reconstituir(
            id: Long,
            placa: String,
            marca: String,
            modelo: String,
            ano: Int,
            clienteId: Long,
            ativo: Boolean,
        ): Veiculo = Veiculo(id, Placa.of(placa), marca, modelo, ano, clienteId, ativo)

        private fun placaValida(valor: String?): Placa {
            if (valor.isNullOrBlank()) throw DadoInvalidoException("placa", "Placa é obrigatória")
            return validando("placa") { Placa.of(valor) }
        }

        private fun marcaValida(valor: String?): String =
            textoObrigatorio("marca", "Marca", valor, TEXTO_MAXIMO, feminino = true)

        private fun modeloValido(valor: String?): String = textoObrigatorio("modelo", "Modelo", valor, TEXTO_MAXIMO)

        private fun anoValido(valor: Int?, anoLimite: Int): Int {
            if (valor == null) throw DadoInvalidoException("ano", "Ano é obrigatório")
            if (valor !in ANO_MINIMO..anoLimite) {
                throw DadoInvalidoException("ano", "Ano deve estar entre $ANO_MINIMO e $anoLimite")
            }
            return valor
        }

        private fun clienteValido(valor: Long?): Long =
            valor ?: throw DadoInvalidoException("clienteId", "Cliente é obrigatório")
    }
}
