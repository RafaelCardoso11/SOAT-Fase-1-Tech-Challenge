# language: pt
Funcionalidade: UC07 - Revisar Orçamento
  Como mecânico ou atendente
  Quero ajustar um orçamento rejeitado
  Para reenviar uma nova proposta ao cliente

  Cenário: Revisar orçamento rejeitado
    Dado que existe uma OS com orçamento "Rejeitado" pelo motivo "Valor acima do esperado"
    Quando o mecânico ajustar o valor do serviço "Troca de pastilhas" para 120.00
    Então o evento "Orçamento revisado" deve ser registrado
    E a nova proposta deve ser reenviada ao cliente
    E o status da OS deve ser "Aguardando aprovação"

  Cenário: Impedir revisão de orçamento não rejeitado
    Dado que existe uma OS com status "Aguardando aprovação"
    Quando o mecânico tentar revisar o orçamento
    Então a revisão não deve ser registrada

  Cenário: Impedir envio de revisão com resposta do cliente pendente
    Dado que existe uma OS com orçamento "Rejeitado"
    E existe uma resposta do cliente pendente de tratamento
    Quando o mecânico tentar enviar o orçamento revisado
    Então o orçamento revisado não deve ser enviado
