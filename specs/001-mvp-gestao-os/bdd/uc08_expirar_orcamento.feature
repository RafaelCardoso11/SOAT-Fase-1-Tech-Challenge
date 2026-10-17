# language: pt
Funcionalidade: UC08 - Expirar Orçamento
  Como sistema
  Quero expirar orçamentos sem resposta após o prazo
  Para que a oficina decida entre reenviar ou cancelar a OS

  Cenário: Expirar orçamento vencido sem resposta
    Dado que existe uma OS com status "Aguardando aprovação"
    E o prazo de validade do orçamento venceu
    E o cliente não respondeu ao orçamento
    Quando o sistema executar a verificação periódica de validade
    Então o orçamento deve ficar com status "Expirado"
    E o evento "Orçamento expirado" deve ser registrado
    E o atendente deve ser notificado para tratativa

  Cenário: Não expirar orçamento dentro do prazo
    Dado que existe uma OS com status "Aguardando aprovação"
    E o prazo de validade do orçamento ainda não venceu
    Quando o sistema executar a verificação periódica de validade
    Então o orçamento deve continuar com status "Aguardando aprovação"

  Cenário: Não expirar orçamento já respondido
    Dado que o prazo de validade do orçamento venceu
    Mas o cliente já aprovou o orçamento
    Quando o sistema executar a verificação periódica de validade
    Então o orçamento não deve ser expirado

  # Hotspot: prazo de validade ainda não definido.
