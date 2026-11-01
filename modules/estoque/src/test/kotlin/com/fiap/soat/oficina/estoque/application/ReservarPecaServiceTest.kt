package com.fiap.soat.oficina.estoque.application

import com.fiap.soat.oficina.estoque.application.usecase.ReservarPecaService
import com.fiap.soat.oficina.estoque.assertDadoInvalido
import com.fiap.soat.oficina.estoque.domain.error.ConflitoException
import com.fiap.soat.oficina.estoque.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.estoque.domain.event.PecaIndisponivel
import com.fiap.soat.oficina.estoque.domain.event.PecaReservada
import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import com.fiap.soat.oficina.estoque.domain.model.Reserva
import com.fiap.soat.oficina.estoque.domain.model.ResultadoReserva
import com.fiap.soat.oficina.estoque.domain.model.SituacaoReserva
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
import com.fiap.soat.oficina.estoque.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.estoque.domain.port.outbound.ReservaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ReservarPecaServiceTest {

    private val itens = mockk<ItemEstoqueRepository>()
    private val reservas = mockk<ReservaRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val service = ReservarPecaService(itens, reservas, eventos)

    private fun item(saldo: Int = 3, reservado: Int = 0, ativo: Boolean = true) =
        ItemEstoque.reconstituir(1, "Pastilha", null, "PECA", BigDecimal("200.00"), saldo, reservado, ativo)

    private fun semReservaAtiva() {
        every { reservas.buscarAtivaPorOsEItem(7, 1) } returns null
    }

    @Test
    fun `reserva quando ha disponivel, vincula a OS e publica o evento`() {
        val itemSalvo = slot<ItemEstoque>()
        every { itens.buscarPorIdParaAtualizar(1) } returns item()
        semReservaAtiva()
        every { itens.salvar(capture(itemSalvo)) } answers { itemSalvo.captured }
        every { reservas.salvar(any()) } answers { firstArg() }

        val resultado = service.reservar(7, 1, 1)

        val reservada = resultado as ResultadoReserva.Reservada
        assertThat(reservada.reserva.osId).isEqualTo(7)
        assertThat(reservada.reserva.itemId).isEqualTo(1)
        assertThat(reservada.reserva.quantidade.valor).isEqualTo(1)
        assertThat(reservada.reserva.situacao).isEqualTo(SituacaoReserva.ATIVA)
        assertThat(itemSalvo.captured.reservado).isEqualTo(1)
        assertThat(itemSalvo.captured.disponivel).isEqualTo(2)
        verify(exactly = 1) { eventos.publicar(PecaReservada(7, 1, 1)) }
    }

    @Test
    fun `reservar exatamente o disponivel funciona e zera o disponivel`() {
        val itemSalvo = slot<ItemEstoque>()
        every { itens.buscarPorIdParaAtualizar(1) } returns item(saldo = 3)
        semReservaAtiva()
        every { itens.salvar(capture(itemSalvo)) } answers { itemSalvo.captured }
        every { reservas.salvar(any()) } answers { firstArg() }

        assertThat(service.reservar(7, 1, 3)).isInstanceOf(ResultadoReserva.Reservada::class.java)
        assertThat(itemSalvo.captured.disponivel).isZero()
    }

    @Test
    fun `sem disponivel suficiente nao cria reserva e devolve indisponivel com o evento`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns item(saldo = 3, reservado = 1)
        semReservaAtiva()

        val resultado = service.reservar(7, 1, 3)

        assertThat(resultado).isEqualTo(ResultadoReserva.Indisponivel(2))
        verify(exactly = 0) { itens.salvar(any()) }
        verify(exactly = 0) { reservas.salvar(any()) }
        verify(exactly = 1) { eventos.publicar(PecaIndisponivel(7, 1, 3, 2)) }
        verify(exactly = 0) { eventos.publicar(ofType<PecaReservada>()) }
    }

    @Test
    fun `item sem nenhuma unidade disponivel e indisponivel`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns item(saldo = 3, reservado = 3)
        semReservaAtiva()

        assertThat(service.reservar(7, 1, 1)).isEqualTo(ResultadoReserva.Indisponivel(0))
    }

    @Test
    fun `item inexistente falha sem publicar evento`() {
        every { itens.buscarPorIdParaAtualizar(9) } returns null

        assertThatThrownBy { service.reservar(7, 9, 1) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
            .hasMessage("Item de estoque não encontrado")
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `item inativo nao pode ser reservado`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns item(ativo = false)

        assertThatThrownBy { service.reservar(7, 1, 1) }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Item inativo não pode ser reservado")
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `OS que ja tem reserva ativa do item recebe conflito`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns item()
        every { reservas.buscarAtivaPorOsEItem(7, 1) } returns Reserva.reconstituir(5, 7, 1, 1, "ATIVA")

        assertThatThrownBy { service.reservar(7, 1, 1) }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("A OS já possui uma reserva ativa para este item")
        verify(exactly = 0) { itens.salvar(any()) }
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `OS pode reservar de novo depois que a reserva foi liberada`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns item()
        every { reservas.buscarAtivaPorOsEItem(7, 1) } returns null
        every { itens.salvar(any()) } answers { firstArg() }
        every { reservas.salvar(any()) } answers { firstArg() }

        assertThat(service.reservar(7, 1, 1)).isInstanceOf(ResultadoReserva.Reservada::class.java)
    }

    @Test
    fun `dado invalido falha antes de consultar o repositorio`() {
        assertDadoInvalido("itemId", "Item é obrigatório") { service.reservar(7, null, 1) }
        assertDadoInvalido("quantidade", "Quantidade deve ser maior que zero") { service.reservar(7, 1, 0) }
        assertDadoInvalido("quantidade", "Quantidade é obrigatória") { service.reservar(7, 1, null) }
        verify(exactly = 0) { itens.buscarPorIdParaAtualizar(any()) }
    }
}
