package com.fiap.soat.oficina

import com.fiap.soat.oficina.ordemservico.domain.model.ItemSolicitado
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.GerarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RealizarDiagnosticoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RevisarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
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
import java.math.BigDecimal

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RevisarOrcamentoPersistenceIntegrationTest {

    @Autowired
    lateinit var repositorio: OrdemServicoRepository

    @Autowired
    lateinit var realizarDiagnostico: RealizarDiagnosticoUseCase

    @Autowired
    lateinit var gerarOrcamento: GerarOrcamentoUseCase

    @Autowired
    lateinit var revisarOrcamento: RevisarOrcamentoUseCase

    @Test
    fun `revisa orcamento substituindo itens mantendo aguardando aprovacao`() {
        val id = repositorio.salvar(
            OrdemServico.criar(
                Documento.of("123.456.789-09"),
                Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
                "Barulho ao frear",
            ),
        )
        realizarDiagnostico.iniciar(id)
        realizarDiagnostico.finalizar(id, pecaIdentificada = true)
        gerarOrcamento.gerar(
            id,
            listOf(ItemSolicitado(TipoItem.SERVICO, "Troca de pastilhas", BigDecimal("150.00"))),
        )

        revisarOrcamento.revisar(
            id,
            listOf(ItemSolicitado(TipoItem.SERVICO, "Troca de pastilhas", BigDecimal("120.00"))),
        )

        val atualizada = repositorio.buscarPorId(id)!!
        assertThat(atualizada.status).isEqualTo(StatusOS.AGUARDANDO_APROVACAO)
        assertThat(atualizada.orcamento!!.total.valor).isEqualByComparingTo("120.00")
        assertThat(atualizada.orcamento!!.itens).hasSize(1)
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
