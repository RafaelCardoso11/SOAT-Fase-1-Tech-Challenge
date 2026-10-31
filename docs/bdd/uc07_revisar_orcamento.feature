# language: pt
Funcionalidade: UC07 - Revisar Orçamento
  Como mecânico ou atendente
  Quero ajustar um orçamento cuja revisão foi solicitada pelo cliente
  Para reenviar uma nova proposta ao cliente

  Cenário: Revisar orçamento a pedido do cliente
    Dado que existe uma OS com status "Aguardando aprovação"
    E o cliente solicitou a revisão do orçamento pelo motivo "Valor acima do esperado"
    Quando o mecânico ajustar o valor do serviço "Troca de pastilhas" para 120.00
    Então o evento "Orçamento revisado" deve ser registrado
    E a nova proposta deve ser reenviada ao cliente
    E o status da OS deve continuar "Aguardando aprovação"

  Cenário: Revisar orçamento após peça indisponível
    Dado que existe uma OS com status "Aguardando aprovação"
    E o evento "Peça indisponível" foi registrado para a peça "Pastilha de freio"
    Quando o mecânico substituir a peça por uma alternativa disponível
    Então o evento "Orçamento revisado" deve ser registrado
    E a reserva da peça alternativa deve ser solicitada ao estoque
    E o status da OS deve continuar "Aguardando aprovação"

  Cenário: Impedir envio de revisão com resposta do cliente pendente
    Dado que existe uma OS com status "Aguardando aprovação"
    E existe uma resposta do cliente pendente de tratamento
    Quando o mecânico tentar enviar o orçamento revisado
    Então o orçamento revisado não deve ser enviado

  Esquema do Cenário: Impedir revisão fora do status "Aguardando aprovação"
    Dado que existe uma OS com status "<status>"
    Quando o mecânico tentar revisar o orçamento
    Então a revisão não deve ser registrada

    Exemplos:
      | status      |
      | Em execução |
      | Finalizada  |
