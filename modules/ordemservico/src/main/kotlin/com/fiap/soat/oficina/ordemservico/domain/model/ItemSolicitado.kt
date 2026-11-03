package com.fiap.soat.oficina.ordemservico.domain.model

import java.math.BigDecimal

/** Item pedido pelo mecânico; `descricao` e `valor` ausentes são completados pelo catálogo. */
data class ItemSolicitado(
    val tipo: TipoItem,
    val descricao: String? = null,
    val valor: BigDecimal? = null,
    val servicoId: Long? = null,
    val itemEstoqueId: Long? = null,
    val quantidade: Int? = null,
)
