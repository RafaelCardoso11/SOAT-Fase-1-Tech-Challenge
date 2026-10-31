# Tasks: Cadastro de clientes, veículos e serviços

**Plan:** specs/cadastro/plan.md

Pacote raiz: `com.fiap.soat.oficina.cadastro` (`…cadastro`), em `modules/cadastro/src/main/kotlin/`. Testes em `modules/cadastro/src/test/kotlin/`.

## Setup / build

- [x] T001 `gradle/libs.versions.toml`: adicionar a lib `swagger-annotations` (`io.swagger.core.v3:swagger-annotations-jakarta`, versão `2.2.22`, a mesma que o springdoc 2.6.0 traz).
- [x] T002 `modules/cadastro/build.gradle.kts`: copiar o padrão do `autenticacao` (plugins `kotlin.spring`, `kotlin.jpa`, `spring.dependency.management` + BOM do Spring Boot); deps `spring-boot-starter-web`, `spring-boot-starter-data-jpa` e `swagger-annotations`; manter `oficina.jacoco-convention`.

## Domain (`modules/cadastro/.../domain/`)

- [x] T003 [P] `error/`: `CadastroException` (base) · `DadoInvalidoException(campo)` · `RegistroNaoEncontradoException` · `DuplicidadeException(campo)` · `ConflitoException`.
- [x] T004 `model/Validacao.kt` (helper que converte `IllegalArgumentException` dos VOs em `DadoInvalidoException(campo)`) e `model/Email.kt` (value class, formato válido). Depende de T003.
- [x] T005 [P] `model/Cliente.kt`: `criar(documento, nome, email)`, `alterar(nome, email)`, `inativar()`, `ativo`; documento via VO `Documento`; rejeita alterar inativo. Depende de T004.
- [x] T006 [P] `model/Veiculo.kt`: `criar(placa, marca, modelo, ano, clienteId, anoLimite)`, `alterar(marca, modelo, ano, clienteId, anoLimite)`, `inativar()`; placa via VO `Placa`; ano entre 1900 e o limite. Depende de T004.
- [x] T007 [P] `model/Servico.kt`: `criar(nome, descricao, preco)`, `alterar(...)`, `inativar()`; preço via VO `Dinheiro`. Depende de T004.
- [x] T008 `port/outbound/{ClienteRepository,VeiculoRepository,ServicoRepository}.kt`: `salvar`, `buscarPorId`, `buscarPorDocumento`/`Placa`, `existePor…`, `listar(ativo)`; `VeiculoRepository` com `listar(ativo, clienteId)` e `existeAtivoDoCliente`; `ServicoRepository.existePorNome` (sem caixa).
- [x] T009 `port/inbound/`: `GerenciarClienteUseCase` e `ConsultarClienteUseCase` (`buscarPorId/Documento(…, somenteAtivos)`, `listar(ativo)`); idem `Veiculo` (`listar(ativo, clienteId)`) e `Servico`.

## Application (`…/application/usecase/`)

- [x] T010 [P] `GerenciarClienteService.kt` (`@Transactional`): cadastrar (checa duplicidade de documento), alterar, inativar (conflito se houver veículo ativo; idempotente). Depende de T005, T008, T009.
- [x] T011 [P] `GerenciarVeiculoService.kt` (`@Transactional`): cadastrar e alterar exigindo cliente existente e ativo, duplicidade de placa, `Clock` para o limite do ano; inativar. Depende de T006, T008, T009.
- [x] T012 [P] `GerenciarServicoService.kt` (`@Transactional`): cadastrar e alterar (nome único ignorando caixa, inclusive ao renomear), inativar. Depende de T007, T008, T009.
- [x] T013 [P] `ConsultarClienteService.kt`, `ConsultarVeiculoService.kt`, `ConsultarServicoService.kt`: buscas e listagens; `somenteAtivos = true` trata inativo como `RegistroNaoEncontradoException`. Depende de T008, T009.

## Data / persistence (`…/adapter/outbound/persistence/`)

- [ ] T014 Migração `bootstrap/src/main/resources/db/migration/V3__create_cadastro.sql`: tabelas `cadastro.cliente`, `cadastro.veiculo` (FK + índice em `cliente_id`) e `cadastro.servico` (índice único em `lower(nome)`), conforme o plan (sem telefone).
- [ ] T015 [P] `ClienteJpaEntity.kt` + `ClienteSpringDataRepository.kt` + `ClienteRepositoryAdapter.kt` (mapeia entidade ↔ modelo; traduz `DataIntegrityViolationException` em `DuplicidadeException`). Depende de T008, T014.
- [ ] T016 [P] `VeiculoJpaEntity.kt` + `VeiculoSpringDataRepository.kt` + `VeiculoRepositoryAdapter.kt` (inclui `listar(ativo, clienteId)` e `existeAtivoDoCliente`). Depende de T008, T014.
- [ ] T017 [P] `ServicoJpaEntity.kt` + `ServicoSpringDataRepository.kt` + `ServicoRepositoryAdapter.kt` (busca de nome sem caixa). Depende de T008, T014.

## Adapters — web (`…/adapter/inbound/web/` e `…/adapter/config/`)

- [ ] T018 `dto/ErroResponse.kt` e `CadastroExceptionHandler.kt` (`@RestControllerAdvice` só para `CadastroException`: 400/404/409, corpo `erro` + `campo`; sem handler genérico). Depende de T003.
- [ ] T019 [P] `ClienteController.kt` + DTOs em `dto/` (`/api/v1/admin/clientes`: POST, GET lista com `?ativo=`, GET `/{id}`, GET `/documento/{documento}`, PUT `/{id}`, POST `/{id}/inativar`; campos de entrada anuláveis; anotações OpenAPI). Depende de T009, T018.
- [ ] T020 [P] `VeiculoController.kt` + DTOs (`/api/v1/admin/veiculos`: idem, mais `?clienteId=` e `/placa/{placa}`). Depende de T009, T018.
- [ ] T021 [P] `ServicoController.kt` + DTOs (`/api/v1/admin/servicos`). Depende de T009, T018.
- [ ] T022 `adapter/config/CadastroBeanConfig.kt`: instancia os 6 services como beans (portas de entrada) e o `Clock` (`@ConditionalOnMissingBean`). Depende de T010–T013.
- [ ] T023 `bootstrap/src/main/kotlin/com/fiap/soat/oficina/config/OpenApiConfig.kt`: bean `OpenAPI` com título e esquema `bearerAuth` (botão Authorize do Swagger UI).

## Tests

- [ ] T024 [P] Unit domain (JUnit5 + AssertJ) em `src/test/…/domain/`: `ClienteTest`, `VeiculoTest`, `ServicoTest`, `EmailTest` — criação válida; obrigatórios; documento/placa/e-mail inválidos; ano em 1899, 1900, limite e limite+1; preço zero e negativo; `alterar` sem tocar documento/placa; inativo rejeita `alterar`; `inativar` idempotente. Depende de T005–T007.
- [ ] T025 [P] Unit application (MockK) em `src/test/…/application/`: `GerenciarClienteServiceTest` (sucesso, duplicidade inclusive de inativo, inativar com/sem veículo ativo), `GerenciarVeiculoServiceTest` (cliente inexistente/inativo no cadastro e na troca de dono, placa duplicada, ano pelo `Clock`), `GerenciarServicoServiceTest` (nome duplicado com caixa diferente, ao renomear), `Consultar*ServiceTest` (não encontrado; `somenteAtivos` trata inativo como não encontrado; documento/placa normalizados). Depende de T010–T013.
- [ ] T026 Integração `bootstrap/src/test/…/CadastroIntegrationTest.kt` (Testcontainers + MockMvc, token do `admin` como no `AutenticacaoIntegrationTest`): fluxo completo dos 3 recursos em `/api/v1/admin/**`; 401 sem token; 400/404/409 com `erro`/`campo`; unicidade de documento, placa e nome (inclusive inativo) pelo banco; inativar cliente com veículo ativo → 409; troca de dono; filtros `?ativo=` e `?clienteId=`; busca por chave com `?ativo=true` em inativo → 404; `/v3/api-docs` contém os caminhos de cadastro. Depende de T014–T023.

## Documentação

- [ ] T027 `README.md`: seção sobre o cadastro (rotas `/api/v1/admin/...`, como obter o token e chamar, Swagger com Authorize) e `docs/arquitetura.md` §8: convenção `/api/v1/admin/**`.

## Finalize

- [ ] T028 Rodar `./gradlew build` (unit + integração + gate Jacoco ≥ 80% em `domain`+`application`) e corrigir falhas; conferir que o código segue o estilo do `autenticacao`.
- [ ] T029 Subir a aplicação (`bootRun`) e validar manualmente no Swagger UI/`curl`: login, criar cliente → veículo → serviço, duplicidade, inativação.
- [ ] T030 Commits em Conventional Commits (`feat(cadastro): …`, `test(cadastro): …`, `docs: …`) com `specs/cadastro/` incluído; preparar o PR (`## Objetivo / O que entra / Como validar / Escopo / Fora de escopo` + `Closes #34`). Push e abertura do PR ficam com você.
