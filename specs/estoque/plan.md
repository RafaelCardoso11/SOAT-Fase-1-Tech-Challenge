# Technical Plan: Estoque de peças e insumos

**Spec Ref:** `specs/estoque/spec.md`
**Issue:** #58
**Status:** Draft

## Summary

Implementar `modules/estoque` no mesmo desenho hexagonal de `cadastro` e `ordemservico`. Dois agregados
no módulo: `ItemEstoque` (raiz: nome, descrição, tipo, preço, saldo, reservado, ativo) e `Reserva` (OS, item,
quantidade, situação). As regras de saldo, reserva, liberação e baixa vivem no domínio; a concorrência é
resolvida com **lock pessimista de linha** no item (`SELECT … FOR UPDATE`), reforçada por `CHECK` no banco
(saldo ≥ 0, reservado ≥ 0, reservado ≤ saldo) e por um índice único parcial (uma reserva ativa por OS e item).
A reserva sem disponibilidade **não é exceção**: o caso de uso devolve um `ResultadoReserva` (reservada ou
indisponível), para a OS tratar o fluxo. Os quatro fatos do Event Storming saem por uma porta de saída
`PublicadorDeEventos`, no padrão do `ordemservico` (adapter sobre o `ApplicationEventPublisher`). Rotas
versionadas em `/api/v1/admin/estoque/**`, protegidas por JWT e documentadas no Swagger, sem mudança em segurança.

## Requirement Mapping

Pacote raiz: `com.fiap.soat.oficina.estoque` (abreviado `…estoque`), em `modules/estoque/src/main/kotlin/`.

| Spec Ref | Componente / Camada | Nota de Implementação |
|---|---|---|
| `FR-001` | `domain/model/ItemEstoque.kt`, `GerenciarItemService.cadastrar` | `ItemEstoque.criar(nome, descricao, tipo, preco, saldoInicial)`; saldo inicial padrão 0 |
| `FR-002` | `ItemEstoque.criar/alterar` + `TipoItem` + VO `Dinheiro` | Nome em branco, tipo inválido, preço ausente/negativo e saldo inicial negativo → `DadoInvalidoException(campo)` |
| `FR-003` | `ItemEstoqueRepository.existePorNome` + índice único `lower(nome)` | Checagem na aplicação (mensagem clara) e constraint (concorrência) → `DuplicidadeException` |
| `FR-004` | `ItemEstoque.alterar(nome, descricao, tipo, preco)` | Saldo e reservado fora da assinatura; DTO de alteração não os recebe |
| `FR-005` | `ConsultarItemService` (`buscarPorId`, `listar`) | Filtro `ativo`; `somenteAtivos` nas buscas por chave (mesmo contrato do `cadastro`) |
| `FR-006` | `ItemEstoque.disponivel` e DTO de resposta | Resposta traz `saldo`, `reservado` e `disponivel = saldo − reservado` |
| `FR-007` | `ItemEstoque.inativar()` + `POST …/itens/{id}/inativar` | Idempotente; sem exclusão física |
| `FR-008` | `GerenciarItemService.inativar` | `reservado > 0` → `ConflitoException` (não precisa consultar a tabela de reservas) |
| `FR-009` | `alterar/registrarEntrada/reservar` do modelo; `somenteAtivos` | Item inativo lança `ConflitoException`; as buscas aceitam `?ativo=true` |
| `FR-010` | `ItemEstoque.registrarEntrada(Quantidade)`, `GerenciarItemService.registrarEntrada`, `POST …/itens/{id}/entradas` | Item carregado com lock; soma ao saldo |
| `FR-011` | Modelo: nenhuma operação além de `baixar` reduz `saldo` | Garantido por testes de domínio e `CHECK (saldo >= 0)` |
| `FR-012` | `ReservarPecaService.reservar(osId, itemId, quantidade)` | Item carregado com lock; valida disponível |
| `FR-013` | `ItemEstoque.reservar(q)` + `Reserva.criar` + evento `PecaReservada` | Soma ao `reservado`; saldo intacto; cria `Reserva` ativa vinculada à OS |
| `FR-014` | `ResultadoReserva.Indisponivel` + evento `PecaIndisponivel` | Nenhuma reserva (nem parcial); REST responde `409` com a mensagem de indisponibilidade |
| `FR-015` | Lock pessimista no item + `CHECK (reservado <= saldo)` | Serializa as disputas; teste de integração com várias threads na última unidade |
| `FR-016` | `ReservaRepository.buscarAtivaPorOsEItem` + índice único parcial | Já existe ativa → `ConflitoException`; a constraint cobre a corrida |
| `FR-017` | `ReservarPecaService` | Item inexistente → `RegistroNaoEncontradoException`; inativo → `ConflitoException` |
| `FR-018` | `LiberarReservaService.liberar(osId, itemId?)`, `POST …/ordens/{osId}/reservas/liberar` | `itemId` opcional; subtrai do `reservado`; evento `ReservaLiberada` por reserva |
| `FR-019` | `LiberarReservaService` | Sem reserva ativa → retorna sem efeito |
| `FR-020` | `BaixarPecasService.baixar(osId)`, `POST …/ordens/{osId}/baixa` | Reservas ativas ordenadas por `itemId`, itens bloqueados nessa ordem; `saldo -= q`, `reservado -= q`; reservas viram consumidas; evento `PecasBaixadas`; transação única (tudo ou nada) |
| `FR-021` | `BaixarPecasService` | Sem reservas ativas → lista vazia, sem evento |
| `FR-022` | `ConsultarReservasService.porOs(osId)`, `GET …/ordens/{osId}/reservas` | Inclui reservas ativas, liberadas e consumidas |

## Entry Point(s)

Controllers REST em `adapter/inbound/web/`, sob **`/api/v1/admin/estoque/**`**, protegidos pelo `anyRequest().authenticated()` do `SecurityConfig` (sem alteração). Os casos de uso de reserva também são a porta de entrada que o módulo `ordemservico` consumirá via ACL.

| Recurso | Operações |
|---|---|
| `/api/v1/admin/estoque/itens` | `POST` cadastrar · `GET` listar (`?ativo=`) · `GET /{id}` (`?ativo=true`) · `PUT /{id}` alterar · `POST /{id}/inativar` · `POST /{id}/entradas` (corpo `{quantidade}`) |
| `/api/v1/admin/estoque/ordens/{osId}/reservas` | `POST` reservar (corpo `{itemId, quantidade}`) · `GET` consultar reservas da OS · `POST /liberar` (`?itemId=` opcional) |
| `/api/v1/admin/estoque/ordens/{osId}/baixa` | `POST` dá baixa em todas as reservas ativas da OS |

Códigos: `201` cadastro e reserva criada · `200` consultas, alteração, entrada e baixa · `204` inativação e liberação · `400` dado inválido · `401` sem token · `404` item inexistente · `409` nome duplicado, item inativo, reserva já existente, inativar com reserva ativa e **quantidade indisponível**. Corpo de erro igual ao do `cadastro`: `{"erro": "...", "campo": "..."}`.

Os DTOs de entrada têm campos anuláveis, validados no domínio ("X é obrigatório"). Sem Bean Validation.

### Tratamento de erros

Mesmo padrão do `cadastro`: hierarquia `sealed` `EstoqueException` (`DadoInvalidoException(campo)`, `RegistroNaoEncontradoException`, `DuplicidadeException(campo)`, `ConflitoException`) em `domain/error`, convertida por um `@RestControllerAdvice` do módulo, que trata **somente** `EstoqueException` (sem catch-all). A indisponibilidade da reserva é `ResultadoReserva.Indisponivel` no caso de uso; o controller a traduz para `409`.

### Documentação Swagger/OpenAPI

Controllers com `@Tag`, `@Operation`, `@SecurityRequirement("bearerAuth")` e `@ApiResponses` para 400/401/404/409 (schema `ErroResponse`), usando `swagger-annotations` já presente no catálogo. O `OpenApiConfig` do `bootstrap` já define o esquema `bearerAuth`.

## Component Plan

| Camada / Área | Ação | Caminho | Notas |
|---|---|---|---|
| Build | Modificar | `modules/estoque/build.gradle.kts` | Copiar o padrão do `cadastro`: `kotlin.spring`, `kotlin.jpa`, `spring.dependency.management` + BOM; `starter-web`, `starter-data-jpa`, `swagger-annotations` |
| Domain · model | Criar | `…estoque/domain/model/` | `ItemEstoque`, `TipoItem`, `Quantidade` (value class, inteiro > 0), `Reserva`, `SituacaoReserva`, `ResultadoReserva` (sealed), `Validacao.kt` (helpers locais) |
| Domain · event | Criar | `…estoque/domain/event/` | `PecaReservada`, `PecaIndisponivel`, `ReservaLiberada`, `PecasBaixadas` (implementam `DomainEvent` do shared-kernel) |
| Domain · error | Criar | `…estoque/domain/error/` | `EstoqueException` (sealed) e as quatro filhas |
| Domain · port inbound | Criar | `…estoque/domain/port/inbound/` | `GerenciarItemUseCase`, `ConsultarItemUseCase`, `ReservarPecaUseCase`, `LiberarReservaUseCase`, `BaixarPecasUseCase`, `ConsultarReservasUseCase` |
| Domain · port outbound | Criar | `…estoque/domain/port/outbound/` | `ItemEstoqueRepository` (inclui `buscarPorIdParaAtualizar` com lock), `ReservaRepository`, `PublicadorDeEventos` |
| Application | Criar | `…estoque/application/usecase/` | Um service por caso de uso; `@Transactional` nos que alteram (cadastro de item, entrada, reservar, liberar, baixar) |
| Adapter · persistence | Criar | `…estoque/adapter/outbound/persistence/` | `ItemEstoqueJpaEntity`, `ReservaJpaEntity`, repositórios Spring Data, `*RepositoryAdapter`, `ViolacaoDeUnicidade` (SQLState `23505`) |
| Adapter · event | Criar | `…estoque/adapter/outbound/event/PublicadorDeEventosSpring.kt` | Sobre o `ApplicationEventPublisher`, como no `ordemservico` |
| Adapter · web | Criar | `…estoque/adapter/inbound/web/` | `ItemEstoqueController`, `ReservaController`, `EstoqueExceptionHandler`, `Localizacao`, DTOs em `dto/` |
| Adapter · config | Criar | `…estoque/adapter/config/EstoqueBeanConfig.kt` | Beans dos casos de uso |
| Migration | Criar | `bootstrap/src/main/resources/db/migration/` | Duas migrations (uma por entrega), numeradas na **próxima versão livre da `main` no momento do PR** (hoje V6 e V7) |
| Docs | Modificar | `README.md` | Seção do estoque (rotas, regras e exemplo) |

`bootstrap/build.gradle.kts` já inclui `:modules:estoque`; o component scan cobre o pacote. Nenhum ajuste em `SecurityConfig` nem `OpenApiConfig`.

## Data Model / Persistence

Migrations numeradas na próxima versão livre quando cada PR for aberto (a `main` está na V5; o outro dev também cria migrations, e versões repetidas quebram o Flyway). Schema `estoque` já existe (V1).

- **`estoque.item`**: `id` identity PK · `nome` varchar(120) NOT NULL · `descricao` varchar(500) NULL · `tipo` varchar(10) NOT NULL CHECK (`PECA`, `INSUMO`) · `preco` numeric(12,2) NOT NULL CHECK (≥ 0) · `saldo` int NOT NULL DEFAULT 0 CHECK (≥ 0) · `reservado` int NOT NULL DEFAULT 0 CHECK (≥ 0) · `ativo` boolean NOT NULL DEFAULT true · CHECK (`reservado <= saldo`) · índice **único** em `lower(nome)`.
- **`estoque.reserva`**: `id` identity PK · `item_id` bigint NOT NULL FK → `estoque.item` · `os_id` bigint NOT NULL (**sem FK**: a OS é outro contexto e outro schema) · `quantidade` int NOT NULL CHECK (> 0) · `situacao` varchar(10) NOT NULL CHECK (`ATIVA`, `LIBERADA`, `CONSUMIDA`) · índice **único parcial** em (`os_id`, `item_id`) onde `situacao = 'ATIVA'` · índice em `os_id`.

`reservado` é mantido no item (e não calculado pela soma das reservas) para o `CHECK` e o lock cobrirem o invariante numa linha só. `ddl-auto: validate` exige que as entidades batam com as colunas.

## Integrations & Observability

- **Integrações:** nenhuma externa. Os casos de uso `ReservarPeca`, `LiberarReserva`, `BaixarPecas` e `ConsultarReservas` são a porta que a OS consome (ACL em `ordemservico/adapter/outbound/acl/`). Criar essa ACL e a dependência Gradle da OS em `estoque` **não** faz parte desta feature.
- **Eventos:** os quatro fatos viram `DomainEvent` publicados pela porta `PublicadorDeEventos`; nenhum listener é criado aqui. A OS (ou outro módulo) pode assiná-los depois.
- **Observabilidade (logs, métricas, tracing): fora de escopo**, como no `cadastro`; o enunciado não a pede.

## Test Strategy

Meta: gate Jacoco ≥ 80% (LINE) em `domain` + `application`.

- **Unitários · domain (JUnit5 + AssertJ):** `Quantidade` (zero, negativo, positivo); `ItemEstoque` (criação válida, campos obrigatórios, preço, tipo, saldo inicial negativo; entrada soma ao saldo; `reservar` com disponível suficiente, exato e insuficiente; `liberar`; `baixar` reduz saldo e reservado; invariantes; item inativo rejeita alterar/entrada/reservar; `inativar` idempotente); `Reserva` (transições ativa → liberada/consumida e imutabilidade depois).
- **Unitários · application (MockK):** cadastrar (nome duplicado); alterar; entrada; inativar com `reservado > 0`; reservar (sucesso e evento, indisponível e evento, item inativo/inexistente, reserva já ativa para a mesma OS e item); liberar um item e a OS inteira (idempotente); baixar (vários itens, ordem de lock por `itemId`, vazia, repetida); consultar reservas; eventos publicados só quando a operação tem efeito.
- **Integração (Testcontainers + MockMvc), em `bootstrap/src/test/…/Estoque*IntegrationTest.kt`**, no padrão do `CadastroIntegrationTest` (login do `admin`): fluxo completo de itens e de reservas em `/api/v1/admin/estoque/**`; 401 sem token; 400/404/409 com `erro`/`campo`; unicidade de nome pelo banco; inativar item com reserva ativa; **disputa simultânea** pela última unidade (várias threads chamando o caso de uso: exatamente uma reserva e as demais indisponíveis); baixa com dois itens; `CHECK` do banco; `/v3/api-docs` publica as rotas.
- **BDD/Cucumber:** o `main` agora tem o runner do outro dev e os `.feature` ficam em `docs/bdd/`. Ligar o UC04 e o UC09 ao estoque exigiria a integração com a OS; fica **fora desta entrega**. As regras do estoque ficam cobertas pelos testes acima.

## Estratégia de entrega (PRs)

Meta: PRs de **cerca de 20 arquivos** (a contagem inclui remoções de `.gitkeep`), sequenciais — cada um parte da `main` já com o anterior mergeado — e cada um com sua issue e `Closes #`. A divisão separa o **núcleo de regras** (Kotlin puro, revisão focada nas regras) dos **adaptadores** (JPA, REST, migration), por agregado.

| # | PR | Conteúdo | Arquivos (estimativa) |
|---|---|---|---|
| 1 | `docs` (#58) | `spec.md`, `plan.md`, `tasks.md` | 3 |
| 2 | Itens · núcleo | `build.gradle.kts`, erros, `Quantidade`, `TipoItem`, `ItemEstoque`, helpers, portas de itens, services de itens, testes unitários | ~22 |
| 3 | Itens · persistência e API | Migration, entidade JPA, adapter, controller + DTOs + handler, bean config, teste de integração, README | ~14 |
| 4 | Reservas · núcleo | `Reserva`, `ResultadoReserva`, eventos, `ItemEstoque.reservar/liberar/baixar`, portas, 4 services, testes unitários | ~25 |
| 5 | Reservas · persistência e API | Migration, entidade JPA, adapter, publicador de eventos, controller + DTOs, bean config, integração com disputa simultânea, README | ~11 |

Se o PR 4 ultrapassar ~25 arquivos na implementação, divide-se em "reservar + liberar" e "baixar + consultar reservas". Os PRs 2 e 4 não trazem comportamento exposto: servem como base revisável dos 3 e 5, que deixam o fluxo utilizável e testável pelo Swagger.

## Quality / Lint Risks & Mitigation

- **Deadlock em operações com vários itens:** `baixar` e `liberar` da OS inteira bloqueiam itens em ordem crescente de `itemId`, sempre. `reservar` bloqueia um item só. Coberto por teste de aplicação (ordem das chamadas) e de integração.
- **Lock pessimista exige transação aberta:** os services que usam `buscarPorIdParaAtualizar` são `@Transactional` (permitido pelo `docs/arquitetura.md` §9). Fora de transação o lock não vale; coberto pelos testes de integração.
- **Reserva repetida sob concorrência (mesma OS e item):** a checagem prévia não basta; o índice único parcial é a garantia, traduzido em `ConflitoException`.
- **Duplicação com `cadastro`** (helpers de validação, `ErroResponse`, `ViolacaoDeUnicidade`, `Localizacao`): cada módulo é dono do seu código (bounded contexts). Extrair para o `shared-kernel` é refatoração futura, fora de escopo.
- **Colisão de versão de migration** com o outro dev: conferir a próxima versão livre da `main` (`git fetch`) antes de abrir cada PR de persistência e renumerar se preciso.
- **Cobertura do gate:** `adapter` não conta; a regra fica em `domain`/`application`.
- **Paginação:** as listagens retornam todos os registros (suficiente para o MVP).

## Alternatives Considered

- **Calcular o reservado pela soma das reservas:** exigiria consulta agregada a cada reserva e não deixaria o `CHECK` proteger o invariante. Descartado.
- **Atualização condicional atômica** (`UPDATE … WHERE saldo - reservado >= q`): resolve a concorrência sem lock, mas tira a regra do domínio e dificulta o teste unitário. Descartado em favor do lock pessimista + regra no domínio.
- **Lock otimista (`@Version`) com retry:** exigiria política de repetição; sob disputa alta gera falhas que o chamador teria de tratar. Descartado.
- **Indisponibilidade como exceção:** misturaria um resultado de negócio esperado com erro. Escolhido o `ResultadoReserva`.
- **Histórico de movimentações (kardex):** fora de escopo na spec.
- **Entrega em um PR só (~35 arquivos) ou em fatias verticais por agregado** (itens ~33, reservas ~35): descartadas pelo tamanho de revisão.

## Risks / Open Questions

- **Integração com a OS:** a OS precisa depender do módulo `estoque`, tratar o `ResultadoReserva.Indisponivel` (hotspot do Event Storming) e chamar liberação/baixa nos momentos dos UC06, UC07, UC08 e UC09. Fora desta entrega; alinhar com o outro dev.
- **Rotas e formato do erro** são decisão deste plano (REST aninhado por OS); confirmar com o time se a OS espera algo diferente.
- **`409` para indisponível** é uma escolha de contrato HTTP; a OS usa o caso de uso direto e não depende do código.
- **Tamanhos de coluna** (nome 120, descrição 500) são suposições; valores acima do limite retornam 400.
- **Eventos sem listener:** os eventos são publicados, mas ninguém reage ainda.
