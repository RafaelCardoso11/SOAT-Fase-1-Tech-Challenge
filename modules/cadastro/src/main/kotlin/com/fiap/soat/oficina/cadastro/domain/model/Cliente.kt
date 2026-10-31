package com.fiap.soat.oficina.cadastro.domain.model

import com.fiap.soat.oficina.cadastro.domain.error.DadoInvalidoException
import com.fiap.soat.oficina.shared.domain.vo.Documento

class Cliente private constructor(
    val id: Long?,
    val documento: Documento,
    val nome: String,
    val email: Email,
    val ativo: Boolean,
) {

    fun alterar(nome: String?, email: String?): Cliente {
        exigirAtivo(ativo, "Cliente inativo não pode ser alterado")
        return Cliente(id, documento, nomeValido(nome), emailValido(email), ativo)
    }

    fun inativar(): Cliente = if (ativo) Cliente(id, documento, nome, email, false) else this

    companion object {
        private const val NOME_MAXIMO = 150

        fun criar(documento: String?, nome: String?, email: String?): Cliente =
            Cliente(null, documentoValido(documento), nomeValido(nome), emailValido(email), true)

        fun reconstituir(id: Long, documento: String, nome: String, email: String, ativo: Boolean): Cliente =
            Cliente(id, Documento.of(documento), nome, Email.of(email), ativo)

        private fun documentoValido(valor: String?): Documento {
            if (valor.isNullOrBlank()) throw DadoInvalidoException("documento", "CPF/CNPJ é obrigatório")
            return validando("documento") { Documento.of(valor) }
        }

        private fun nomeValido(valor: String?): String = textoObrigatorio("nome", "Nome", valor, NOME_MAXIMO)

        private fun emailValido(valor: String?): Email {
            if (valor.isNullOrBlank()) throw DadoInvalidoException("email", "E-mail é obrigatório")
            return validando("email") { Email.of(valor) }
        }
    }
}
