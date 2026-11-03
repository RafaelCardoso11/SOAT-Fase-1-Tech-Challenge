package com.fiap.soat.oficina.ordemservico.domain.model

data class Pagina<T>(
    val conteudo: List<T>,
    val pagina: Int,
    val tamanho: Int,
    val totalElementos: Long,
) {
    val totalPaginas: Int get() = ((totalElementos + tamanho - 1) / tamanho).toInt()
}
