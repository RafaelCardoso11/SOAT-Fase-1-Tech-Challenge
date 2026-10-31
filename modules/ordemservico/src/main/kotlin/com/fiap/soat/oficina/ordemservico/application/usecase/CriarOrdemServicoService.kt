package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.event.OrdemServicoCriada
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.CriarOrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.CriarOrdemServicoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa

class CriarOrdemServicoService(
    private val repositorio: OrdemServicoRepository,
    private val eventos: PublicadorDeEventos,
) : CriarOrdemServicoUseCase {

    override fun criar(comando: CriarOrdemServico): Long {
        require(comando.documento.isNotBlank()) { "CPF/CNPJ é obrigatório" }
        require(comando.placa.isNotBlank()) { "Placa é obrigatória" }

        val documento = Documento.of(comando.documento)
        val placa = Placa.of(comando.placa)
        val veiculo = Veiculo(placa, comando.marca, comando.modelo, comando.ano)

        val id = repositorio.salvar(OrdemServico.criar(documento, veiculo, comando.relato))
        eventos.publicar(OrdemServicoCriada(id))
        return id
    }
}
