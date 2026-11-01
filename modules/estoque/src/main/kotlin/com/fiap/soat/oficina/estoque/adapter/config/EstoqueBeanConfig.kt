package com.fiap.soat.oficina.estoque.adapter.config

import com.fiap.soat.oficina.estoque.application.usecase.ConsultarItemService
import com.fiap.soat.oficina.estoque.application.usecase.GerenciarItemService
import com.fiap.soat.oficina.estoque.domain.port.inbound.ConsultarItemUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.GerenciarItemUseCase
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EstoqueBeanConfig {

    @Bean
    fun gerenciarItemUseCase(itens: ItemEstoqueRepository): GerenciarItemUseCase = GerenciarItemService(itens)

    @Bean
    fun consultarItemUseCase(itens: ItemEstoqueRepository): ConsultarItemUseCase = ConsultarItemService(itens)
}
