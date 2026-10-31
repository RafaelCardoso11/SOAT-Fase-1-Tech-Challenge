# language: pt
@implementado
Funcionalidade: UC12 - Consultar Status da OS
  Como cliente ou atendente
  Quero consultar o status da OS
  Para acompanhar o andamento do atendimento

  Contexto:
    Dado que existe uma OS número 10, placa "ABC1D23", documento "123.456.789-09" com status "Recebida"

  Cenário: Consultar OS pelo número
    Quando o cliente consultar a OS pelo número 10
    Então o sistema deve exibir o status "Recebida"
    E deve exibir a linha do tempo do atendimento
    E nenhum evento de domínio deve ser registrado

  Cenário: Consultar OS pela placa
    Quando o cliente consultar a OS pela placa "ABC1D23"
    Então o sistema deve exibir o status "Recebida"
    E deve exibir a linha do tempo do atendimento

  Cenário: Consultar OS pelo documento
    Quando o atendente consultar a OS pelo documento "123.456.789-09"
    Então o sistema deve exibir o status "Recebida"
    E deve exibir a linha do tempo do atendimento

  Cenário: Consultar OS inexistente
    Dado que não existe OS para a placa "ZZZ9Z99"
    Quando o cliente consultar a OS pela placa "ZZZ9Z99"
    Então o sistema deve informar que nenhuma OS foi encontrada
