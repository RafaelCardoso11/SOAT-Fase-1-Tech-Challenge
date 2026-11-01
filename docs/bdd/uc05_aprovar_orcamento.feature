# language: pt
@implementado
Funcionalidade: UC05 - Aprovar Orçamento
  Como cliente
  Quero aprovar o orçamento da minha OS
  Para autorizar a execução do serviço

  Cenário: Aprovar orçamento dentro da validade
    Dado uma OS aguardando aprovação com orçamento no prazo
    Quando o cliente aprovar o orçamento
    Então a aprovação deve ser registrada com data e hora
    E deve registrar o evento de aprovação "Orçamento aprovado"
    E o status da OS após a aprovação deve ser "Em execução"

  Cenário: Impedir aprovação de orçamento expirado
    Dado uma OS aguardando aprovação com orçamento expirado
    Quando o cliente tentar aprovar o orçamento
    Então a aprovação não deve ser registrada
    E o cliente deve ser informado de que o orçamento expirou

  Esquema do Cenário: Impedir aprovação fora do status "Aguardando aprovação"
    Dado uma OS para aprovação no status "<status>"
    Quando o cliente tentar aprovar o orçamento
    Então a aprovação não deve ser registrada
    E o status da OS após a aprovação deve continuar "<status>"

    Exemplos:
      | status         |
      | Em diagnóstico |
      | Em execução    |
      | Finalizada     |
