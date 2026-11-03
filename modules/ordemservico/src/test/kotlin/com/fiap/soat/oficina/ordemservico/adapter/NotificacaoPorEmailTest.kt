package com.fiap.soat.oficina.ordemservico.adapter

import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Cliente
import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarClienteUseCase
import com.fiap.soat.oficina.ordemservico.adapter.outbound.notificacao.NotificacaoPorEmail
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import com.fiap.soat.oficina.shared.domain.vo.Documento
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.math.BigDecimal

class NotificacaoPorEmailTest {

    private val mailSender = mockk<JavaMailSender>(relaxed = true)
    private val clientes = mockk<ConsultarClienteUseCase>()
    private val adapter = NotificacaoPorEmail(mailSender, "oficina@exemplo.com", "padrao@exemplo.com", clientes)
    private val documento = Documento.of("12345678909")
    private val mensagem = slot<SimpleMailMessage>()

    @BeforeEach
    fun requisicao() {
        val request = MockHttpServletRequest().apply {
            scheme = "https"
            serverName = "oficina.exemplo.com"
            serverPort = 443
        }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    @AfterEach
    fun limpar() = RequestContextHolder.resetRequestAttributes()

    private fun clienteCadastrado() {
        every { clientes.buscarPorDocumento("12345678909") } returns
            Cliente.criar("12345678909", "Maria", "maria@email.com")
    }

    @Test
    fun `orcamento vai para o cliente com o link do endereco da requisicao`() {
        clienteCadastrado()

        adapter.notificarOrcamentoGerado(10L, documento, Dinheiro.of(BigDecimal("350.00")), "tok-123")

        verify { mailSender.send(capture(mensagem)) }
        assertThat(mensagem.captured.from).isEqualTo("oficina@exemplo.com")
        assertThat(mensagem.captured.to).containsExactly("maria@email.com")
        assertThat(mensagem.captured.subject).contains("#10")
        assertThat(mensagem.captured.text).contains("#10").contains("350.00")
            .contains("https://oficina.exemplo.com/api/public/orcamentos/tok-123/aprovar")
            .contains("https://oficina.exemplo.com/api/public/orcamentos/tok-123/rejeitar")
    }

    @Test
    fun `sem cliente cadastrado usa o destinatario padrao`() {
        every { clientes.buscarPorDocumento("12345678909") } throws RegistroNaoEncontradoException("Cliente não encontrado")

        adapter.notificarOrcamentoGerado(10L, documento, Dinheiro.of(BigDecimal("350.00")), "tok-123")

        verify { mailSender.send(capture(mensagem)) }
        assertThat(mensagem.captured.to).containsExactly("padrao@exemplo.com")
    }

    @Test
    fun `veiculo pronto vai para o cliente`() {
        clienteCadastrado()

        adapter.notificarVeiculoPronto(10L, documento)

        verify { mailSender.send(capture(mensagem)) }
        assertThat(mensagem.captured.to).containsExactly("maria@email.com")
    }
}
