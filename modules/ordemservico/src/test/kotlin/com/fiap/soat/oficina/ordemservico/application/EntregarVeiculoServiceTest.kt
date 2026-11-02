package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.EntregarVeiculoService
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.event.VeiculoEntregue
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class EntregarVeiculoServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val service = EntregarVeiculoService(repositorio, eventos)

    private fun os(status: StatusOS) = OrdemServico.reconstituir(
        id = 1L,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = status,
        transicoes = listOf(TransicaoStatus(status, Instant.now())),
    )

    @Test
    fun `entrega muda para entregue e publica eventos na ordem`() {
        val ordem = os(StatusOS.FINALIZADA)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L

        service.entregar(1L)

        assertThat(ordem.status).isEqualTo(StatusOS.ENTREGUE)
        verifyOrder {
            eventos.publicar(VeiculoEntregue(1L))
            eventos.publicar(OsEncerrada(1L))
        }
    }

    @Test
    fun `bloqueia entrega de OS nao finalizada`() {
        val ordem = os(StatusOS.EM_EXECUCAO)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.entregar(1L) }.isInstanceOf(IllegalStateException::class.java)
        assertThat(ordem.status).isEqualTo(StatusOS.EM_EXECUCAO)
        verify(exactly = 0) { repositorio.salvar(any()) }
    }

    @Test
    fun `OS inexistente lanca NoSuchElement`() {
        every { repositorio.buscarPorId(99L) } returns null

        assertThatThrownBy { service.entregar(99L) }.isInstanceOf(NoSuchElementException::class.java)
    }
}
