package com.fiap.soat.oficina.shared

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EsteiraDeTestesTest {

    @Test
    fun `esteira esta ativa`() {
        assertThat(EsteiraDeTestes.ativo()).isTrue()
    }

    @Test
    fun `soma dois numeros`() {
        assertThat(EsteiraDeTestes.soma(2, 3)).isEqualTo(5)
    }
}
