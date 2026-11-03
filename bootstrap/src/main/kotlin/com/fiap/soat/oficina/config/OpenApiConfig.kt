package com.fiap.soat.oficina.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("API da Oficina Mecânica")
                .version("v1")
                .description("MVP de gestão de ordens de serviço, clientes, veículos, serviços e peças."),
        )
        .components(
            Components().addSecuritySchemes(
                "bearerAuth",
                SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"),
            ),
        )

    // Cobre os controllers de ordemservico, módulo que não depende do springdoc para anotar @SecurityRequirement.
    @Bean
    fun jwtNasRotasAdministrativas() = OpenApiCustomizer { api ->
        api.paths.filterKeys { it.contains("/admin/") }.values
            .flatMap { it.readOperations() }
            .filter { it.security == null }
            .forEach { it.addSecurityItem(SecurityRequirement().addList("bearerAuth")) }
    }
}
