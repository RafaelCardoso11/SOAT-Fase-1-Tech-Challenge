# language: pt
Funcionalidade: UC06 - Rejeitar Orçamento
  Como cliente
  Quero rejeitar o orçamento da minha OS
  Para encerrar o atendimento sem execução

  Cenário: Rejeitar orçamento informando o motivo
    Dado que existe uma OS com status "Aguardando aprovação"
    E a peça "Pastilha de freio" está reservada para a OS
    Quando o cliente rejeitar o orçamento com o motivo "Valor acima do esperado"
    Então o motivo "Valor acima do esperado" deve ser registrado
    E o evento "Orçamento rejeitado" deve ser registrado
    E o evento "OS encerrada" deve ser registrado
    E o status da OS deve ser "Finalizada"
    E a reserva da peça "Pastilha de freio" deve ser liberada
    E o evento "Reserva liberada" deve ser registrado
    E a oficina e o mecânico devem ser notificados

  Cenário: Rejeitar orçamento sem informar motivo
    Dado que existe uma OS com status "Aguardando aprovação"
    Quando o cliente rejeitar o orçamento sem informar motivo
    Então o evento "Orçamento rejeitado" deve ser registrado
    E o status da OS deve ser "Finalizada"

  Esquema do Cenário: Impedir rejeição fora do status "Aguardando aprovação"
    Dado que existe uma OS com status "<status>"
    Quando o cliente tentar rejeitar o orçamento
    Então a rejeição não deve ser registrada
    E o status da OS deve continuar "<status>"

    Exemplos:
      | status      |
      | Em execução |
      | Finalizada  |
