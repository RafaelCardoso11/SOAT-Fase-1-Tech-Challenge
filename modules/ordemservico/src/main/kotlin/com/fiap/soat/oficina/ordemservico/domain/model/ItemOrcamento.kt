package com.fiap.soat.oficina.ordemservico.domain.model

import com.fiap.soat.oficina.shared.domain.vo.Dinheiro

data class ItemOrcamento(val tipo: TipoItem, val descricao: String, val valor: Dinheiro)
