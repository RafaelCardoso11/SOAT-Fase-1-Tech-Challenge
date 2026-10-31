package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.ConsultarOrdensService
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ConsultarOrdensServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val service = ConsultarOrdensService(repositorio)

    private fun os() = OrdemServico.reconstituir(
        id = 10L,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = StatusOS.RECEBIDA,
        transicoes = listOf(TransicaoStatus(StatusOS.RECEBIDA, Instant.now())),
    )

    @Test
    fun `consulta por numero devolve a OS com linha do tempo`() {
        every { repositorio.buscarPorId(10L) } returns os()

        val encontrada = service.porNumero(10L)

        assertThat(encontrada?.id).isEqualTo(10L)
        assertThat(encontrada?.transicoes).singleElement()
            .extracting { it.status }.isEqualTo(StatusOS.RECEBIDA)
    }

    @Test
    fun `consulta por numero inexistente devolve nulo`() {
        every { repositorio.buscarPorId(99L) } returns null

        assertThat(service.porNumero(99L)).isNull()
    }

    @Test
    fun `consulta por placa devolve a lista`() {
        every { repositorio.buscarPorPlaca("ABC1D23") } returns listOf(os())

        assertThat(service.porPlaca("ABC1D23")).hasSize(1)
    }

    @Test
    fun `placa malformada devolve lista vazia sem erro`() {
        assertThat(service.porPlaca("123")).isEmpty()
    }

    @Test
    fun `consulta por documento devolve a lista`() {
        every { repositorio.buscarPorDocumento("12345678909") } returns listOf(os())

        assertThat(service.porDocumento("123.456.789-09")).hasSize(1)
    }

    @Test
    fun `documento malformado devolve lista vazia sem erro`() {
        assertThat(service.porDocumento("000")).isEmpty()
    }
}
