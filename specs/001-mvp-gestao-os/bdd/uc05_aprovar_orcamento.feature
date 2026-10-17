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
    E o status da OS deve ser "Aguardando execução"

  Cenário: Impedir aprovação de orçamento expirado
    Dado que existe uma OS com orçamento "Expirado"
    Quando o cliente tentar aprovar o orçamento
    Então a aprovação não deve ser registrada
    E o cliente deve ser informado de que o orçamento expirou

  Cenário: Impedir aprovação fora do status "Aguardando aprovação"
    Dado que existe uma OS com status "Diagnóstico finalizado"
    Quando o cliente tentar aprovar o orçamento
    Então a aprovação não deve ser registrada
