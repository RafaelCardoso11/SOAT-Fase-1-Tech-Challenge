# language: pt
Funcionalidade: UC12 - Consultar Status da OS
  Como cliente ou atendente
  Quero consultar o status da OS
  Para acompanhar o andamento do atendimento

  Esquema do Cenário: Consultar OS existente
    Dado que existe uma OS da placa "ABC1D23" com status "Em execução"
    Quando o <ator> consultar a OS pelo <identificador>
    Então o sistema deve exibir o status "Em execução"
    E deve exibir a linha do tempo do atendimento
    E deve exibir os detalhes de serviços e orçamento
    E nenhum evento de domínio deve ser registrado

    Exemplos:
      | ator      | identificador |
      | cliente   | número da OS  |
      | cliente   | placa         |
      | atendente | CPF           |

  Cenário: Consultar OS inexistente
    Dado que não existe OS para a placa "ZZZ9Z99"
    Quando o cliente consultar a OS pela placa "ZZZ9Z99"
    Então o sistema deve informar que nenhuma OS foi encontrada
