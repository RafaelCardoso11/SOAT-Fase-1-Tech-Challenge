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
    @Value("\${notificacao.email.url-base}") private val urlBase: String,
) : NotificacaoPort {

    override fun notificarOrcamentoGerado(ordemServicoId: Long, total: Dinheiro, tokenAprovacao: String) {
        val link = "$urlBase/api/public/orcamentos/$tokenAprovacao"
        enviar(
            destinatario,
            "Orçamento da OS #$ordemServicoId",
            "O orçamento da sua ordem de serviço #$ordemServicoId foi gerado no valor de R$ ${total.valor}.\n\n" +
                "Veja os itens em $link\n" +
                "Para aprovar, envie POST para $link/aprovar; para rejeitar, POST para $link/rejeitar.",
        )
    }

    override fun notificarOrcamentoRejeitado(ordemServicoId: Long, motivo: String?) {
        val detalhe = motivo?.let { " Motivo: $it." } ?: ""
        enviar(remetente, "Orçamento da OS #$ordemServicoId rejeitado", "O cliente rejeitou o orçamento.$detalhe")
    }

    override fun notificarOrcamentoExpirado(ordemServicoId: Long) {
        enviar(remetente, "Orçamento da OS #$ordemServicoId expirado", "O orçamento expirou sem resposta do cliente.")
    }

    override fun notificarVeiculoPronto(ordemServicoId: Long) {
        enviar(destinatario, "Veículo da OS #$ordemServicoId pronto", "O serviço foi concluído; o veículo está pronto para retirada.")
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
