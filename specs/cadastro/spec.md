# Cadastro de clientes, veículos e serviços

**Issue:** #34
**Status:** Ready for Plan

## Overview / Problema

A oficina perde histórico de clientes e veículos e não tem um catálogo único dos serviços que oferece,
pois tudo vive em anotações e planilhas. A abertura da Ordem de Serviço (OS) e a geração do orçamento
dependem desses dados de apoio. Precisamos permitir que os usuários internos mantenham o cadastro de
**clientes**, **veículos** e **serviços**, com dados sensíveis (CPF/CNPJ e placa) validados, para que a OS
consulte informações confiáveis.

## Goals & Non-Goals

**Goals:**
- Cadastrar, consultar, listar, alterar e inativar clientes, veículos e serviços.
- Garantir documentos (CPF/CNPJ) e placas válidos e sem duplicidade.
- Permitir localizar um cliente pelo documento e um veículo pela placa.
- Preservar o histórico: nenhum cadastro já usado é apagado.

**Non-Goals:**
- Peças e insumos e controle de estoque (feature própria).
- Criação e acompanhamento da OS e do orçamento.
- Endereço, telefone, histórico de manutenção ou múltiplos contatos por cliente (o único contato é o e-mail).
- Reativação de cadastros inativos e exclusão definitiva.
- Autorização por perfil (qualquer usuário autenticado acessa os cadastros).

## User Stories

- Como **atendente**, quero cadastrar um cliente com documento, nome e e-mail, para abrir OS e notificá-lo.
- Como **atendente**, quero cadastrar um veículo (placa, marca, modelo, ano) vinculado a um cliente, para registrar o atendimento.
- Como **atendente**, quero cadastrar serviços com nome, descrição e preço, para compor orçamentos.
- Como **atendente**, quero localizar um cliente pelo documento e um veículo pela placa, para abrir a OS rapidamente.
- Como **atendente**, quero listar e detalhar cadastros, para conferir os dados.
- Como **atendente**, quero alterar dados, inclusive trocar o dono de um veículo, para manter o cadastro correto.
- Como **atendente**, quero inativar cadastros que não são mais usados, sem perder o histórico das OS.

Comportamento (BDD):
- Dado um CPF válido e ainda não cadastrado / Quando cadastro o cliente / Então ele é criado ativo.
- Dado um CPF inválido ou já cadastrado / Quando cadastro o cliente / Então é rejeitado com mensagem clara.
- Dada uma placa válida, ainda não cadastrada, e um cliente ativo / Quando cadastro o veículo / Então ele é vinculado ao cliente.
- Dada uma placa inválida ou duplicada / Quando cadastro o veículo / Então é rejeitado.
- Dado um veículo cadastrado / Quando altero o cliente dono / Então o veículo passa a pertencer ao novo cliente.
- Dado um serviço com nome já existente / Quando cadastro / Então é rejeitado.
- Dado um cadastro ativo / Quando o inativo / Então ele deixa de ser oferecido para novas OS, mas continua consultável.

## Functional Requirements

**Cliente**
- `FR-001`: O sistema MUST permitir cadastrar cliente com documento (CPF ou CNPJ), nome e e-mail.
- `FR-002`: O sistema MUST rejeitar documento ausente ou inválido (dígitos verificadores) e aceitar a máscara usual (com ou sem pontuação).
- `FR-003`: O sistema MUST impedir dois clientes com o mesmo documento, inclusive se o existente estiver inativo.
- `FR-004`: O sistema MUST rejeitar nome ausente e e-mail ausente ou em formato inválido.
- `FR-005`: O sistema MUST permitir alterar nome e e-mail; o documento MUST NOT ser alterado.
- `FR-006`: O sistema MUST permitir consultar um cliente por identificador e por documento, e listar clientes.

**Veículo**
- `FR-007`: O sistema MUST permitir cadastrar veículo com placa, marca, modelo, ano e cliente dono.
- `FR-008`: O sistema MUST rejeitar placa ausente ou inválida (formato antigo ou Mercosul), normalizando maiúsculas e separadores.
- `FR-009`: O sistema MUST impedir dois veículos com a mesma placa, inclusive se o existente estiver inativo.
- `FR-010`: O sistema MUST exigir que o dono seja um cliente existente e ativo.
- `FR-011`: O sistema MUST rejeitar marca ou modelo ausentes e ano fora de um intervalo plausível `[NEEDS CLARIFICATION: limites do ano]`.
- `FR-012`: O sistema MUST permitir alterar marca, modelo, ano e o cliente dono; a placa MUST NOT ser alterada.
- `FR-013`: O sistema MUST permitir consultar veículo por identificador e por placa, listar veículos e listar os veículos de um cliente.

**Serviço**
- `FR-014`: O sistema MUST permitir cadastrar serviço com nome, descrição e preço.
- `FR-015`: O sistema MUST impedir dois serviços com o mesmo nome (sem diferenciar maiúsculas/minúsculas), inclusive se o existente estiver inativo.
- `FR-016`: O sistema MUST rejeitar nome ausente e preço ausente ou negativo; o preço é em reais, com duas casas decimais.
- `FR-017`: O sistema MUST permitir alterar nome, descrição e preço; a alteração de preço MUST NOT afetar orçamentos já gerados.
- `FR-018`: O sistema MUST permitir consultar serviço por identificador e listar serviços.

**Inativação (comum)**
- `FR-019`: O sistema MUST permitir inativar cliente, veículo e serviço; o cadastro inativo permanece consultável e preserva o histórico.
- `FR-020`: O sistema MUST permitir restringir consultas e listagens apenas aos cadastros ativos, para que novas OS não usem cadastros inativos; as listagens MUST permitir distinguir ativos de inativos.
- `FR-021`: Inativar um cliente MUST ser negado enquanto ele possuir veículos ativos.
- `FR-022`: O sistema MUST NOT permitir alterar um cadastro inativo.

## Non-Functional Requirements

- **Segurança:** todas as operações exigem usuário autenticado; apenas usuários internos acessam os cadastros.
- **Integridade:** unicidade de documento, placa e nome de serviço garantida mesmo sob requisições simultâneas.
- **Qualidade:** regras de documento e placa cobertas por testes; cobertura mínima de 80% no domínio e na aplicação.
- **Respostas de erro:** mensagens claras e em português, indicando o campo inválido.

## Edge Cases

- Documento com máscara (`123.456.789-09`) e sem máscara têm o mesmo efeito; sequências repetidas (`111.111.111-11`) são inválidas.
- Placa em minúsculas ou com hífen (`abc-1d23`) é normalizada antes de validar e comparar.
- Cadastro simultâneo do mesmo documento, placa ou nome de serviço: apenas um é criado.
- Consulta, alteração ou inativação de registro inexistente: resposta de não encontrado.
- Veículo cadastrado para cliente inexistente ou inativo: rejeitado.
- Inativar cliente com veículos ativos: negado; inativar um cliente sem veículos ativos: permitido.
- Inativar algo já inativo não gera erro (resultado idêntico).
- Troca de dono do veículo para cliente inativo: rejeitada.
- Preço zero: aceito.

## Acceptance Criteria

- [ ] Cliente, veículo e serviço podem ser cadastrados, consultados, listados, alterados e inativados.
- [ ] CPF/CNPJ e placa inválidos são rejeitados; duplicados também.
- [ ] Veículo exige cliente ativo e permite troca de dono.
- [ ] Cliente é localizado pelo documento e veículo pela placa.
- [ ] Cadastros inativos não são oferecidos para novas OS e permanecem consultáveis.
- [ ] Inativar cliente com veículos ativos é negado.
- [ ] Todas as operações exigem autenticação.
- [ ] Testes unitários e de integração cobrem os fluxos acima, com cobertura mínima de 80%.

## Open Questions

- `[NEEDS CLARIFICATION: limites do ano do veículo]` (sugestão: de 1900 até o ano atual + 1; não bloqueia). `aceito`
- `[NEEDS CLARIFICATION: serviço com preço zero é válido?]` (assumindo que **sim**, ex.: cortesia; não bloqueia). `aceito`
- `[NEEDS CLARIFICATION: inativar um cliente deve inativar também os veículos?]` (assumindo **não**: a inativação é negada enquanto houver veículos ativos). `aceito`
- `[NEEDS CLARIFICATION: reativar cadastro inativo?]` (fora do escopo do MVP; não bloqueia). `fora de escopo`
