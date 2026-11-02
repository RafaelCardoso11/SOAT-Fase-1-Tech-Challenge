# language: pt
@implementado
Funcionalidade: UC10 - Finalizar Execução da OS
  Como mecânico
  Quero registrar a conclusão dos serviços
  Para liberar o veículo para retirada

  Cenário: Finalizar execução com todos os itens aplicados
    Dado uma OS em execução com todos os itens aplicados
    Quando o mecânico registrar a conclusão
    Então a finalização deve registrar o evento "Execução finalizada"
    E o status da OS após finalizar deve ser "Finalizada"
    E o cliente deve ser notificado de que o veículo está pronto

  Cenário: Impedir finalização com itens pendentes
    Dado uma OS em execução com itens pendentes
    Quando o mecânico tentar registrar a conclusão
    Então o status da OS após finalizar deve continuar "Em execução"
    E o mecânico deve ser informado de que há itens pendentes

  Cenário: Impedir finalização de OS que não está em execução
    Dado uma OS para finalizar no status "Aguardando aprovação"
    Quando o mecânico tentar registrar a conclusão
    Então o status da OS após finalizar deve continuar "Aguardando aprovação"
