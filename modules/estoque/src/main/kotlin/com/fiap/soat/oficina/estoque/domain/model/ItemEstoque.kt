package com.fiap.soat.oficina.estoque.domain.model

import com.fiap.soat.oficina.estoque.domain.error.ConflitoException
import com.fiap.soat.oficina.estoque.domain.error.DadoInvalidoException
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import java.math.BigDecimal

class ItemEstoque private constructor(
    val id: Long?,
    val nome: String,
    val descricao: String?,
    val tipo: TipoItem,
    val preco: Dinheiro,
    val saldo: Int,
    val reservado: Int,
    val ativo: Boolean,
) {

    val disponivel: Int get() = saldo - reservado

    fun alterar(nome: String?, descricao: String?, tipo: String?, preco: BigDecimal?): ItemEstoque {
        exigirAtivo(ativo, "Item inativo não pode ser alterado")
        return ItemEstoque(
            id, nomeValido(nome), descricaoValida(descricao), TipoItem.de(tipo), precoValido(preco),
            saldo, reservado, ativo,
        )
    }

    fun registrarEntrada(quantidade: Quantidade): ItemEstoque {
        exigirAtivo(ativo, "Item inativo não pode receber entrada de estoque")
        return ItemEstoque(id, nome, descricao, tipo, preco, saldo + quantidade.valor, reservado, ativo)
    }

    fun inativar(): ItemEstoque {
        if (!ativo) return this
        if (reservado > 0) throw ConflitoException("Item possui reservas ativas e não pode ser inativado")
        return ItemEstoque(id, nome, descricao, tipo, preco, saldo, reservado, false)
    }

    companion object {
        private const val NOME_MAXIMO = 120
        private const val DESCRICAO_MAXIMA = 500

        fun criar(
            nome: String?,
            descricao: String?,
            tipo: String?,
            preco: BigDecimal?,
            saldoInicial: Int?,
        ): ItemEstoque = ItemEstoque(
            null, nomeValido(nome), descricaoValida(descricao), TipoItem.de(tipo), precoValido(preco),
            saldoValido(saldoInicial), 0, true,
        )

        fun reconstituir(
            id: Long,
            nome: String,
            descricao: String?,
            tipo: String,
            preco: BigDecimal,
            saldo: Int,
            reservado: Int,
            ativo: Boolean,
        ): ItemEstoque = ItemEstoque(id, nome, descricao, TipoItem.de(tipo), Dinheiro.of(preco), saldo, reservado, ativo)

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

        private fun saldoValido(valor: Int?): Int {
            val saldo = valor ?: 0
            if (saldo < 0) throw DadoInvalidoException("saldo", "Saldo inicial não pode ser negativo")
            return saldo
        }
    }
}
