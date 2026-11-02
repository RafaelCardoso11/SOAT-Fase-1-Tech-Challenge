# language: pt
@implementado
Funcionalidade: UC11 - Entregar Veículo ao Cliente
  Como atendente
  Quero registrar a entrega do veículo
  Para encerrar a OS

  Cenário: Entregar veículo de OS finalizada
    Dado uma OS finalizada pronta para entrega
    Quando o atendente confirmar a entrega do veículo
    Então a entrega deve registrar o evento "Veículo entregue"
    E o status da OS após a entrega deve ser "Entregue"
    E a entrega deve registrar o evento "OS encerrada" depois de "Veículo entregue"
    E o comprovante de entrega deve ser disponibilizado

  Esquema do Cenário: Impedir entrega de OS não finalizada
    Dado uma OS para entrega no status "<status>"
    Quando o atendente tentar confirmar a entrega do veículo
    Então a entrega não deve ser registrada
    E o status da OS após a entrega deve continuar "<status>"

    Exemplos:
      | status               |
      | Aguardando aprovação |
      | Em execução          |
      | Entregue             |
