# language: pt
@implementado
Funcionalidade: UC09 - Iniciar Execução da OS
  Como mecânico
  Quero iniciar os serviços aprovados
  Para executar o reparo e dar baixa nas peças reservadas

  Cenário: Iniciar execução com orçamento aprovado
    Dado uma OS em execução pronta para iniciar
    Quando o mecânico iniciar a execução
    Então a execução deve registrar os eventos "Execução iniciada" e "Peças baixadas"
    E o status da OS após iniciar deve continuar "Em execução"
    E a baixa das peças deve ser feita no estoque

  Esquema do Cenário: Impedir execução sem orçamento aprovado
    Dado uma OS para execução no status "<status>"
    Quando o mecânico tentar iniciar a execução
    Então a execução não deve ser iniciada
    E nenhuma peça deve ser baixada do estoque

    Exemplos:
      | status               |
      | Em diagnóstico       |
      | Aguardando aprovação |
      | Finalizada           |
