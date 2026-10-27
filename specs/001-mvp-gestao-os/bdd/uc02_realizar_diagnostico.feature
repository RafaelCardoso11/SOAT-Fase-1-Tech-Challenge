# language: pt
Funcionalidade: UC02 - Realizar Diagnóstico
  Como mecânico
  Quero inspecionar o veículo e registrar o diagnóstico
  Para identificar os serviços e peças necessários

  Cenário: Iniciar diagnóstico de uma OS recebida
    Dado que existe uma OS com status "Recebida"
    Quando o mecânico assumir a OS
    Então o status da OS deve ser "Em diagnóstico"
    E o evento "Diagnóstico iniciado" deve ser registrado

  Cenário: Registrar a inspeção técnica
    Dado que existe uma OS com status "Em diagnóstico"
    Quando o mecânico registrar as observações da inspeção
    Então o evento "Diagnóstico realizado" deve ser registrado

  Cenário: Finalizar diagnóstico
    Dado que existe uma OS com status "Em diagnóstico"
    E o mecânico registrou as observações da inspeção
    Quando o mecânico concluir a análise informando serviços e peças necessários
    Então o evento "Diagnóstico finalizado" deve ser registrado
    E o status da OS deve continuar "Em diagnóstico"
    E a OS deve ficar liberada para geração de orçamento

  Cenário: Encerrar a OS quando a peça não for identificada
    Dado que existe uma OS com status "Em diagnóstico"
    Quando o mecânico concluir a inspeção sem identificar a peça necessária
    Então o evento "Diagnóstico realizado" não deve ser registrado
    E o evento "OS encerrada" deve ser registrado
    E o status da OS deve ser "Finalizada"

  Esquema do Cenário: Impedir início de diagnóstico fora do status "Recebida"
    Dado que existe uma OS com status "<status>"
    Quando o mecânico tentar iniciar o diagnóstico
    Então o diagnóstico não deve ser iniciado
    E o status da OS deve continuar "<status>"

    Exemplos:
      | status               |
      | Em diagnóstico       |
      | Aguardando aprovação |
      | Em execução          |
      | Finalizada           |
      | Entregue             |
