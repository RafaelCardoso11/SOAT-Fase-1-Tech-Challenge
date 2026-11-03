package com.fiap.soat.oficina.bdd

import com.fiap.soat.oficina.ordemservico.application.usecase.GerarOrcamentoService
import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoGerado
import com.fiap.soat.oficina.ordemservico.domain.model.ItemSolicitado
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.CatalogoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.ItemCatalogo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.event.DomainEvent
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.cucumber.datatable.DataTable
import io.cucumber.java.pt.Dado
import io.cucumber.java.pt.Entao
import io.cucumber.java.pt.Quando
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.Instant

class GerarOrcamentoSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>()
    private val estoque = mockk<EstoquePort>(relaxed = true)
    private val catalogo = mockk<CatalogoPort> {
        every { buscarPeca(any()) } returns ItemCatalogo("Peça do estoque", Dinheiro.of(BigDecimal("100.00")))
    }
    private val notificacao = mockk<NotificacaoPort>(relaxed = true)
    private val service = GerarOrcamentoService(repositorio, eventos, estoque, catalogo, notificacao, validadeDias = 7L)

    private lateinit var ordem: OrdemServico
    private val publicados = mutableListOf<DomainEvent>()

    private fun prepara(status: StatusOS, liberado: Boolean) {
        ordem = OrdemServico.reconstituir(
            id = 1L,
            documento = Documento.of("123.456.789-09"),
            veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
            relato = "Barulho ao frear",
            status = status,
            transicoes = listOf(TransicaoStatus(status, Instant.now())),
            liberadoParaOrcamento = liberado,
        )
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(any()) } returns 1L
        every { eventos.publicar(capture(publicados)) } just Runs
        every { estoque.reservar(any(), any(), any()) } returns true
    }

    @Dado("uma OS com diagnóstico finalizado")
    fun osComDiagnosticoFinalizado() = prepara(StatusOS.EM_DIAGNOSTICO, liberado = true)

    @Dado("uma OS em diagnóstico sem o diagnóstico finalizado")
    fun osSemDiagnosticoFinalizado() = prepara(StatusOS.EM_DIAGNOSTICO, liberado = false)

    @Dado("uma OS no status {string}")
    fun osNoStatus(status: String) = prepara(statusDe(status), liberado = false)

    @Quando("o mecânico montar o orçamento com os itens:")
    fun montarOrcamento(tabela: DataTable) {
        val itens = tabela.asMaps().map {
            val tipo = tipoDe(it["tipo"]!!)
            val peca = tipo == TipoItem.PECA
            ItemSolicitado(
                tipo, it["descrição"]!!, BigDecimal(it["valor"]!!),
                itemEstoqueId = if (peca) 50L else null, quantidade = if (peca) 1 else null,
            )
        }
        service.gerar(1L, itens)
    }

    @Quando("o mecânico tentar gerar o orçamento")
    fun tentarGerar() {
        runCatching {
            service.gerar(1L, listOf(ItemSolicitado(TipoItem.SERVICO, "Item", BigDecimal("10.00"))))
        }
    }

    @Entao("o valor total do orçamento deve ser {string}")
    fun totalDeveSer(valor: String) {
        assertThat(ordem.orcamento!!.total.valor).isEqualByComparingTo(valor)
    }

    @Entao("o orçamento deve estar associado à OS")
    fun orcamentoAssociado() {
        assertThat(ordem.orcamento).isNotNull
    }

    @Entao("o orçamento deve gerar o evento {string}")
    fun deveGerarEvento(nome: String) {
        assertThat(publicados).anyMatch { it is OrcamentoGerado }
    }

    @Entao("o status da OS após o orçamento deve ser {string}")
    fun statusDeveSer(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("a reserva da peça {string} deve ser solicitada ao estoque")
    fun reservaSolicitada(peca: String) {
        verify { estoque.reservar(1L, 50L, 1) }
    }

    @Entao("o cliente deve ser notificado do orçamento gerado")
    fun clienteNotificado() {
        verify { notificacao.notificarOrcamentoGerado(1L, any(), any(), any()) }
    }

    @Entao("o orçamento não deve ser gerado")
    fun orcamentoNaoGerado() {
        assertThat(ordem.orcamento).isNull()
        assertThat(publicados).noneMatch { it is OrcamentoGerado }
    }

    @Entao("o status da OS após o orçamento deve continuar {string}")
    fun statusContinua(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    private fun statusDe(nome: String): StatusOS = when (nome.trim().lowercase()) {
        "recebida" -> StatusOS.RECEBIDA
        "em diagnóstico", "em diagnostico" -> StatusOS.EM_DIAGNOSTICO
        "aguardando aprovação", "aguardando aprovacao" -> StatusOS.AGUARDANDO_APROVACAO
        "em execução", "em execucao" -> StatusOS.EM_EXECUCAO
        "finalizada" -> StatusOS.FINALIZADA
        "entregue" -> StatusOS.ENTREGUE
        else -> error("status desconhecido: $nome")
    }

    private fun tipoDe(nome: String): TipoItem = when (nome.trim().lowercase()) {
        "serviço", "servico" -> TipoItem.SERVICO
        "peça", "peca" -> TipoItem.PECA
        else -> error("tipo desconhecido: $nome")
    }
}
