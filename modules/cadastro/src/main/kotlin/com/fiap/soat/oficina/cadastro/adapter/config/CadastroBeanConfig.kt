package com.fiap.soat.oficina.cadastro.adapter.config

import com.fiap.soat.oficina.cadastro.application.usecase.ConsultarClienteService
import com.fiap.soat.oficina.cadastro.application.usecase.ConsultarServicoService
import com.fiap.soat.oficina.cadastro.application.usecase.ConsultarVeiculoService
import com.fiap.soat.oficina.cadastro.application.usecase.GerenciarClienteService
import com.fiap.soat.oficina.cadastro.application.usecase.GerenciarServicoService
import com.fiap.soat.oficina.cadastro.application.usecase.GerenciarVeiculoService
import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarClienteUseCase
import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarServicoUseCase
import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarVeiculoUseCase
import com.fiap.soat.oficina.cadastro.domain.port.inbound.GerenciarClienteUseCase
import com.fiap.soat.oficina.cadastro.domain.port.inbound.GerenciarServicoUseCase
import com.fiap.soat.oficina.cadastro.domain.port.inbound.GerenciarVeiculoUseCase
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ClienteRepository
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ServicoRepository
import com.fiap.soat.oficina.cadastro.domain.port.outbound.VeiculoRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class CadastroBeanConfig {

    @Bean
    @ConditionalOnMissingBean
    fun clock(): Clock = Clock.systemDefaultZone()

    @Bean
    fun gerenciarClienteUseCase(
        clientes: ClienteRepository,
        veiculos: VeiculoRepository,
    ): GerenciarClienteUseCase = GerenciarClienteService(clientes, veiculos)

    @Bean
    fun consultarClienteUseCase(clientes: ClienteRepository): ConsultarClienteUseCase =
        ConsultarClienteService(clientes)

    @Bean
    fun gerenciarVeiculoUseCase(
        veiculos: VeiculoRepository,
        clientes: ClienteRepository,
        clock: Clock,
    ): GerenciarVeiculoUseCase = GerenciarVeiculoService(veiculos, clientes, clock)

    @Bean
    fun consultarVeiculoUseCase(veiculos: VeiculoRepository): ConsultarVeiculoUseCase =
        ConsultarVeiculoService(veiculos)

    @Bean
    fun gerenciarServicoUseCase(servicos: ServicoRepository): GerenciarServicoUseCase =
        GerenciarServicoService(servicos)

    @Bean
    fun consultarServicoUseCase(servicos: ServicoRepository): ConsultarServicoUseCase =
        ConsultarServicoService(servicos)
}
