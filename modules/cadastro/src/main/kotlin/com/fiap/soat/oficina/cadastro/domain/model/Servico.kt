package com.fiap.soat.oficina.cadastro.domain.model

import com.fiap.soat.oficina.cadastro.domain.error.DadoInvalidoException
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import java.math.BigDecimal

class Servico private constructor(
    val id: Long?,
    val nome: String,
    val descricao: String?,
    val preco: Dinheiro,
    val ativo: Boolean,
) {

    fun alterar(nome: String?, descricao: String?, preco: BigDecimal?): Servico {
        exigirAtivo(ativo, "Serviço inativo não pode ser alterado")
        return Servico(id, nomeValido(nome), descricaoValida(descricao), precoValido(preco), ativo)
    }

    fun inativar(): Servico = if (ativo) Servico(id, nome, descricao, preco, false) else this

    companion object {
        private const val NOME_MAXIMO = 120
        private const val DESCRICAO_MAXIMA = 500

        fun criar(nome: String?, descricao: String?, preco: BigDecimal?): Servico =
            Servico(null, nomeValido(nome), descricaoValida(descricao), precoValido(preco), true)

        fun reconstituir(id: Long, nome: String, descricao: String?, preco: BigDecimal, ativo: Boolean): Servico =
            Servico(id, nome, descricao, Dinheiro.of(preco), ativo)

        private fun nomeValido(valor: String?): String = textoObrigatorio("nome", "Nome", valor, NOME_MAXIMO)

        private fun descricaoValida(valor: String?): String? {
            val texto = valor?.trim()?.ifEmpty { null }
            if (texto != null && texto.length > DESCRICAO_MAXIMA) {
                throw DadoInvalidoException("descricao", "Descrição deve ter no máximo $DESCRICAO_MAXIMA caracteres")
            }
            return texto
        }

        private fun precoValido(valor: BigDecimal?): Dinheiro {
            if (valor == null) throw DadoInvalidoException("preco", "Preço é obrigatório")
            return validando("preco", "Preço não pode ser negativo") { Dinheiro.of(valor) }
        }
    }
}
