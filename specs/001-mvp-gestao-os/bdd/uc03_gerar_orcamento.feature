# language: pt
Funcionalidade: UC03 - Gerar Orçamento
  Como mecânico
  Quero montar o orçamento a partir do diagnóstico
  Para que o cliente possa aprovar o serviço

  Cenário: Gerar orçamento a partir de diagnóstico finalizado
    Dado que existe uma OS com status "Diagnóstico finalizado"
    Quando o mecânico montar o orçamento com os itens:
      | tipo    | descrição             | valor  |
      | serviço | Troca de pastilhas    | 150.00 |
      | peça    | Pastilha de freio     | 200.00 |
    Então o valor total do orçamento deve ser 350.00
    E o orçamento deve estar associado à OS
    E o status da OS deve ser "Aguardando aprovação"
    E o cliente deve ser notificado do orçamento gerado

  Esquema do Cenário: Impedir orçamento sem diagnóstico finalizado
    Dado que existe uma OS com status "<status>"
    Quando o mecânico tentar gerar o orçamento
    Então o orçamento não deve ser gerado
    E o status da OS deve continuar "<status>"

    Exemplos:
      | status         |
      | Recebida       |
      | Em diagnóstico |
