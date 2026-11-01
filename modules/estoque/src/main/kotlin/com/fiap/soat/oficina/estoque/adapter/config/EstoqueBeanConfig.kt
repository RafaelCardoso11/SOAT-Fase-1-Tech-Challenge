package com.fiap.soat.oficina.estoque.adapter.config

import com.fiap.soat.oficina.estoque.application.usecase.BaixarPecasService
import com.fiap.soat.oficina.estoque.application.usecase.ConsultarItemService
import com.fiap.soat.oficina.estoque.application.usecase.ConsultarReservasService
import com.fiap.soat.oficina.estoque.application.usecase.GerenciarItemService
import com.fiap.soat.oficina.estoque.application.usecase.LiberarReservaService
import com.fiap.soat.oficina.estoque.application.usecase.ReservarPecaService
import com.fiap.soat.oficina.estoque.domain.port.inbound.BaixarPecasUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.ConsultarItemUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.ConsultarReservasUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.GerenciarItemUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.LiberarReservaUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.ReservarPecaUseCase
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
import com.fiap.soat.oficina.estoque.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.estoque.domain.port.outbound.ReservaRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EstoqueBeanConfig {

    @Bean
    fun gerenciarItemUseCase(itens: ItemEstoqueRepository): GerenciarItemUseCase = GerenciarItemService(itens)

    @Bean
    fun consultarItemUseCase(itens: ItemEstoqueRepository): ConsultarItemUseCase = ConsultarItemService(itens)

    @Bean
    fun reservarPecaUseCase(
        itens: ItemEstoqueRepository,
        reservas: ReservaRepository,
        eventos: PublicadorDeEventos,
    ): ReservarPecaUseCase = ReservarPecaService(itens, reservas, eventos)

    @Bean
    fun liberarReservaUseCase(
        itens: ItemEstoqueRepository,
        reservas: ReservaRepository,
        eventos: PublicadorDeEventos,
    ): LiberarReservaUseCase = LiberarReservaService(itens, reservas, eventos)

    @Bean
    fun baixarPecasUseCase(
        itens: ItemEstoqueRepository,
        reservas: ReservaRepository,
        eventos: PublicadorDeEventos,
    ): BaixarPecasUseCase = BaixarPecasService(itens, reservas, eventos)

    @Bean
    fun consultarReservasUseCase(reservas: ReservaRepository): ConsultarReservasUseCase =
        ConsultarReservasService(reservas)
}
