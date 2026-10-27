package com.fiap.soat.oficina.shared.domain.vo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DocumentoTest {

    @Test
    fun `aceita CPF valido e normaliza`() {
        assertThat(Documento.of("123.456.789-09").valor).isEqualTo("12345678909")
    }

    @Test
    fun `aceita CNPJ valido`() {
        assertThat(Documento.of("11.222.333/0001-81").valor).isEqualTo("11222333000181")
    }

    @Test
    fun `rejeita CPF com digito verificador invalido`() {
        assertThatThrownBy { Documento.of("123.456.789-00") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejeita documento com todos os digitos iguais`() {
        assertThatThrownBy { Documento.of("111.111.111-11") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejeita CNPJ invalido`() {
        assertThatThrownBy { Documento.of("11.222.333/0001-00") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejeita comprimento invalido`() {
        assertThatThrownBy { Documento.of("123") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
