package com.fiap.soat.oficina.estoque.application

import com.fiap.soat.oficina.estoque.application.usecase.ConsultarReservasService
import com.fiap.soat.oficina.estoque.domain.model.Reserva
import com.fiap.soat.oficina.estoque.domain.model.SituacaoReserva
import com.fiap.soat.oficina.estoque.domain.port.outbound.ReservaRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConsultarReservasServiceTest {

    private val reservas = mockk<ReservaRepository>()
    private val service = ConsultarReservasService(reservas)

    @Test
    fun `devolve as reservas da OS em todas as situacoes`() {
        every { reservas.listarPorOs(7) } returns listOf(
            Reserva.reconstituir(1, 7, 1, 2, "ATIVA"),
            Reserva.reconstituir(2, 7, 2, 1, "LIBERADA"),
            Reserva.reconstituir(3, 7, 3, 4, "CONSUMIDA"),
        )

        val resultado = service.porOs(7)

        assertThat(resultado.map { it.situacao })
            .containsExactly(SituacaoReserva.ATIVA, SituacaoReserva.LIBERADA, SituacaoReserva.CONSUMIDA)
    }

    @Test
    fun `OS sem reservas devolve lista vazia`() {
        every { reservas.listarPorOs(8) } returns emptyList()

        assertThat(service.porOs(8)).isEmpty()
    }
}
