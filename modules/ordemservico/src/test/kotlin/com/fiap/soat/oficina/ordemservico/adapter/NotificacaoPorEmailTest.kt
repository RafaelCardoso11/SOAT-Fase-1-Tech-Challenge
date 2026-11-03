package com.fiap.soat.oficina.ordemservico.adapter

import com.fiap.soat.oficina.ordemservico.adapter.outbound.notificacao.NotificacaoPorEmail
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import java.math.BigDecimal

class NotificacaoPorEmailTest {

    private val mailSender = mockk<JavaMailSender>(relaxed = true)
    private val adapter = NotificacaoPorEmail(mailSender, "oficina@exemplo.com", "cliente@exemplo.com", "http://localhost:8080")

    @Test
    fun `envia email com remetente, destinatario e corpo do orcamento`() {
        val mensagem = slot<SimpleMailMessage>()

        adapter.notificarOrcamentoGerado(10L, Dinheiro.of(BigDecimal("350.00")), "tok-123")

        verify { mailSender.send(capture(mensagem)) }
        assertThat(mensagem.captured.from).isEqualTo("oficina@exemplo.com")
        assertThat(mensagem.captured.to).containsExactly("cliente@exemplo.com")
        assertThat(mensagem.captured.subject).contains("#10")
        assertThat(mensagem.captured.text).contains("#10").contains("350.00")
            .contains("http://localhost:8080/api/public/orcamentos/tok-123/aprovar")
            .contains("http://localhost:8080/api/public/orcamentos/tok-123/rejeitar")
    }
}
