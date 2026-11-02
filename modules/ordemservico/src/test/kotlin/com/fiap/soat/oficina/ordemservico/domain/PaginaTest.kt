package com.fiap.soat.oficina.ordemservico.domain

import com.fiap.soat.oficina.ordemservico.domain.model.Pagina
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PaginaTest {

    @Test
    fun `sem elementos nao ha paginas`() {
        assertThat(Pagina<String>(emptyList(), 0, 20, 0).totalPaginas).isZero()
    }

    @Test
    fun `arredonda para cima o total de paginas`() {
        assertThat(Pagina(listOf("a", "b"), 0, 2, 5).totalPaginas).isEqualTo(3)
    }

    @Test
    fun `multiplo exato do tamanho nao cria pagina extra`() {
        assertThat(Pagina(listOf("a", "b"), 0, 2, 4).totalPaginas).isEqualTo(2)
    }

    @Test
    fun `cabe em uma unica pagina`() {
        assertThat(Pagina(listOf("a"), 0, 20, 1).totalPaginas).isEqualTo(1)
    }
}
