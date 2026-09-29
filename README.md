# Sistema de Gestão de Oficina Mecânica

Tech Challenge — Fase 1 (FIAP SOAT)

## Objetivo

Substituir o controle manual (anotações e planilhas) de uma oficina mecânica de médio porte por um sistema que organize o atendimento, o diagnóstico, o orçamento, a execução e a entrega dos veículos, com controle de peças e acompanhamento do status da Ordem de Serviço (OS).

## Solução

API REST para a gestão de Ordens de Serviço:

- Abertura da OS com dados do cliente e do veículo
- Diagnóstico, orçamento e reserva de peças
- Aprovação, rejeição, revisão e expiração de orçamentos pelo cliente
- Execução com baixa de peças no estoque e entrega do veículo
- Consulta do status da OS pelo cliente
- CRUD de clientes, veículos, serviços e peças (APIs administrativas protegidas por JWT)

Status da OS: `Recebida` → `Em diagnóstico` → `Aguardando aprovação` → `Em execução` → `Finalizada` → `Entregue`

## Arquitetura

Monolito em camadas, rodando em Docker:

```
Controllers → Use Cases → Domain (Entities / Domain Services) → Repositories → Banco de Dados
```

Diagramas C4 em [`c4/`](c4/):

- [Nível 1 — Contexto](c4/c4-nivel1-contexto.mmd)
- [Nível 2 — Contêineres](c4/c4-nivel2-conteineres.mmd)
- [Nível 3 — Componentes](c4/c4-nivel3-componentes.mmd)

## Tecnologias

- Linguagem/framework: _a definir_
- Banco de dados: _a definir_
- Docker e Docker Compose
- Swagger / OpenAPI
- JWT

## Banco de dados

_A definir: banco escolhido e justificativa._

## Pré-requisitos

- Docker
- Docker Compose

## Execução

```bash
docker compose up --build
```

## Testes

_A definir: comando para rodar os testes e gerar a cobertura._

Cenários BDD em [`bdd/`](bdd/), um arquivo `.feature` por caso de uso (UC01 a UC12).

## Documentação da API

Swagger disponível em: _a definir (ex.: `http://localhost:8080/swagger-ui`)_

## Autenticação

_A definir: usuários e credenciais de demonstração ou procedimento para obter o token JWT._

## Estrutura do projeto

```
.
├── bdd/     # Cenários BDD (Gherkin) dos casos de uso
├── c4/      # Diagramas C4 (Mermaid)
├── docs/    # Enunciado do desafio e fluxograma do Event Storming
└── README.md
```

## Documentação DDD

- Event Storming: _link do Miro_
- Fluxograma do Event Storming: [`docs/fluxograma-oficina.mmd`](docs/fluxograma-oficina.mmd)

## Vídeo da entrega

_Link do vídeo_
