package com.fiap.soat.oficina

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class MigracoesFlywayTest {

    private val arquivos: List<String> =
        PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/V*__*.sql")
            .mapNotNull { it.filename }

    @Test
    fun `as migrations sao encontradas`() {
        assertThat(arquivos).isNotEmpty()
    }

    @Test
    fun `nao ha duas migrations com a mesma versao`() {
        val repetidas = arquivos
            .groupBy { it.substringBefore("__").removePrefix("V") }
            .filterValues { it.size > 1 }

        assertThat(repetidas).describedAs("versões de migration repetidas").isEmpty()
    }
}
