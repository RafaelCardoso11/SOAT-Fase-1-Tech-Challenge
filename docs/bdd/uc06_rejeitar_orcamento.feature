# language: pt
@implementado
Funcionalidade: UC06 - Rejeitar Orçamento
  Como cliente
  Quero rejeitar o orçamento da minha OS
  Para encerrar o atendimento sem execução

  Cenário: Rejeitar orçamento informando o motivo
    Dado uma OS aguardando aprovação com a peça "Pastilha de freio" reservada
    Quando o cliente rejeitar o orçamento com o motivo "Valor acima do esperado"
    Então o motivo "Valor acima do esperado" deve constar no orçamento
    E a rejeição deve registrar os eventos "Orçamento rejeitado", "OS encerrada" e "Reserva liberada"
    E o status da OS após a rejeição deve ser "Finalizada"
    E a reserva da peça "Pastilha de freio" deve ser liberada no estoque
    E a oficina deve ser notificada da rejeição

  Cenário: Rejeitar orçamento sem informar motivo
    Dado uma OS aguardando aprovação para rejeição
    Quando o cliente rejeitar o orçamento sem informar motivo
    Então a rejeição deve registrar o evento "Orçamento rejeitado"
    E o status da OS após a rejeição deve ser "Finalizada"

  Esquema do Cenário: Impedir rejeição fora do status "Aguardando aprovação"
    Dado uma OS para rejeição no status "<status>"
    Quando o cliente tentar rejeitar o orçamento
    Então a rejeição não deve ser registrada
    E o status da OS após a rejeição deve continuar "<status>"

    Exemplos:
      | status      |
      | Em execução |
      | Finalizada  |
