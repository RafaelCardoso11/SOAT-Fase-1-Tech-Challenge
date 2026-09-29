# language: pt
Funcionalidade: UC04 - Reservar Peça
  Como sistema de estoque
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

  Cenário: Peça reservada não pode ser usada por outra OS
    Dado que o estoque possui 1 unidade disponível da peça "Pastilha de freio"
    E essa unidade foi reservada para a OS
    Quando o sistema tentar reservar a mesma peça para outra OS
    Então a reserva da outra OS não deve ser criada
    E o evento "Peça indisponível" deve ser registrado para a outra OS

  # Hotspot: o UC diz que peça indisponível "pode levar ao encerramento da OS
  # caso não haja alternativa de fornecimento" — regra ainda não definida.
