package com.fiap.soat.oficina.ordemservico.adapter.config

import com.fiap.soat.oficina.ordemservico.application.usecase.AprovarOrcamentoService
import com.fiap.soat.oficina.ordemservico.application.usecase.ConsultarOrdensService
import com.fiap.soat.oficina.ordemservico.application.usecase.CriarOrdemServicoService
import com.fiap.soat.oficina.ordemservico.application.usecase.ExpirarOrcamentosService
import com.fiap.soat.oficina.ordemservico.application.usecase.FinalizarExecucaoService
import com.fiap.soat.oficina.ordemservico.application.usecase.GerarOrcamentoService
import com.fiap.soat.oficina.ordemservico.application.usecase.IniciarExecucaoService
import com.fiap.soat.oficina.ordemservico.application.usecase.RealizarDiagnosticoService
import com.fiap.soat.oficina.ordemservico.application.usecase.RejeitarOrcamentoService
import com.fiap.soat.oficina.ordemservico.application.usecase.RevisarOrcamentoService
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.AprovarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ConsultarOrdensUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.CriarOrdemServicoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ExpirarOrcamentosUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.FinalizarExecucaoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.GerarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.IniciarExecucaoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RealizarDiagnosticoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RejeitarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RevisarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import org.springframework.beans.factory.annotation.Value
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

    @Bean
    fun gerarOrcamentoUseCase(
        repositorio: OrdemServicoRepository,
        eventos: PublicadorDeEventos,
        estoque: EstoquePort,
        notificacao: NotificacaoPort,
        @Value("\${orcamento.validade-dias}") validadeDias: Long,
    ): GerarOrcamentoUseCase = GerarOrcamentoService(repositorio, eventos, estoque, notificacao, validadeDias)

    @Bean
    fun aprovarOrcamentoUseCase(
        repositorio: OrdemServicoRepository,
        eventos: PublicadorDeEventos,
    ): AprovarOrcamentoUseCase = AprovarOrcamentoService(repositorio, eventos)

    @Bean
    fun rejeitarOrcamentoUseCase(
        repositorio: OrdemServicoRepository,
        eventos: PublicadorDeEventos,
        estoque: EstoquePort,
        notificacao: NotificacaoPort,
    ): RejeitarOrcamentoUseCase = RejeitarOrcamentoService(repositorio, eventos, estoque, notificacao)

    @Bean
    fun revisarOrcamentoUseCase(
        repositorio: OrdemServicoRepository,
        eventos: PublicadorDeEventos,
        estoque: EstoquePort,
        notificacao: NotificacaoPort,
        @Value("\${orcamento.validade-dias}") validadeDias: Long,
    ): RevisarOrcamentoUseCase = RevisarOrcamentoService(repositorio, eventos, estoque, notificacao, validadeDias)

    @Bean
    fun iniciarExecucaoUseCase(
        repositorio: OrdemServicoRepository,
        eventos: PublicadorDeEventos,
        estoque: EstoquePort,
    ): IniciarExecucaoUseCase = IniciarExecucaoService(repositorio, eventos, estoque)

    @Bean
    fun finalizarExecucaoUseCase(
        repositorio: OrdemServicoRepository,
        eventos: PublicadorDeEventos,
        estoque: EstoquePort,
        notificacao: NotificacaoPort,
    ): FinalizarExecucaoUseCase = FinalizarExecucaoService(repositorio, eventos, estoque, notificacao)

    @Bean
    fun expirarOrcamentosUseCase(
        repositorio: OrdemServicoRepository,
        eventos: PublicadorDeEventos,
        estoque: EstoquePort,
        notificacao: NotificacaoPort,
    ): ExpirarOrcamentosUseCase = ExpirarOrcamentosService(repositorio, eventos, estoque, notificacao)
}
