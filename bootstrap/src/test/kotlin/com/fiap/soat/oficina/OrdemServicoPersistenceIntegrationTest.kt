package com.fiap.soat.oficina

import com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence.OrdemServicoSpringDataRepository
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
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
class OrdemServicoPersistenceIntegrationTest {

    @Autowired
    lateinit var repositorio: OrdemServicoRepository

    @Autowired
    lateinit var springData: OrdemServicoSpringDataRepository

    @Test
    fun `salva e persiste a ordem de servico`() {
        val ordem = OrdemServico.criar(
            Documento.of("123.456.789-09"),
            Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
            "Barulho ao frear",
        )

        val id = repositorio.salvar(ordem)

        val persistida = springData.findById(id).orElseThrow()
        assertThat(persistida.documento).isEqualTo("12345678909")
        assertThat(persistida.placa).isEqualTo("ABC1D23")
        assertThat(persistida.status).isEqualTo("RECEBIDA")
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
