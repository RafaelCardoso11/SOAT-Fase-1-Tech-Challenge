package com.fiap.soat.oficina.ordemservico.adapter

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.mascararDocumento
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MascararDocumentoTest {

    @Test
    fun `mascara mantendo apenas os dois ultimos digitos`() {
        assertThat(mascararDocumento("12345678909")).isEqualTo("*********09")
        assertThat(mascararDocumento("12")).isEqualTo("**")
        assertThat(mascararDocumento("")).isEqualTo("")
    }
}
