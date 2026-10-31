# language: pt
Funcionalidade: UC04 - Reservar Peça
  Como sistema (módulo de Estoque)
  Quero reservar as peças do orçamento
  Para garantir disponibilidade até a execução

  Contexto:
    Dado que existe uma OS com orçamento em elaboração que exige a peça "Pastilha de freio"

  Cenário: Reservar peça disponível
    Dado que o estoque possui 3 unidades disponíveis da peça "Pastilha de freio"
    Quando o sistema reservar 1 unidade da peça para a OS
    Então o evento "Peça reservada" deve ser registrado
    E a reserva deve estar vinculada à OS
    E o estoque deve ter 2 unidades disponíveis da peça "Pastilha de freio"

  Cenário: Peça indisponível no estoque
    Dado que o estoque possui 0 unidades disponíveis da peça "Pastilha de freio"
    Quando o sistema tentar reservar 1 unidade da peça para a OS
    Então a reserva não deve ser criada
    E o evento "Peça indisponível" deve ser registrado
    E o atendente e o mecânico devem ser notificados
    E o orçamento deve ficar disponível para revisão

  Cenário: Encerrar a OS quando não há alternativa de fornecimento
    Dado que o evento "Peça indisponível" foi registrado para a OS
    E não há alternativa de fornecimento para a peça "Pastilha de freio"
    Quando o atendente encerrar a OS
    Então o evento "OS encerrada" deve ser registrado
    E o status da OS deve ser "Finalizada"
    E as demais peças reservadas para a OS devem ser liberadas

  Cenário: Peça reservada não pode ser usada por outra OS
    Dado que o estoque possui 1 unidade disponível da peça "Pastilha de freio"
    E essa unidade foi reservada para a OS
    Quando o sistema tentar reservar a mesma peça para outra OS
    Então a reserva da outra OS não deve ser criada
    E o evento "Peça indisponível" deve ser registrado para a outra OS
