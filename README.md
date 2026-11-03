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

A OS também vai direto para `Finalizada` (encerrada) quando a peça não é identificada no diagnóstico, quando falta peça no estoque para o orçamento ou quando o orçamento é rejeitado ou expira.

### Casos de uso

| UC | Caso de uso | Situação |
|----|-------------|----------|
| UC01 | Criar Ordem de Serviço | ✅ |
| UC02 | Realizar Diagnóstico | ✅ |
| UC03 | Gerar Orçamento | ✅ |
| UC04 | Reservar Peça | ✅ na geração e revisão do orçamento |
| UC05 | Aprovar Orçamento | ✅ |
| UC06 | Rejeitar Orçamento | ✅ |
| UC07 | Revisar Orçamento | ✅ |
| UC08 | Expirar Orçamento | ✅ |
| UC09 | Iniciar Execução | ✅ |
| UC10 | Finalizar Execução | ✅ |
| UC11 | Entregar Veículo | ✅ |
| UC12 | Consultar Status | ✅ |
| UC13 | Tempo Médio de Execução | ✅ |

## Arquitetura

**Monolito modular** com arquitetura **hexagonal** (Ports & Adapters), um módulo Gradle por *bounded context*. É um único deployable em camadas, como pede o enunciado, mas cada contexto pode ser extraído como serviço sem reescrever o domínio.

```
Adapter IN (REST, scheduler) → Application (use cases) → Domain (agregados, VOs, eventos) → Portas OUT → Adapter OUT (JPA, SMTP, outro módulo)
```

| Módulo | Subdomínio | Responsabilidade |
|--------|-----------|------------------|
| `ordemservico` | Principal (core) | Ciclo de vida da OS, diagnóstico e orçamento |
| `cadastro` | Suporte | Clientes, veículos e serviços |
| `estoque` | Suporte | Peças e insumos, reserva, liberação e baixa |
| `autenticacao` | Genérico | Login e emissão/validação de JWT |
| `shared-kernel` | — | VOs `Documento` (CPF/CNPJ), `Placa`, `Dinheiro` e `DomainEvent` |

Documento de arquitetura: [`docs/arquitetura.md`](docs/arquitetura.md)

Diagramas C4 em [`docs/c4/`](docs/c4/) (Mermaid, importáveis no Miro):

- [Nível 1 — Contexto](docs/c4/c4-nivel1-contexto.mmd)
- [Nível 2 — Contêineres](docs/c4/c4-nivel2-conteineres.mmd)
- [Nível 3 — Componentes](docs/c4/c4-nivel3-componentes.mmd)

## Tecnologias

- Kotlin 1.9 (JVM 17) + Spring Boot 3.3 (Web, Data JPA, Security, Mail, Actuator)
- PostgreSQL 16 com migrations Flyway
- JWT (JJWT 0.12) e BCrypt
- Swagger / OpenAPI (springdoc 2.6)
- JUnit 5, MockK, AssertJ, Testcontainers, Cucumber-JVM e Jacoco
- Docker e Docker Compose (app + PostgreSQL + Mailpit)
- Trivy (scan de vulnerabilidades) e GitHub Actions

## Banco de dados

**PostgreSQL**, pelos motivos:

- **ACID**: reserva e baixa de peças e as transições de status da OS precisam de transação forte entre várias tabelas.
- **Integridade**: unicidade de CPF/CNPJ, placa e nome do item de estoque, chaves estrangeiras e `CHECK`s (saldo ≥ 0, reservado ≤ saldo).
- **Concorrência**: lock pessimista de linha no item de estoque durante reserva, entrada e baixa.
- **Isolamento por contexto**: um schema por módulo (`autenticacao`, `cadastro`, `estoque`, `ordemservico`), sem `JOIN` entre schemas; cada um pode virar o banco de um microsserviço.

As migrations ficam em `bootstrap/src/main/resources/db/migration/` e rodam na subida da aplicação.

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

Cenários BDD em [`docs/bdd/`](docs/bdd/), um arquivo `.feature` por caso de uso (UC01 a UC13). O Cucumber roda no `bootstrap` só os cenários com a tag `@implementado`.

## Documentação da API

Swagger UI: `http://localhost:8080/swagger-ui.html` · OpenAPI JSON: `http://localhost:8080/v3/api-docs`

E-mails enviados em dev (orçamento gerado, rejeitado, expirado e veículo pronto) ficam no Mailpit: `http://localhost:8025`.

## Análise de vulnerabilidades

Scan de dependências com Trivy:

```bash
./gradlew :bootstrap:bootJar
trivy rootfs --scanners vuln --severity HIGH,CRITICAL bootstrap/build/libs
```

Roda também no CI (workflow `Security`). Detalhes em [`docs/security/`](docs/security/README.md).

## Autenticação

As APIs administrativas exigem um token **JWT** no cabeçalho `Authorization: Bearer <token>`. Ficam abertas só o login (`/api/auth/**`), a consulta pública da OS (`/api/public/**`), `/api/ping`, o Swagger e `/actuator/health`.

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

## Ordem de Serviço e orçamento

Rotas administrativas em `/api/admin/ordens`, protegidas por JWT. Especificações em `specs/criar-ordem-servico/`, `specs/realizar-diagnostico/`, `specs/gerar-orcamento/`, `specs/aprovar-orcamento/`, `specs/rejeitar-orcamento/`, `specs/revisar-orcamento/`, `specs/expirar-orcamento/`, `specs/iniciar-execucao/`, `specs/finalizar-execucao/`, `specs/entregar-veiculo/` e `specs/tempo-medio-execucao/`.

| Operação | Rota |
|----------|------|
| Criar OS (UC01) | `POST /api/admin/ordens` |
| Diagnóstico (UC02) | `POST /api/admin/ordens/{id}/diagnostico/iniciar` · `/inspecao` · `/finalizar` |
| Gerar orçamento (UC03) | `POST /api/admin/ordens/{id}/orcamento` |
| Aprovar / rejeitar / revisar (UC05–UC07) | `POST /api/admin/ordens/{id}/orcamento/aprovar` · `/rejeitar` · `/revisar` |
| Expirar orçamentos vencidos (UC08) | `POST /api/admin/orcamentos/expirar` |
| Iniciar / finalizar execução (UC09, UC10) | `POST /api/admin/ordens/{id}/execucao/iniciar` · `/finalizar` com `{"todosAplicados": true}` |
| Entregar veículo (UC11) | `POST /api/admin/ordens/{id}/entrega` → comprovante |
| Tempo médio de execução (UC13) | `GET /api/admin/relatorios/tempo-medio-execucao` |

Regras principais:

- O orçamento é montado com os itens informados e o total é calculado; ele vale `ORCAMENTO_VALIDADE_DIAS` dias (padrão 7). Serviço leva `servicoId` e peça leva `itemEstoqueId` + `quantidade`: nome e preço vêm do cadastro/estoque (peça = preço × quantidade). `descricao` e `valor`, quando enviados, prevalecem (`valor` é o da linha); sem id, o item avulso exige `descricao` e `valor`. Id inexistente ou inativo responde 404; item sem preço possível, 400. Vale para gerar e revisar.
- Um job agendado (`ORCAMENTO_EXPIRACAO_CRON`, padrão de hora em hora) expira os orçamentos vencidos sem resposta; a rota `/expirar` dispara a mesma verificação manualmente.
- Ao gerar ou revisar o orçamento, as peças são reservadas no estoque; na revisão, as reservas anteriores da OS são liberadas antes de reservar as do orçamento revisado. Se alguma faltar, as reservas da OS são liberadas e a OS é encerrada.
- Rejeição e expiração encerram a OS e liberam as reservas de peça; a revisão gera um novo orçamento e reinicia o prazo.
- Ao iniciar a execução, as peças reservadas são baixadas do estoque. A finalização exige todos os itens aplicados, muda a OS para `Finalizada` e avisa o cliente que o veículo está pronto.
- A entrega só acontece com a OS `Finalizada` e devolve o comprovante (número, veículo, documento, status e data da entrega).
- O tempo médio de execução considera as OS com início e fim de execução registrados, em minutos.
- Cada transição de status é registrada com data e hora (histórico da OS).

Consulta pública (UC12), sem token:

```bash
curl http://localhost:8080/api/public/ordens/{numero}
curl "http://localhost:8080/api/public/ordens?placa=ABC1D23"
curl "http://localhost:8080/api/public/ordens?documento=12345678909"
```

## Estoque — peças e insumos

Itens de estoque (peça ou insumo) em `/api/v1/admin/estoque/itens`, protegidos por JWT e documentados no
Swagger UI. Especificação em [`specs/estoque/`](specs/estoque/).

| Operação | Rota |
|----------|------|
| Cadastrar / listar | `POST` · `GET /api/v1/admin/estoque/itens` (`?ativo=true\|false`) |
| Detalhar / alterar | `GET` · `PUT /api/v1/admin/estoque/itens/{id}` |
| Entrada de estoque | `POST /api/v1/admin/estoque/itens/{id}/entradas` com `{"quantidade": 10}` |
| Inativar | `POST /api/v1/admin/estoque/itens/{id}/inativar` |

Regras principais:

- Cada item tem nome **único** (sem diferenciar maiúsculas), descrição opcional, tipo (`PECA` ou `INSUMO`), preço e saldo inicial opcional.
- As respostas trazem `saldo` (quantidade física), `reservado` (separado para OS) e `disponivel` (saldo menos reservado).
- O saldo só aumenta por **entrada** (quantidade inteira maior que zero); alterar o item não mexe no saldo.
- Não há exclusão: itens são **inativados**; item inativo não é alterado nem recebe entrada, e a busca por id aceita `?ativo=true` para tratá-lo como não encontrado.
- A concorrência é resolvida com lock de linha no item e `CHECK`s no banco (saldo ≥ 0 e reservado ≤ saldo).
- Erros: `400` dado inválido · `404` não encontrado · `409` nome duplicado ou item inativo, com corpo `{"erro": "...", "campo": "..."}`.

Exemplo:

```bash
curl -X POST http://localhost:8080/api/v1/admin/estoque/itens \
  -H "Authorization: Bearer <jwt>" -H 'Content-Type: application/json' \
  -d '{"nome":"Pastilha de freio","tipo":"PECA","preco":200.00,"saldoInicial":10}'
```

### Estoque — reservas e baixa

Reserva de peças para uma OS, liberação e baixa, em `/api/v1/admin/estoque/ordens/{osId}`. O `osId` é o
da OS (outro módulo); o estoque não valida a existência da OS.

| Operação | Rota |
|----------|------|
| Reservar | `POST …/reservas` com `{"itemId": 1, "quantidade": 2}` → `201` ou `409` se não há disponível |
| Consultar reservas da OS | `GET …/reservas` (ativas, liberadas e consumidas) |
| Liberar | `POST …/reservas/liberar` (todas da OS) ou `?itemId=` (só a do item) → `204` |
| Baixar (início da execução) | `POST …/baixa` → `200` com as reservas consumidas |

Regras principais:

- **Reservar** separa a quantidade para a OS: sobe o `reservado` e cai o `disponivel`, sem mexer no `saldo`. Sem disponível suficiente nada é reservado (nem parcial) e a resposta é `409`.
- Uma unidade reservada para uma OS **não é prometida a outra**, mesmo com requisições simultâneas pela última unidade (lock de linha no item e `CHECK`s no banco).
- Uma OS tem no máximo **uma reserva ativa por item**; depois de liberada, pode reservar de novo.
- **Liberar** devolve ao disponível. **Baixar** diminui o `saldo` e consome as reservas ativas da OS, **tudo ou nada**. As duas operações são idempotentes.
- Item inativo não é reservado e item com reserva ativa não pode ser inativado.
- Os fatos `PecaReservada`, `PecaIndisponivel`, `ReservaLiberada` e `PecasBaixadas` são publicados como eventos de domínio.

Exemplo:

```bash
curl -X POST http://localhost:8080/api/v1/admin/estoque/ordens/7/reservas \
  -H "Authorization: Bearer <jwt>" -H 'Content-Type: application/json' \
  -d '{"itemId":1,"quantidade":2}'
```

## Estrutura do projeto

```
.
├── bootstrap/      # Aplicação Spring Boot (deployable único)
├── shared-kernel/  # VOs e base de domínio compartilhados
├── modules/        # Bounded contexts: autenticacao, cadastro, estoque, ordemservico
├── buildSrc/       # convenção de build (gate Jacoco)
├── specs/          # Spec-Driven Development: spec/plan/tasks por feature
├── docs/           # arquitetura.md, bdd/, c4/, ddd/, security/ e enunciados/ (enunciado de cada fase)
├── docker/         # Dockerfile e docker-compose
└── README.md
```

## Documentação DDD

Board do Event Storming no Miro: [Tech01](https://miro.com/app/board/uXjVHq1XvOs=/?share_link_id=344863119386) (público, sem login). Exportado e alinhado ao código em [`docs/ddd/`](docs/ddd/):

- [`event-storming.pdf`](docs/ddd/event-storming.pdf) ([fonte](docs/ddd/fontes/event-storming.md)): criação e acompanhamento da OS, gestão de peças e insumos, com eventos, comandos, políticas, atores, read models, agregados e hotspots
- [`diagramas.pdf`](docs/ddd/diagramas.pdf) ([fonte](docs/ddd/fontes/diagramas.md)): mapa de contexto, bounded contexts, agregados/entidades/VOs, ciclo de vida da OS, sequência do orçamento e C4
- [`linguagem-ubiqua.md`](docs/ddd/linguagem-ubiqua.md): linguagem ubíqua, glossário e relação entre documentação e código
- [`fontes/`](docs/ddd/fontes/): fontes editáveis dos PDFs e o [mapa de contexto](docs/ddd/fontes/mapa-de-contexto.jpg) original

Os PDFs são gerados das fontes `.md` com `python3 docs/ddd/fontes/gerar-pdfs.py` (Google Chrome).

## Vídeo da entrega

_Link do vídeo_
