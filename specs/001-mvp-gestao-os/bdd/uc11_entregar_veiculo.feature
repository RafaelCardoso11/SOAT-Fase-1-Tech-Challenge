# language: pt
Funcionalidade: UC11 - Entregar Veículo ao Cliente
  Como atendente
  Quero registrar a entrega do veículo
  Para encerrar a OS

  Cenário: Entregar veículo de OS finalizada
    Dado que existe uma OS com status "Finalizada"
    Quando o atendente confirmar a entrega do veículo
    Então o evento "Veículo entregue" deve ser registrado
    E o status da OS deve ser "Entregue"
    E o evento "OS encerrada" deve ser registrado depois de "Veículo entregue"
    E o comprovante de entrega deve ser disponibilizado

  Esquema do Cenário: Impedir entrega de OS não finalizada
    Dado que existe uma OS com status "<status>"
    Quando o atendente tentar confirmar a entrega do veículo
    Então a entrega não deve ser registrada
    E o status da OS deve continuar "<status>"

    Exemplos:
      | status               |
      | Aguardando aprovação |
      | Em execução          |
      | Entregue             |
