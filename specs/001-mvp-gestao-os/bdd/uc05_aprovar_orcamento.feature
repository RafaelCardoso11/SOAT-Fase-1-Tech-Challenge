# language: pt
Funcionalidade: UC05 - Aprovar Orçamento
  Como cliente
  Quero aprovar o orçamento da minha OS
  Para autorizar a execução do serviço

  Cenário: Aprovar orçamento dentro da validade
    Dado que existe uma OS com status "Aguardando aprovação"
    E o orçamento está dentro do prazo de validade
    Quando o cliente aprovar o orçamento
    Então a aprovação deve ser registrada com data e hora
    E o evento "Orçamento aprovado" deve ser registrado
    E o status da OS deve ser "Em execução"

  Cenário: Impedir aprovação de orçamento expirado
    Dado que existe uma OS cujo orçamento expirou
    Quando o cliente tentar aprovar o orçamento
    Então a aprovação não deve ser registrada
    E o cliente deve ser informado de que o orçamento expirou

  Esquema do Cenário: Impedir aprovação fora do status "Aguardando aprovação"
    Dado que existe uma OS com status "<status>"
    Quando o cliente tentar aprovar o orçamento
    Então a aprovação não deve ser registrada
    E o status da OS deve continuar "<status>"

    Exemplos:
      | status         |
      | Em diagnóstico |
      | Em execução    |
      | Finalizada     |
