package com.fiap.soat.oficina.estoque.application

import com.fiap.soat.oficina.estoque.application.usecase.LiberarReservaService
import com.fiap.soat.oficina.estoque.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.estoque.domain.event.ReservaLiberada
import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import com.fiap.soat.oficina.estoque.domain.model.Reserva
import com.fiap.soat.oficina.estoque.domain.model.SituacaoReserva
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
import com.fiap.soat.oficina.estoque.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.estoque.domain.port.outbound.ReservaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class LiberarReservaServiceTest {

    private val itens = mockk<ItemEstoqueRepository>()
    private val reservas = mockk<ReservaRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val service = LiberarReservaService(itens, reservas, eventos)

    private val reservaDoItem1 = Reserva.reconstituir(10, 7, 1, 3, "ATIVA")
    private val reservaDoItem2 = Reserva.reconstituir(11, 7, 2, 2, "ATIVA")

    private val itensSalvos = mutableListOf<ItemEstoque>()
    private val reservasSalvas = mutableListOf<Reserva>()

    init {
        every { itens.buscarPorIdParaAtualizar(1) } returns
            ItemEstoque.reconstituir(1, "Pastilha", null, "PECA", BigDecimal.TEN, 10, 3, true)
        every { itens.buscarPorIdParaAtualizar(2) } returns
            ItemEstoque.reconstituir(2, "Disco", null, "PECA", BigDecimal.TEN, 10, 2, true)
        every { itens.salvar(any()) } answers { firstArg<ItemEstoque>().also { itensSalvos.add(it) } }
        every { reservas.salvar(any()) } answers { firstArg<Reserva>().also { reservasSalvas.add(it) } }
    }

    @Test
    fun `libera todas as reservas ativas da OS devolvendo ao disponivel`() {
        every { reservas.listarAtivasPorOs(7) } returns listOf(reservaDoItem2, reservaDoItem1)

        service.liberar(7)

        assertThat(itensSalvos.map { it.id to it.reservado }).containsExactly(1L to 0, 2L to 0)
        assertThat(reservasSalvas.map { it.situacao }).containsOnly(SituacaoReserva.LIBERADA)
        verify(exactly = 1) { eventos.publicar(ReservaLiberada(7, 1, 3)) }
        verify(exactly = 1) { eventos.publicar(ReservaLiberada(7, 2, 2)) }
    }

    @Test
    fun `bloqueia os itens em ordem crescente de id`() {
        every { reservas.listarAtivasPorOs(7) } returns listOf(reservaDoItem2, reservaDoItem1)

        service.liberar(7)

        verifyOrder {
            itens.buscarPorIdParaAtualizar(1)
            itens.buscarPorIdParaAtualizar(2)
        }
    }

    @Test
    fun `libera so a reserva do item informado`() {
        every { reservas.listarAtivasPorOs(7) } returns listOf(reservaDoItem1, reservaDoItem2)

        service.liberar(7, itemId = 2)

        assertThat(itensSalvos.map { it.id }).containsExactly(2L)
        verify(exactly = 1) { eventos.publicar(ReservaLiberada(7, 2, 2)) }
        verify(exactly = 0) { itens.buscarPorIdParaAtualizar(1) }
    }

    @Test
    fun `sem reservas ativas nao faz nada nem publica evento`() {
        every { reservas.listarAtivasPorOs(7) } returns emptyList()

        service.liberar(7)

        verify(exactly = 0) { itens.buscarPorIdParaAtualizar(any()) }
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `item sem reserva ativa na OS nao faz nada`() {
        every { reservas.listarAtivasPorOs(7) } returns listOf(reservaDoItem1)

        service.liberar(7, itemId = 2)

        verify(exactly = 0) { itens.salvar(any()) }
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `reserva que aponta para item inexistente falha`() {
        every { reservas.listarAtivasPorOs(7) } returns listOf(Reserva.reconstituir(12, 7, 9, 1, "ATIVA"))
        every { itens.buscarPorIdParaAtualizar(9) } returns null

        assertThatThrownBy { service.liberar(7) }.isInstanceOf(RegistroNaoEncontradoException::class.java)
        verify(exactly = 0) { eventos.publicar(any()) }
    }
}
