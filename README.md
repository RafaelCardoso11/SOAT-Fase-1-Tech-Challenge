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

Documento de arquitetura: [`docs/arquitetura.md`](docs/arquitetura.md)

Diagramas C4 em [`docs/c4/`](docs/c4/):

- [Nível 1 — Contexto](docs/c4/c4-nivel1-contexto.mmd)
- [Nível 2 — Contêineres](docs/c4/c4-nivel2-conteineres.mmd)
- [Nível 3 — Componentes](docs/c4/c4-nivel3-componentes.mmd)

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

Ambiente completo (app + PostgreSQL + MailHog) via Docker:

```bash
cp .env.example .env            # ajuste os valores
docker compose -f docker/docker-compose.yml up --build
```

Ou localmente (sobe só a aplicação, sem banco):

```bash
./gradlew :bootstrap:bootRun
```

Sanidade: `curl http://localhost:8080/api/ping` → `{"aplicacao":"oficina","status":"ok"}`
e `http://localhost:8080/actuator/health` → `{"status":"UP"}`.

## Testes

_A definir: comando para rodar os testes e gerar a cobertura._

Cenários BDD em [`specs/001-mvp-gestao-os/bdd/`](specs/001-mvp-gestao-os/bdd/), um arquivo `.feature` por caso de uso (UC01 a UC13).

## Documentação da API

Swagger UI: `http://localhost:8080/swagger-ui.html` · OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Autenticação

_A definir: usuários e credenciais de demonstração ou procedimento para obter o token JWT._

## Estrutura do projeto

```
.
├── bootstrap/      # Aplicação Spring Boot (deployable único)
├── shared-kernel/  # VOs e base de domínio compartilhados
├── modules/        # Bounded contexts: autenticacao, cadastro, estoque, ordemservico
├── specs/          # Spec-Driven Development (spec/plan/tasks) + cenários BDD
├── docs/           # arquitetura.md, adr/, ddd/, c4/ e enunciado
├── docker/         # Dockerfile e docker-compose
└── README.md
```

## Documentação DDD

- Event Storming: _link do Miro_
- Fluxograma do Event Storming: [`docs/ddd/fluxograma-oficina.mmd`](docs/ddd/fluxograma-oficina.mmd)
- Mapa de contexto: [`docs/ddd/mapa-de-contexto.jpg`](docs/ddd/mapa-de-contexto.jpg)

## Vídeo da entrega

_Link do vídeo_
