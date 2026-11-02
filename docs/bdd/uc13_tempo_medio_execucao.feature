# language: pt
# Caso de uso adicional (requisito do enunciado: monitorar o tempo médio de execução).
# É uma consulta (não gera evento de domínio); usa os instantes de início/fim da execução.
@implementado
Funcionalidade: UC13 - Consultar Tempo Médio de Execução
  Como atendente
  Quero consultar o tempo médio de execução dos serviços
  Para monitorar a eficiência da oficina

  Cenário: Calcular o tempo médio entre início e fim da execução
    Dado a OS "1001" executou de "2026-01-10 08:00" a "2026-01-10 10:00"
    E a OS "1002" executou de "2026-01-11 09:00" a "2026-01-11 13:00"
    Quando o atendente consultar o tempo médio de execução
    Então o tempo médio de execução deve ser de 180 minutos

  Cenário: Considerar apenas OS com execução iniciada e finalizada
    Dado a OS "2001" executou de "2026-02-01 08:00" a "2026-02-01 09:00"
    E a OS "2002" iniciou a execução em "2026-02-02 08:00" mas não finalizou
    Quando o atendente consultar o tempo médio de execução
    Então o tempo médio de execução deve ser de 60 minutos

  Cenário: Nenhuma OS elegível para o cálculo
    Dado que nenhuma OS tem execução iniciada e finalizada
    Quando o atendente consultar o tempo médio de execução
    Então o sistema deve informar que não há dados suficientes
