package com.fiap.soat.oficina.estoque.domain.model

import com.fiap.soat.oficina.estoque.domain.error.DadoInvalidoException

enum class TipoItem {
    PECA,
    INSUMO,
    ;

    companion object {
        fun de(entrada: String?): TipoItem {
            if (entrada.isNullOrBlank()) throw DadoInvalidoException("tipo", "Tipo é obrigatório")
            return entries.firstOrNull { it.name == entrada.trim().uppercase() }
                ?: throw DadoInvalidoException("tipo", "Tipo inválido: use PECA ou INSUMO")
        }
    }
}
