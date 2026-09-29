# language: pt
Funcionalidade: UC10 - Finalizar Execução da OS
  Como mecânico
  Quero registrar a conclusão dos serviços
  Para liberar o veículo para retirada

  Cenário: Finalizar execução com todos os itens aplicados
    Dado que existe uma OS com status "Em execução"
    E todos os serviços e peças do orçamento foram aplicados
    Quando o mecânico registrar a conclusão
    Então o status da OS deve ser "Finalizada"
    E o evento "Execução finalizada" deve ser registrado
    E o cliente deve ser notificado de que o veículo está pronto para retirada

  Cenário: Impedir finalização com itens pendentes
    Dado que existe uma OS com status "Em execução"
    E o serviço "Troca de pastilhas" ainda não foi aplicado
    Quando o mecânico tentar registrar a conclusão
    Então a OS deve continuar com status "Em execução"
    E o mecânico deve ser informado dos itens pendentes

  Cenário: Impedir finalização de OS que não está em execução
    Dado que existe uma OS com status "Aguardando execução"
    Quando o mecânico tentar registrar a conclusão
    Então a OS deve continuar com status "Aguardando execução"
