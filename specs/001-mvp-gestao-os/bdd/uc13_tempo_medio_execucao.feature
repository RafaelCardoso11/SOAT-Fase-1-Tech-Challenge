# language: pt
# NOVO NA V2, A CONFERIR PELO GRUPO.
# Caso de uso adicional, não derivado do Event Storming. Atende ao requisito do enunciado de
# monitorar o tempo médio de execução dos serviços. É uma consulta (não gera evento de domínio)
# e não depende de nenhum status: usa só os eventos "Execução iniciada" (UC09) e "Execução finalizada" (UC10).
Funcionalidade: UC13 - Consultar Tempo Médio de Execução
  Como atendente
  Quero consultar o tempo médio de execução dos serviços
  Para monitorar a eficiência da oficina

  Contexto:
    Dado que o atendente está autenticado nas APIs administrativas

  Cenário: Calcular o tempo médio entre início e fim da execução
    Dado que a OS "1001" registrou "Execução iniciada" em "2026-01-10 08:00" e "Execução finalizada" em "2026-01-10 10:00"
    E a OS "1002" registrou "Execução iniciada" em "2026-01-11 09:00" e "Execução finalizada" em "2026-01-11 13:00"
    Quando o atendente consultar o tempo médio de execução
    Então o sistema deve exibir o tempo médio de execução de 3 horas
    E nenhum evento de domínio deve ser registrado

  Cenário: Considerar apenas OS com execução iniciada e finalizada
    Dado que a OS "2001" registrou "Execução iniciada" em "2026-02-01 08:00" e "Execução finalizada" em "2026-02-01 09:00"
    E a OS "2002" registrou "Execução iniciada" em "2026-02-02 08:00" mas não registrou "Execução finalizada"
    Quando o atendente consultar o tempo médio de execução
    Então o cálculo deve considerar apenas a OS "2001"
    E o sistema deve exibir o tempo médio de execução de 1 hora

  Cenário: Nenhuma OS elegível para o cálculo
    Dado que não existe OS com os eventos "Execução iniciada" e "Execução finalizada" registrados
    Quando o atendente consultar o tempo médio de execução
    Então o sistema deve informar que não há dados suficientes para calcular o tempo médio
