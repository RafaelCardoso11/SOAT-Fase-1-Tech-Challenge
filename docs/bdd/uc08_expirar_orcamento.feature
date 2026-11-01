# language: pt
@implementado
Funcionalidade: UC08 - Expirar Orçamento
  Como sistema
  Quero expirar orçamentos sem resposta após o prazo
  Para encerrar a OS e liberar as peças reservadas

  Cenário: Expirar orçamento vencido sem resposta
    Dado uma OS aguardando aprovação com orçamento vencido e a peça "Pastilha de freio" reservada
    Quando o sistema executar a verificação de validade
    Então a expiração deve registrar os eventos "Orçamento expirado" e "OS encerrada"
    E o status da OS após a expiração deve ser "Finalizada"
    E a reserva da peça "Pastilha de freio" deve ser liberada na expiração
    E o atendente deve ser notificado da expiração

  Cenário: Não expirar orçamento dentro do prazo
    Dado uma OS aguardando aprovação com orçamento no prazo para expiração
    Quando o sistema executar a verificação de validade
    Então a expiração não deve registrar o evento "Orçamento expirado"
    E o status da OS após a expiração deve continuar "Aguardando aprovação"

  Cenário: Não expirar orçamento já aprovado
    Dado uma OS já em execução com orçamento vencido
    Quando o sistema executar a verificação de validade
    Então a expiração não deve registrar o evento "Orçamento expirado"
    E o status da OS após a expiração deve continuar "Em execução"
