package com.fiap.soat.oficina.ordemservico.adapter.outbound.notificacao

import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "notificacao.email", name = ["enabled"], havingValue = "true")
class NotificacaoPorEmail(
    private val mailSender: JavaMailSender,
    @Value("\${notificacao.email.remetente}") private val remetente: String,
    // shortcut: e-mail real do cliente depende da integração com cadastro; usa destinatário configurável por ora.
    @Value("\${notificacao.email.destinatario-padrao}") private val destinatario: String,
) : NotificacaoPort {

    override fun notificarOrcamentoGerado(ordemServicoId: Long, total: Dinheiro) {
        enviar(
            destinatario,
            "Orçamento da OS #$ordemServicoId",
            "O orçamento da sua ordem de serviço #$ordemServicoId foi gerado no valor de R$ ${total.valor}.",
        )
    }

    override fun notificarOrcamentoRejeitado(ordemServicoId: Long, motivo: String?) {
        val detalhe = motivo?.let { " Motivo: $it." } ?: ""
        enviar(remetente, "Orçamento da OS #$ordemServicoId rejeitado", "O cliente rejeitou o orçamento.$detalhe")
    }

    private fun enviar(para: String, assunto: String, corpo: String) {
        val mensagem = SimpleMailMessage().apply {
            from = remetente
            setTo(para)
            subject = assunto
            text = corpo
        }
        mailSender.send(mensagem)
    }
}
