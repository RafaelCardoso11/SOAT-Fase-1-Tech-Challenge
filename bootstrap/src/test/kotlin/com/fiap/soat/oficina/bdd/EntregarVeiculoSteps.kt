package com.fiap.soat.oficina.bdd

import com.fiap.soat.oficina.ordemservico.application.usecase.EntregarVeiculoService
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.event.VeiculoEntregue
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.event.DomainEvent
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
import java.time.Instant
import kotlin.reflect.KClass

class EntregarVeiculoSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>()
    private val service = EntregarVeiculoService(repositorio, eventos)

    private lateinit var ordem: OrdemServico
    private val publicados = mutableListOf<DomainEvent>()
    private var resultado: OrdemServico? = null

    private fun prepara(status: StatusOS) {
        ordem = OrdemServico.reconstituir(
            id = 1L,
            documento = Documento.of("123.456.789-09"),
            veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
            relato = "Barulho ao frear",
            status = status,
            transicoes = listOf(TransicaoStatus(status, Instant.now())),
        )
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(any()) } returns 1L
        every { eventos.publicar(capture(publicados)) } just Runs
    }

    @Dado("uma OS finalizada pronta para entrega")
    fun finalizada() = prepara(StatusOS.FINALIZADA)

    @Dado("uma OS para entrega no status {string}")
    fun noStatus(status: String) = prepara(statusDe(status))

    @Quando("o atendente confirmar a entrega do veículo")
    fun confirmar() {
        resultado = service.entregar(1L)
    }

    @Quando("o atendente tentar confirmar a entrega do veículo")
    fun tentarConfirmar() {
        runCatching { service.entregar(1L) }
    }

    @Entao("a entrega deve registrar o evento {string}")
    fun registraEvento(nome: String) {
        assertThat(publicados).anyMatch { classeDe(nome).isInstance(it) }
    }

    @Entao("o status da OS após a entrega deve ser {string}")
    fun statusDeveSer(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("o status da OS após a entrega deve continuar {string}")
    fun statusContinua(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("a entrega deve registrar o evento {string} depois de {string}")
    fun eventoDepoisDe(depois: String, antes: String) {
        val idxAntes = publicados.indexOfFirst { classeDe(antes).isInstance(it) }
        val idxDepois = publicados.indexOfFirst { classeDe(depois).isInstance(it) }
        assertThat(idxAntes).isGreaterThanOrEqualTo(0)
        assertThat(idxDepois).isGreaterThan(idxAntes)
    }

    @Entao("o comprovante de entrega deve ser disponibilizado")
    fun comprovanteDisponibilizado() {
        assertThat(resultado?.status).isEqualTo(StatusOS.ENTREGUE)
    }

    @Entao("a entrega não deve ser registrada")
    fun entregaNaoRegistrada() {
        assertThat(publicados).noneMatch { it is VeiculoEntregue }
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

    private fun classeDe(nome: String): KClass<out DomainEvent> = when (nome.trim().lowercase()) {
        "veículo entregue", "veiculo entregue" -> VeiculoEntregue::class
        "os encerrada" -> OsEncerrada::class
        else -> error("evento desconhecido: $nome")
    }
}
