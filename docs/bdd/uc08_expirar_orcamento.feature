# language: pt
Funcionalidade: UC08 - Expirar Orçamento
  Como sistema
  Quero expirar orçamentos sem resposta após o prazo
  Para encerrar a OS e liberar as peças reservadas

  Cenário: Expirar orçamento vencido sem resposta
    Dado que existe uma OS com status "Aguardando aprovação"
    E a peça "Pastilha de freio" está reservada para a OS
    E o prazo de validade do orçamento venceu
    E o cliente não respondeu ao orçamento
    Quando o sistema executar a verificação periódica de validade
    Então o evento "Orçamento expirado" deve ser registrado
    E o evento "OS encerrada" deve ser registrado
    E o status da OS deve ser "Finalizada"
    E a reserva da peça "Pastilha de freio" deve ser liberada
    E o atendente deve ser notificado

  Cenário: Não expirar orçamento dentro do prazo
    Dado que existe uma OS com status "Aguardando aprovação"
    E o prazo de validade do orçamento ainda não venceu
    Quando o sistema executar a verificação periódica de validade
    Então o evento "Orçamento expirado" não deve ser registrado
    E o status da OS deve continuar "Aguardando aprovação"

  Cenário: Não expirar orçamento já aprovado
    Dado que o prazo de validade do orçamento venceu
    Mas o cliente já aprovou o orçamento
    E a OS está com status "Em execução"
    Quando o sistema executar a verificação periódica de validade
    Então o evento "Orçamento expirado" não deve ser registrado
    E o status da OS deve continuar "Em execução"

  # Hotspot: prazo de validade ainda não definido.
