# language: pt
@implementado
Funcionalidade: UC03 - Gerar Orçamento
  Como mecânico
  Quero montar o orçamento a partir do diagnóstico
  Para que o cliente possa aprovar o serviço

  Cenário: Gerar orçamento a partir de diagnóstico finalizado
    Dado uma OS com diagnóstico finalizado
    Quando o mecânico montar o orçamento com os itens:
      | tipo    | descrição          | valor  |
      | serviço | Troca de pastilhas | 150.00 |
      | peça    | Pastilha de freio  | 200.00 |
    Então o valor total do orçamento deve ser "350.00"
    E o orçamento deve estar associado à OS
    E o orçamento deve gerar o evento "Orçamento gerado"
    E o status da OS após o orçamento deve ser "Aguardando aprovação"
    E a reserva da peça "Pastilha de freio" deve ser solicitada ao estoque
    E o cliente deve ser notificado do orçamento gerado

  Cenário: Impedir orçamento com diagnóstico ainda não finalizado
    Dado uma OS em diagnóstico sem o diagnóstico finalizado
    Quando o mecânico tentar gerar o orçamento
    Então o orçamento não deve ser gerado
    E o status da OS após o orçamento deve continuar "Em diagnóstico"

  Esquema do Cenário: Impedir orçamento fora da etapa de diagnóstico
    Dado uma OS no status "<status>"
    Quando o mecânico tentar gerar o orçamento
    Então o orçamento não deve ser gerado
    E o status da OS após o orçamento deve continuar "<status>"

    Exemplos:
      | status      |
      | Recebida    |
      | Em execução |
      | Finalizada  |
