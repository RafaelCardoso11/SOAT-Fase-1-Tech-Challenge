# language: pt
Funcionalidade: UC01 - Criar Ordem de Serviço
  Como atendente
  Quero abrir uma OS com os dados do cliente e do veículo
  Para registrar o atendimento na oficina

  Cenário: Criar OS com dados válidos
    Dado que o atendente informou o CPF "123.456.789-09" do cliente
    E informou o veículo de placa "ABC1D23", marca "Fiat", modelo "Uno" e ano 2020
    E registrou o relato "Barulho ao frear"
    Quando o atendente solicitar a criação da OS
    Então a OS deve ser criada com status "Recebida"
    E o atendente deve receber a confirmação da criação

  Esquema do Cenário: Bloquear criação com dados inválidos
    Dado que o atendente informou o documento "<documento>" do cliente
    E informou o veículo de placa "<placa>"
    Quando o atendente solicitar a criação da OS
    Então a OS não deve ser criada
    E o sistema deve exibir a mensagem "<mensagem>"

    Exemplos:
      | documento      | placa   | mensagem                 |
      |                | ABC1D23 | CPF/CNPJ é obrigatório   |
      | 111.111.111-11 | ABC1D23 | CPF/CNPJ inválido        |
      | 123.456.789-09 |         | Placa é obrigatória      |
      | 123.456.789-09 | 12345   | Placa inválida           |
