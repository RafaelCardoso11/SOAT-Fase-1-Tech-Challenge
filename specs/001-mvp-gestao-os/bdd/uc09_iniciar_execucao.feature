# language: pt
Funcionalidade: UC09 - Iniciar Execução da OS
  Como mecânico
  Quero iniciar os serviços aprovados
  Para executar o reparo e dar baixa nas peças reservadas

  Cenário: Iniciar execução com orçamento aprovado
    Dado que existe uma OS com status "Aguardando execução"
    E a peça "Pastilha de freio" está reservada para a OS
    Quando o mecânico iniciar a execução
    Então o status da OS deve ser "Em execução"
    E o evento "Execução iniciada" deve ser registrado
    E a baixa definitiva da peça "Pastilha de freio" deve ser feita no estoque
    E o evento "Peças baixadas" deve ser registrado

  Esquema do Cenário: Impedir execução sem orçamento aprovado
    Dado que existe uma OS com orçamento "<status_orcamento>"
    Quando o mecânico tentar iniciar a execução
    Então a execução não deve ser iniciada
    E o mecânico deve receber um alerta de orçamento não aprovado
    E nenhuma peça deve ser baixada do estoque

    Exemplos:
      | status_orcamento     |
      | Aguardando aprovação |
      | Rejeitado            |
      | Expirado             |
