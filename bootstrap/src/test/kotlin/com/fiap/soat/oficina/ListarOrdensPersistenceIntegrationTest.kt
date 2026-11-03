package com.fiap.soat.oficina

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RealizarDiagnosticoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ListarOrdensPersistenceIntegrationTest {

    @Autowired
    lateinit var repositorio: OrdemServicoRepository

    @Autowired
    lateinit var realizarDiagnostico: RealizarDiagnosticoUseCase

    private fun criar(status: StatusOS): Long {
        val id = repositorio.salvar(
            OrdemServico.criar(
                Documento.of("123.456.789-09"),
                Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
                "Barulho ao frear",
            ),
        )
        if (status == StatusOS.RECEBIDA) return id
        realizarDiagnostico.iniciar(id)
        if (status == StatusOS.FINALIZADA) realizarDiagnostico.finalizar(id, pecaIdentificada = false)
        return id
    }

    private fun total(status: StatusOS?) = repositorio.listar(status, 0, 1).totalElementos

    @Test
    fun `lista da OS mais recente para a mais antiga`() {
        val primeira = criar(StatusOS.RECEBIDA)
        val segunda = criar(StatusOS.RECEBIDA)
        val terceira = criar(StatusOS.RECEBIDA)

        val pagina = repositorio.listar(null, 0, 3)

        assertThat(pagina.conteudo.map { it.numero }).containsExactly(terceira, segunda, primeira)
    }

    @Test
    fun `o resumo traz status, documento, veiculo e relato`() {
        val id = criar(StatusOS.RECEBIDA)

        val resumo = repositorio.listar(null, 0, 1).conteudo.single()

        assertThat(resumo.numero).isEqualTo(id)
        assertThat(resumo.status).isEqualTo(StatusOS.RECEBIDA)
        assertThat(resumo.documento.valor).isEqualTo("12345678909")
        assertThat(resumo.veiculo.placa.valor).isEqualTo("ABC1D23")
        assertThat(resumo.veiculo.marca).isEqualTo("Fiat")
        assertThat(resumo.veiculo.modelo).isEqualTo("Uno")
        assertThat(resumo.veiculo.ano).isEqualTo(2020)
        assertThat(resumo.relato).isEqualTo("Barulho ao frear")
    }

    @Test
    fun `filtra por status e so traz OS desse status`() {
        val antesDiagnostico = total(StatusOS.EM_DIAGNOSTICO)
        val antesRecebida = total(StatusOS.RECEBIDA)
        val emDiagnostico = listOf(criar(StatusOS.EM_DIAGNOSTICO), criar(StatusOS.EM_DIAGNOSTICO))
        criar(StatusOS.RECEBIDA)

        val pagina = repositorio.listar(StatusOS.EM_DIAGNOSTICO, 0, 100)

        assertThat(pagina.totalElementos).isEqualTo(antesDiagnostico + 2)
        assertThat(pagina.conteudo.map { it.status }).containsOnly(StatusOS.EM_DIAGNOSTICO)
        assertThat(pagina.conteudo.map { it.numero }).containsAll(emDiagnostico)
        assertThat(total(StatusOS.RECEBIDA)).isEqualTo(antesRecebida + 1)
    }

    @Test
    fun `status sem nenhuma OS devolve pagina vazia`() {
        val pagina = repositorio.listar(StatusOS.ENTREGUE, 0, 20)

        assertThat(pagina.conteudo).isEmpty()
        assertThat(pagina.totalElementos).isZero()
        assertThat(pagina.totalPaginas).isZero()
    }

    @Test
    fun `pagina os resultados e informa totais`() {
        val antes = total(StatusOS.FINALIZADA)
        repeat(5) { criar(StatusOS.FINALIZADA) }
        val totalEsperado = antes + 5

        val primeira = repositorio.listar(StatusOS.FINALIZADA, 0, 2)
        val ultima = repositorio.listar(StatusOS.FINALIZADA, ((totalEsperado - 1) / 2).toInt(), 2)

        assertThat(primeira.conteudo).hasSize(2)
        assertThat(primeira.totalElementos).isEqualTo(totalEsperado)
        assertThat(primeira.totalPaginas).isEqualTo(((totalEsperado + 1) / 2).toInt())
        assertThat(ultima.conteudo).hasSize(if (totalEsperado % 2 == 0L) 2 else 1)
    }

    @Test
    fun `paginas diferentes trazem OS diferentes`() {
        repeat(3) { criar(StatusOS.RECEBIDA) }

        val pagina0 = repositorio.listar(null, 0, 1).conteudo.single().numero
        val pagina1 = repositorio.listar(null, 1, 1).conteudo.single().numero

        assertThat(pagina0).isGreaterThan(pagina1)
    }

    @Test
    fun `pagina alem do fim devolve conteudo vazio mantendo o total`() {
        criar(StatusOS.RECEBIDA)
        val total = total(null)

        val alemDoFim = repositorio.listar(null, 1_000, 20)

        assertThat(alemDoFim.conteudo).isEmpty()
        assertThat(alemDoFim.totalElementos).isEqualTo(total)
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
