package com.fiap.soat.oficina.estoque.domain.model

import com.fiap.soat.oficina.estoque.domain.error.ConflitoException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ReservaTest {

    private fun ativa() = Reserva.criar(7, 1, Quantidade.of(2))

    @Test
    fun `cria reserva ativa vinculada a OS e ao item`() {
        val reserva = ativa()

        assertThat(reserva.id).isNull()
        assertThat(reserva.osId).isEqualTo(7)
        assertThat(reserva.itemId).isEqualTo(1)
        assertThat(reserva.quantidade.valor).isEqualTo(2)
        assertThat(reserva.situacao).isEqualTo(SituacaoReserva.ATIVA)
        assertThat(reserva.ativa).isTrue()
    }

    @Test
    fun `liberar muda para liberada sem alterar a original`() {
        val original = ativa()

        val liberada = original.liberar()

        assertThat(liberada.situacao).isEqualTo(SituacaoReserva.LIBERADA)
        assertThat(liberada.ativa).isFalse()
        assertThat(original.situacao).isEqualTo(SituacaoReserva.ATIVA)
    }

    @Test
    fun `consumir muda para consumida`() {
        assertThat(ativa().consumir().situacao).isEqualTo(SituacaoReserva.CONSUMIDA)
    }

    @Test
    fun `reserva liberada nao pode ser liberada nem consumida`() {
        val liberada = ativa().liberar()

        assertThatThrownBy { liberada.liberar() }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Reserva não está ativa e não pode ser alterada")
        assertThatThrownBy { liberada.consumir() }.isInstanceOf(ConflitoException::class.java)
    }

    @Test
    fun `reserva consumida nao pode ser liberada nem consumida`() {
        val consumida = ativa().consumir()

        assertThatThrownBy { consumida.liberar() }.isInstanceOf(ConflitoException::class.java)
        assertThatThrownBy { consumida.consumir() }.isInstanceOf(ConflitoException::class.java)
    }

    @Test
    fun `reconstitui uma reserva persistida`() {
        val reserva = Reserva.reconstituir(5, 7, 1, 3, "CONSUMIDA")

        assertThat(reserva.id).isEqualTo(5)
        assertThat(reserva.quantidade.valor).isEqualTo(3)
        assertThat(reserva.situacao).isEqualTo(SituacaoReserva.CONSUMIDA)
    }
}
