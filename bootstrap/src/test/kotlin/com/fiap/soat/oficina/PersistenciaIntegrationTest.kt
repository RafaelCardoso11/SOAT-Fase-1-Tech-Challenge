package com.fiap.soat.oficina

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PersistenciaIntegrationTest {

    @Autowired
    lateinit var dataSource: DataSource

    @Test
    fun `flyway cria os schemas por modulo no postgres`() {
        val schemas = mutableSetOf<String>()
        dataSource.connection.use { connection ->
            connection.metaData.schemas.use { rs ->
                while (rs.next()) {
                    schemas.add(rs.getString("TABLE_SCHEM"))
                }
            }
        }
        assertThat(schemas).contains("autenticacao", "cadastro", "estoque", "ordemservico")
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
