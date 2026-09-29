# language: pt
Funcionalidade: UC06 - Rejeitar Orçamento
  Como cliente
  Quero rejeitar o orçamento da minha OS
  Para que a oficina revise a proposta ou cancele o serviço

  Cenário: Rejeitar orçamento informando o motivo
    Dado que existe uma OS com status "Aguardando aprovação"
    Quando o cliente rejeitar o orçamento com o motivo "Valor acima do esperado"
    Então o orçamento deve ficar com status "Rejeitado"
    E o motivo "Valor acima do esperado" deve ser registrado
    E o evento "Orçamento rejeitado" deve ser registrado
    E o mecânico deve ser notificado

  Cenário: Rejeitar orçamento sem informar motivo
    Dado que existe uma OS com status "Aguardando aprovação"
    Quando o cliente rejeitar o orçamento sem informar motivo
    Então o orçamento deve ficar com status "Rejeitado"
    E o evento "Orçamento rejeitado" deve ser registrado

  Esquema do Cenário: Impedir rejeição fora do status "Aguardando aprovação"
    Dado que existe uma OS com status "<status>"
    Quando o cliente tentar rejeitar o orçamento
    Então a rejeição não deve ser registrada

    Exemplos:
      | status              |
      | Aguardando execução |
      | Em execução         |
