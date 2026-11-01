# Tasks: Estoque de peças e insumos

**Plan:** specs/estoque/plan.md

As tarefas estão agrupadas por **PR** (ver "Estratégia de entrega" no plano). Cada PR parte da `main` já com o anterior mergeado, tem a sua própria issue e termina com `Closes #<issue>`. Dentro de cada PR, as tarefas formam blocos de revisão: o commit sai depois da aprovação do bloco.

Pacote raiz: `com.fiap.soat.oficina.estoque` (`…estoque`), em `modules/estoque/src/main/kotlin/`. Testes em `modules/estoque/src/test/kotlin/`. Remover os `.gitkeep` das pastas que passarem a ter arquivos.

---

## PR 1 — Documentação (issue #58, branch `docs/58-estoque-sdd`)

- [x] T001 Commitar `specs/estoque/{spec,plan,tasks}.md` (`docs(estoque): spec, plano e tarefas do estoque`).
- [x] T002 Abrir o PR (`Closes #58`): 3 arquivos, sem código de produção.

---

## PR 2 — Itens de estoque · núcleo (branch `feat/<issue>-estoque-itens-nucleo`)

### Setup

- [x] T003 Abrir a issue do PR 2 (`enhancement`) e criar a branch a partir de `origin/main` atualizada.
- [x] T004 `modules/estoque/build.gradle.kts`: copiar o padrão do `cadastro` (`kotlin.spring`, `kotlin.jpa`, `spring.dependency.management` + BOM; deps `starter-web`, `starter-data-jpa`, `swagger-annotations`; manter `oficina.jacoco-convention`).

### Domain (`…estoque/domain/`)

- [x] T005 [P] `error/`: `EstoqueException` (sealed) · `DadoInvalidoException(campo)` · `RegistroNaoEncontradoException` · `DuplicidadeException(campo)` · `ConflitoException`.
- [x] T006 `model/Validacao.kt`: helpers `validando`, `textoObrigatorio` e `exigirAtivo` (cópia local, no padrão do `cadastro`). Depende de T005.
- [x] T007 [P] `model/Quantidade.kt` (value class, inteiro > 0, "Quantidade deve ser maior que zero") e `model/TipoItem.kt` (`PECA`, `INSUMO`).
- [x] T008 `model/ItemEstoque.kt`: `criar(nome, descricao, tipo, preco, saldoInicial)`, `alterar(nome, descricao, tipo, preco)`, `registrarEntrada(Quantidade)`, `inativar()`, `disponivel`, `reconstituir(...)`; item inativo rejeita alterar e entrada. Depende de T006, T007.
- [x] T009 `port/outbound/ItemEstoqueRepository.kt`: `salvar`, `buscarPorId`, `buscarPorIdParaAtualizar` (lock), `existePorNome(nome, ignorandoId)`, `listar(ativo)`.
- [x] T010 `port/inbound/`: `GerenciarItemUseCase` (`cadastrar`, `alterar`, `registrarEntrada`, `inativar`) e `ConsultarItemUseCase` (`buscarPorId(id, somenteAtivos)`, `listar(ativo)`). Depende de T008.

### Application (`…estoque/application/usecase/`)

- [x] T011 `GerenciarItemService.kt` (`@Transactional`): cadastrar (nome duplicado), alterar, entrada (item com lock), inativar (negado se `reservado > 0`; idempotente). Depende de T008–T010.
- [x] T012 `ConsultarItemService.kt`: busca por id (com `somenteAtivos`) e listagem. Depende de T009, T010.

### Tests (`modules/estoque/src/test/kotlin/…/estoque/`)

- [x] T013 [P] `Assercoes.kt` (helpers `assertDadoInvalido`/`assertDuplicidade`) e `domain/model/QuantidadeTest.kt` (zero, negativo, positivo).
- [x] T014 `domain/model/ItemEstoqueTest.kt`: criação válida e normalizada; nome/tipo/preço/saldo inicial inválidos; preço zero; alterar sem mexer em saldo/reservado; entrada soma ao saldo; `disponivel`; inativo rejeita alterar e entrada; `inativar` idempotente. Depende de T008, T013.
- [x] T015 `application/GerenciarItemServiceTest.kt` (MockK): cadastrar, nome duplicado, alterar, entrada, item inexistente, inativar com e sem `reservado`, inativar repetido. Depende de T011, T013.
- [x] T016 `application/ConsultarItemServiceTest.kt`: busca, não encontrado, `somenteAtivos` trata inativo como não encontrado, listagem com filtro. Depende de T012, T013.

### Finalize

- [x] T017 Rodar `./gradlew :modules:estoque:build` e `./gradlew build` (gate Jacoco ≥ 80%); conferir `git diff --stat origin/main...HEAD` (~22 arquivos); preparar o PR (`Closes #<issue>`).

---

## PR 3 — Itens de estoque · persistência e API (branch `feat/<issue>-estoque-itens-api`)

### Setup

- [x] T018 Abrir a issue do PR 3 e criar a branch a partir da `main` com o PR 2 mergeado; `git fetch` e conferir a próxima versão livre de migration.

### Data / persistence (`…estoque/adapter/outbound/persistence/`)

- [x] T019 Migration `bootstrap/src/main/resources/db/migration/V7__create_estoque_item.sql`: `estoque.item` com `CHECK`s (tipo, preço, saldo, reservado, `reservado <= saldo`) e índice único em `lower(nome)`.
- [x] T020 `ItemEstoqueJpaEntity.kt` e `ItemEstoqueSpringDataRepository.kt` (inclui consulta com `@Lock(PESSIMISTIC_WRITE)`). Depende de T019.
- [x] T021 `ItemEstoqueRepositoryAdapter.kt` e `ViolacaoDeUnicidade.kt` (SQLState `23505` → `DuplicidadeException`). Depende de T020.

### Adapters — web (`…estoque/adapter/inbound/web/` e `…/adapter/config/`)

- [x] T022 `dto/ErroResponse.kt`, `EstoqueExceptionHandler.kt` (somente `EstoqueException`: 400/404/409, sem catch-all) e `Localizacao.kt`.
- [x] T023 `ItemEstoqueController.kt` + `dto/ItemEstoqueDtos.kt` (`/api/v1/admin/estoque/itens`: POST, GET lista com `?ativo=`, GET `/{id}` com `?ativo=true`, PUT, `/{id}/inativar`, `/{id}/entradas`; saldo/reservado/disponivel na resposta; anotações OpenAPI). Depende de T010, T022.
- [x] T024 `adapter/config/EstoqueBeanConfig.kt`: beans de `GerenciarItemUseCase` e `ConsultarItemUseCase`. Depende de T011, T012.

### Tests

- [ ] T025 `bootstrap/src/test/kotlin/com/fiap/soat/oficina/EstoqueItensIntegrationTest.kt` (Testcontainers + MockMvc): fluxo completo; 401 sem token; 400/404/409 com `erro`/`campo`; nome duplicado (inclusive inativo e caixa diferente); entrada; filtros `?ativo=`; unicidade pelo banco; `CHECK` do banco; `/v3/api-docs` publica as rotas. Depende de T019–T024.

### Documentação

- [ ] T026 `README.md`: seção "Estoque — itens" (rotas, regras, exemplo).

### Finalize

- [ ] T027 Criar `api-local/estoque-itens.http` (fora do Git) e validar manualmente com a aplicação no ar.
- [ ] T028 Rodar `./gradlew build` (gate Jacoco); conferir o diff (~14 arquivos); preparar o PR (`Closes #<issue>`).

---

## PR 4 — Reservas · núcleo (branch `feat/<issue>-estoque-reservas-nucleo`)

### Setup

- [ ] T029 Abrir a issue do PR 4 e criar a branch a partir da `main` com o PR 3 mergeado.

### Domain (`…estoque/domain/`)

- [ ] T030 [P] `model/SituacaoReserva.kt` (`ATIVA`, `LIBERADA`, `CONSUMIDA`), `model/Reserva.kt` (`criar(osId, itemId, quantidade)`, `liberar()`, `consumir()`; imutável depois de liberada/consumida) e `model/ResultadoReserva.kt` (sealed: `Reservada(reserva)`, `Indisponivel(disponivel)`).
- [ ] T031 `model/ItemEstoque.kt`: acrescentar `reservar(Quantidade)`, `liberar(Quantidade)` e `baixar(Quantidade)`, mantendo os invariantes (saldo ≥ 0, reservado ≤ saldo); item inativo rejeita reservar. Depende de T030.
- [ ] T032 [P] `event/`: `PecaReservada`, `PecaIndisponivel`, `ReservaLiberada`, `PecasBaixadas` (implementam `DomainEvent`).
- [ ] T033 `port/outbound/ReservaRepository.kt` (`salvar`, `buscarAtivaPorOsEItem`, `listarAtivasPorOs`, `listarPorOs`) e `port/outbound/PublicadorDeEventos.kt`.
- [ ] T034 `port/inbound/`: `ReservarPecaUseCase`, `LiberarReservaUseCase`, `BaixarPecasUseCase`, `ConsultarReservasUseCase`. Depende de T030.

### Application (`…estoque/application/usecase/`)

- [ ] T035 `ReservarPecaService.kt` (`@Transactional`): item com lock; inexistente/inativo; reserva já ativa para a OS e o item → conflito; disponível insuficiente → `Indisponivel` + evento; senão reserva, salva e publica `PecaReservada`. Depende de T031–T034.
- [ ] T036 `LiberarReservaService.kt` (`@Transactional`): um item ou a OS inteira; itens bloqueados em ordem de `itemId`; idempotente; evento `ReservaLiberada` por reserva liberada. Depende de T031–T034.
- [ ] T037 `BaixarPecasService.kt` (`@Transactional`): reservas ativas da OS, itens bloqueados em ordem de `itemId`, `baixar` em cada um, reservas consumidas, evento `PecasBaixadas`; vazia/repetida sem efeito. Depende de T031–T034.
- [ ] T038 `ConsultarReservasService.kt`: reservas da OS (todas as situações). Depende de T033, T034.

### Tests

- [ ] T039 `domain/model/ReservaTest.kt` e extensão de `ItemEstoqueTest.kt`: `reservar` com disponível suficiente, exato e insuficiente; `liberar`; `baixar` reduz saldo e reservado; invariantes; inativo rejeita reservar; transições da `Reserva`. Depende de T030, T031.
- [ ] T040 `application/ReservarPecaServiceTest.kt` e `LiberarReservaServiceTest.kt` (MockK): sucesso e evento, indisponível e evento, item inativo/inexistente, reserva repetida, liberar item e OS inteira, idempotência, ordem de lock. Depende de T035, T036.
- [ ] T041 `application/BaixarPecasServiceTest.kt` e `ConsultarReservasServiceTest.kt`: baixa com vários itens, ordem de lock, vazia, repetida, evento só com efeito; consulta com situações. Depende de T037, T038.

### Finalize

- [ ] T042 Rodar `./gradlew build` (gate Jacoco); conferir o diff (~25 arquivos; se passar muito, dividir em "reservar + liberar" e "baixar + consultar"); preparar o PR (`Closes #<issue>`).

---

## PR 5 — Reservas · persistência e API (branch `feat/<issue>-estoque-reservas-api`)

### Setup

- [ ] T043 Abrir a issue do PR 5 e criar a branch a partir da `main` com o PR 4 mergeado; `git fetch` e conferir a próxima versão livre de migration.

### Data / persistence e eventos

- [ ] T044 Migration `bootstrap/src/main/resources/db/migration/V<próxima>__create_estoque_reserva.sql`: `estoque.reserva` com FK para `estoque.item`, `CHECK`s, índice único parcial (`os_id`, `item_id`) onde `ATIVA` e índice em `os_id`.
- [ ] T045 `ReservaJpaEntity.kt` e `ReservaSpringDataRepository.kt`. Depende de T044.
- [ ] T046 `ReservaRepositoryAdapter.kt` (violação do índice parcial → `ConflitoException`). Depende de T045.
- [ ] T047 `adapter/outbound/event/PublicadorDeEventosSpring.kt` (sobre o `ApplicationEventPublisher`, no padrão do `ordemservico`).

### Adapters — web

- [ ] T048 `ReservaController.kt` + `dto/ReservaDtos.kt` (`/api/v1/admin/estoque/ordens/{osId}/reservas`: POST reservar → `201` ou `409` indisponível, GET consultar, POST `/liberar` com `?itemId=` opcional; `/api/v1/admin/estoque/ordens/{osId}/baixa`: POST; anotações OpenAPI).
- [ ] T049 `EstoqueBeanConfig.kt`: beans dos quatro casos de uso de reserva. Depende de T035–T038.

### Tests

- [ ] T050 `bootstrap/src/test/kotlin/com/fiap/soat/oficina/EstoqueReservasIntegrationTest.kt` (Testcontainers + MockMvc): reservar, indisponível (409), reserva repetida, liberar item e OS, baixa com dois itens e saldo final, idempotência da baixa, inativar item com reserva ativa, 401 sem token, **disputa simultânea pela última unidade** (várias threads: exatamente uma reserva), `CHECK` do banco, eventos publicados, `/v3/api-docs`. Depende de T044–T049.

### Documentação

- [ ] T051 `README.md`: seção "Estoque — reservas e baixa".

### Finalize

- [ ] T052 Criar `api-local/estoque-reservas.http` (fora do Git) e validar manualmente com a aplicação no ar.
- [ ] T053 Rodar `./gradlew build` (gate Jacoco); conferir o diff (~11 arquivos); preparar o PR (`Closes #<issue>`).
