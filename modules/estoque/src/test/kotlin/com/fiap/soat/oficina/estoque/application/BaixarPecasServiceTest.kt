package com.fiap.soat.oficina.estoque.application

import com.fiap.soat.oficina.estoque.application.usecase.BaixarPecasService
import com.fiap.soat.oficina.estoque.domain.error.ConflitoException
import com.fiap.soat.oficina.estoque.domain.event.PecasBaixadas
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

class BaixarPecasServiceTest {

    private val itens = mockk<ItemEstoqueRepository>()
    private val reservas = mockk<ReservaRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val service = BaixarPecasService(itens, reservas, eventos)

    private val itensSalvos = mutableListOf<ItemEstoque>()

    private fun item(id: Long, saldo: Int, reservado: Int) =
        ItemEstoque.reconstituir(id, "Item $id", null, "PECA", BigDecimal.TEN, saldo, reservado, true)

    private val reservaDoItem1 = Reserva.reconstituir(10, 7, 1, 3, "ATIVA")
    private val reservaDoItem2 = Reserva.reconstituir(11, 7, 2, 2, "ATIVA")

    init {
        every { itens.salvar(any()) } answers { firstArg<ItemEstoque>().also { itensSalvos.add(it) } }
        every { reservas.salvar(any()) } answers { firstArg() }
    }

    @Test
    fun `da baixa em todas as reservas da OS, consome e publica um unico evento`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns item(1, saldo = 10, reservado = 3)
        every { itens.buscarPorIdParaAtualizar(2) } returns item(2, saldo = 5, reservado = 2)
        every { reservas.listarAtivasPorOs(7) } returns listOf(reservaDoItem2, reservaDoItem1)

        val consumidas = service.baixar(7)

        assertThat(consumidas.map { it.situacao }).containsOnly(SituacaoReserva.CONSUMIDA)
        assertThat(itensSalvos.map { Triple(it.id, it.saldo, it.reservado) })
            .containsExactly(Triple(1L, 7, 0), Triple(2L, 3, 0))
        verify(exactly = 1) {
            eventos.publicar(
                PecasBaixadas(7, listOf(PecasBaixadas.ItemBaixado(1, 3), PecasBaixadas.ItemBaixado(2, 2))),
            )
        }
    }

    @Test
    fun `bloqueia os itens em ordem crescente de id`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns item(1, saldo = 10, reservado = 3)
        every { itens.buscarPorIdParaAtualizar(2) } returns item(2, saldo = 5, reservado = 2)
        every { reservas.listarAtivasPorOs(7) } returns listOf(reservaDoItem2, reservaDoItem1)

        service.baixar(7)

        verifyOrder {
            itens.buscarPorIdParaAtualizar(1)
            itens.buscarPorIdParaAtualizar(2)
        }
    }

    @Test
    fun `OS sem reservas ativas devolve lista vazia sem alterar saldo nem publicar`() {
        every { reservas.listarAtivasPorOs(7) } returns emptyList()

        assertThat(service.baixar(7)).isEmpty()

        verify(exactly = 0) { itens.buscarPorIdParaAtualizar(any()) }
        verify(exactly = 0) { itens.salvar(any()) }
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `falha em um item interrompe a baixa sem publicar evento`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns item(1, saldo = 10, reservado = 3)
        every { itens.buscarPorIdParaAtualizar(2) } returns item(2, saldo = 5, reservado = 1)
        every { reservas.listarAtivasPorOs(7) } returns listOf(reservaDoItem1, reservaDoItem2)

        assertThatThrownBy { service.baixar(7) }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Quantidade a dar baixa excede o reservado")
        verify(exactly = 0) { eventos.publicar(any()) }
    }
}
