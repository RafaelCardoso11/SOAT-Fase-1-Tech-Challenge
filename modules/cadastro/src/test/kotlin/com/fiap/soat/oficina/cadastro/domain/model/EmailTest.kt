package com.fiap.soat.oficina.cadastro.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EmailTest {

    @Test
    fun `normaliza espacos e maiusculas`() {
        assertThat(Email.of("  Maria@Email.COM ").valor).isEqualTo("maria@email.com")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "sem-arroba", "@email.com", "maria@", "maria@email", "ma ria@email.com", "a@@b.com"])
    fun `rejeita formato invalido`(entrada: String) {
        assertThatThrownBy { Email.of(entrada) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("E-mail inválido")
    }

    @Test
    fun `aceita 150 caracteres e rejeita acima disso`() {
        assertThat(Email.of("a".repeat(140) + "@email.com").valor).hasSize(150)

        assertThatThrownBy { Email.of("a".repeat(141) + "@email.com") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("E-mail deve ter no máximo 150 caracteres")
    }
}
