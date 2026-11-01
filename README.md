# Sistema de Gestão de Oficina Mecânica

[![CI](https://github.com/RafaelCardoso11/SOAT-Fase-1-Tech-Challenge/actions/workflows/ci.yml/badge.svg)](https://github.com/RafaelCardoso11/SOAT-Fase-1-Tech-Challenge/actions/workflows/ci.yml)

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

Ambiente completo (app + PostgreSQL + Mailpit) via Docker:

```bash
cp .env.example .env            # ajuste os valores
docker compose -f docker/docker-compose.yml up --build
```

Localmente, a aplicação precisa de um PostgreSQL acessível (via `SPRING_DATASOURCE_*`). Suba só o banco pelo compose e rode a app:

```bash
docker compose -f docker/docker-compose.yml up -d postgres
./gradlew :bootstrap:bootRun
```

Sanidade: `curl http://localhost:8080/api/ping` → `{"aplicacao":"oficina","status":"ok"}`
e `http://localhost:8080/actuator/health` → `{"status":"UP"}`.

## Testes

```bash
./gradlew build
```

Roda testes unitários e de integração e aplica o gate de cobertura (Jacoco ≥ 80% nos módulos de domínio). Os testes de integração usam Testcontainers e sobem um PostgreSQL efêmero — exigem Docker em execução (são pulados automaticamente quando o Docker não está disponível).

Cenários BDD em [`docs/bdd/`](docs/bdd/), um arquivo `.feature` por caso de uso (UC01 a UC13).

## Documentação da API

Swagger UI: `http://localhost:8080/swagger-ui.html` · OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Análise de vulnerabilidades

Scan de dependências com Trivy:

```bash
./gradlew :bootstrap:bootJar
trivy fs --scanners vuln --severity HIGH,CRITICAL bootstrap/build/libs
```

Roda também no CI (workflow `Security`). Detalhes em [`docs/security/`](docs/security/README.md).

## Autenticação

As APIs administrativas exigem um token **JWT** no cabeçalho `Authorization: Bearer <token>`. A consulta pública de status da OS (`/api/public/**`) não exige token.

Usuários de demonstração (semeados na inicialização):

| Usuário | Senha |
|---------|-------|
| `admin` | `admin@123` |
| `mecanico` | `mecanico@123` |

Obter o token e usá-lo:

```bash
# login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"nomeUsuario":"admin","senha":"admin@123"}'
# → {"token":"<jwt>","tipo":"Bearer","expiraEmSegundos":3600}

# chamar rota administrativa com o token
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/api/v1/admin/...
```

O segredo de assinatura e a expiração vêm do ambiente (`JWT_SECRET`, `JWT_EXPIRATION_MINUTES`).

## Cadastro de clientes, veículos e serviços

APIs administrativas versionadas (`/api/v1/admin/**`), protegidas por JWT e documentadas no Swagger UI
(botão **Authorize** com o token do login). Especificação em [`specs/cadastro/`](specs/cadastro/).

| Recurso | Rotas |
|---------|-------|
| Clientes | `POST/GET /api/v1/admin/clientes` · `GET/PUT /{id}` · `GET /documento/{cpf-ou-cnpj}` · `POST /{id}/inativar` |
| Veículos | `POST/GET /api/v1/admin/veiculos` (`?clienteId=`) · `GET/PUT /{id}` · `GET /placa/{placa}` · `POST /{id}/inativar` |
| Serviços | `POST/GET /api/v1/admin/servicos` · `GET/PUT /{id}` · `POST /{id}/inativar` |

Regras principais:

- CPF/CNPJ e placa são validados e **únicos** (inclusive entre cadastros inativos); o documento do cliente e a placa do veículo não podem ser alterados.
- O veículo pertence a um cliente **ativo** e o dono pode ser trocado.
- Não há exclusão: cadastros são **inativados** (`POST /{id}/inativar`), e um cliente só é inativado sem veículos ativos.
- As listagens e buscas aceitam `?ativo=true|false`; nas buscas por chave, `?ativo=true` trata cadastro inativo como não encontrado.
- Erros: `400` dado inválido · `404` não encontrado · `409` duplicidade ou regra de negócio, com corpo `{"erro": "...", "campo": "..."}`.

Exemplo:

```bash
curl -X POST http://localhost:8080/api/v1/admin/clientes \
  -H "Authorization: Bearer <jwt>" -H 'Content-Type: application/json' \
  -d '{"documento":"123.456.789-09","nome":"Maria Silva","email":"maria@email.com"}'
```

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
