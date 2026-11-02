# language: pt
Funcionalidade: UC07 - Revisar Orçamento
  Como mecânico ou atendente
  Quero ajustar um orçamento cuja revisão foi solicitada pelo cliente
  Para reenviar uma nova proposta ao cliente

  @implementado
  Cenário: Revisar orçamento a pedido do cliente
    Dado uma OS aguardando aprovação com orçamento para revisão
    Quando o mecânico ajustar o orçamento para o valor total "120.00"
    Então a revisão deve registrar o evento "Orçamento revisado"
    E a nova proposta deve ser reenviada ao cliente na revisão
    E o status da OS após a revisão deve continuar "Aguardando aprovação"

  @implementado
  Cenário: Revisar orçamento trocando a peça
    Dado uma OS aguardando aprovação com orçamento para revisão
    Quando o mecânico revisar o orçamento incluindo a peça "Pastilha alternativa"
    Então a revisão deve registrar o evento "Orçamento revisado"
    E a reserva da peça "Pastilha alternativa" deve ser solicitada na revisão
    E o status da OS após a revisão deve continuar "Aguardando aprovação"

  # Hotspot: "resposta do cliente pendente de tratamento" não modelada no MVP — cenário sem @implementado.
  Cenário: Impedir envio de revisão com resposta do cliente pendente
    Dado que existe uma OS com status "Aguardando aprovação"
    E existe uma resposta do cliente pendente de tratamento
    Quando o mecânico tentar enviar o orçamento revisado
    Então o orçamento revisado não deve ser enviado

  @implementado
  Esquema do Cenário: Impedir revisão fora do status "Aguardando aprovação"
    Dado uma OS para revisão no status "<status>"
    Quando o mecânico tentar revisar o orçamento
    Então a revisão não deve ser registrada na OS

    Exemplos:
      | status      |
      | Em execução |
      | Finalizada  |
