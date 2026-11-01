package com.fiap.soat.oficina.ordemservico.adapter.config

import com.fiap.soat.oficina.ordemservico.application.usecase.ConsultarOrdensService
import com.fiap.soat.oficina.ordemservico.application.usecase.CriarOrdemServicoService
import com.fiap.soat.oficina.ordemservico.application.usecase.RealizarDiagnosticoService
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ConsultarOrdensUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.CriarOrdemServicoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RealizarDiagnosticoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OrdemServicoBeanConfig {

    @Bean
    fun criarOrdemServicoUseCase(
        repositorio: OrdemServicoRepository,
        eventos: PublicadorDeEventos,
    ): CriarOrdemServicoUseCase = CriarOrdemServicoService(repositorio, eventos)

    @Bean
    fun consultarOrdensUseCase(
        repositorio: OrdemServicoRepository,
    ): ConsultarOrdensUseCase = ConsultarOrdensService(repositorio)

    @Bean
    fun realizarDiagnosticoUseCase(
        repositorio: OrdemServicoRepository,
        eventos: PublicadorDeEventos,
    ): RealizarDiagnosticoUseCase = RealizarDiagnosticoService(repositorio, eventos)
}
