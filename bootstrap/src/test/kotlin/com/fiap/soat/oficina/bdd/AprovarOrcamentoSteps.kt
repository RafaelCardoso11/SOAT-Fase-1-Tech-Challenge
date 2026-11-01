package com.fiap.soat.oficina.bdd

import com.fiap.soat.oficina.ordemservico.application.usecase.AprovarOrcamentoService
import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoAprovado
import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.Orcamento
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.event.DomainEvent
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.cucumber.java.pt.Dado
import io.cucumber.java.pt.Entao
import io.cucumber.java.pt.Quando
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.Instant

class AprovarOrcamentoSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>()
    private val service = AprovarOrcamentoService(repositorio, eventos)

    private lateinit var ordem: OrdemServico
    private val publicados = mutableListOf<DomainEvent>()
    private var erro: Throwable? = null

    private fun prepara(status: StatusOS, orcamento: Orcamento?) {
        ordem = OrdemServico.reconstituir(
            id = 1L,
            documento = Documento.of("123.456.789-09"),
            veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
            relato = "Barulho ao frear",
            status = status,
            transicoes = listOf(TransicaoStatus(status, Instant.now())),
            liberadoParaOrcamento = true,
            orcamento = orcamento,
        )
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(any()) } returns 1L
        every { eventos.publicar(capture(publicados)) } just Runs
    }

    private fun orcamento(validoAte: Instant) = Orcamento(
        listOf(ItemOrcamento(TipoItem.SERVICO, "Troca de pastilhas", Dinheiro.of(BigDecimal("150.00")))),
        validoAte = validoAte,
    )

    @Dado("uma OS aguardando aprovação com orçamento no prazo")
    fun aguardandoNoPrazo() =
        prepara(StatusOS.AGUARDANDO_APROVACAO, orcamento(Instant.now().plusSeconds(3600)))

    @Dado("uma OS aguardando aprovação com orçamento expirado")
    fun aguardandoExpirado() =
        prepara(StatusOS.AGUARDANDO_APROVACAO, orcamento(Instant.now().minusSeconds(3600)))

    @Dado("uma OS para aprovação no status {string}")
    fun noStatus(status: String) = prepara(statusDe(status), orcamento = null)

    @Quando("o cliente aprovar o orçamento")
    fun aprovar() {
        service.aprovar(1L)
    }

    @Quando("o cliente tentar aprovar o orçamento")
    fun tentarAprovar() {
        erro = runCatching { service.aprovar(1L) }.exceptionOrNull()
    }

    @Entao("a aprovação deve ser registrada com data e hora")
    fun aprovacaoRegistrada() {
        assertThat(ordem.orcamento!!.aprovadoEm).isNotNull
    }

    @Entao("deve registrar o evento de aprovação {string}")
    fun deveRegistrarEvento(nome: String) {
        assertThat(publicados).anyMatch { it is OrcamentoAprovado }
    }

    @Entao("o status da OS após a aprovação deve ser {string}")
    fun statusDeveSer(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("a aprovação não deve ser registrada")
    fun aprovacaoNaoRegistrada() {
        assertThat(ordem.orcamento?.aprovadoEm).isNull()
        assertThat(publicados).noneMatch { it is OrcamentoAprovado }
    }

    @Entao("o cliente deve ser informado de que o orçamento expirou")
    fun informadoExpirado() {
        assertThat(erro).isNotNull
        assertThat(erro!!.message).contains("expirado")
    }

    @Entao("o status da OS após a aprovação deve continuar {string}")
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
}
