package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.ConsultarOrdensService
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.Pagina
import com.fiap.soat.oficina.ordemservico.domain.model.ResumoOrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

    private fun resumo(numero: Long, status: StatusOS) = ResumoOrdemServico(
        numero = numero,
        status = status,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
    )

    @Test
    fun `lista repassa filtro e paginacao ao repositorio`() {
        val pagina = Pagina(listOf(resumo(2L, StatusOS.EM_DIAGNOSTICO)), pagina = 1, tamanho = 5, totalElementos = 6)
        every { repositorio.listar(StatusOS.EM_DIAGNOSTICO, 1, 5) } returns pagina

        val resultado = service.listar(StatusOS.EM_DIAGNOSTICO, 1, 5)

        assertThat(resultado).isSameAs(pagina)
        assertThat(resultado.totalPaginas).isEqualTo(2)
    }

    @Test
    fun `lista sem filtro de status traz todas`() {
        every { repositorio.listar(null, 0, 20) } returns Pagina(emptyList(), 0, 20, 0)

        assertThat(service.listar(null, 0, 20).conteudo).isEmpty()
    }

    @Test
    fun `lista aceita o tamanho maximo de 100`() {
        every { repositorio.listar(null, 0, 100) } returns Pagina(emptyList(), 0, 100, 0)

        assertThat(service.listar(null, 0, 100).tamanho).isEqualTo(100)
    }

    @Test
    fun `pagina negativa e rejeitada`() {
        assertThatThrownBy { service.listar(null, -1, 20) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("A página deve ser maior ou igual a zero")
    }

    @Test
    fun `tamanho fora do limite e rejeitado`() {
        listOf(0, -1, 101).forEach { tamanho ->
            assertThatThrownBy { service.listar(null, 0, tamanho) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("O tamanho da página deve estar entre 1 e 100")
        }
    }

    @Test
    fun `detalhar devolve a OS`() {
        every { repositorio.buscarPorId(10L) } returns os()

        assertThat(service.detalhar(10L).id).isEqualTo(10L)
    }

    @Test
    fun `detalhar OS inexistente falha com nao encontrado`() {
        every { repositorio.buscarPorId(99L) } returns null

        assertThatThrownBy { service.detalhar(99L) }
            .isInstanceOf(NoSuchElementException::class.java)
            .hasMessage("Ordem de serviço não encontrada")
    }
}
