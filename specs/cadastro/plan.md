# Technical Plan: Cadastro de clientes, veículos e serviços

**Spec Ref:** `specs/cadastro/spec.md`
**Issue:** #34
**Status:** Approved

## Summary

Implementar o módulo `modules/cadastro` seguindo o mesmo desenho hexagonal do `modules/autenticacao`:
modelos de domínio imutáveis com as regras (validação, inativação), portas de entrada (casos de uso) e de
saída (repositórios), serviços de aplicação sem Spring, e adapters de web (REST) e persistência (JPA).
Três agregados independentes (`Cliente`, `Veiculo`, `Servico`) no mesmo módulo, persistidos no schema
`cadastro` por uma migration Flyway no `bootstrap` (convenção atual do repositório). As validações de
CPF/CNPJ, placa e dinheiro reutilizam os VOs do `shared-kernel`. Unicidade é garantida por constraint no
banco, com checagem prévia na aplicação para dar mensagem clara. As rotas são versionadas (`/api/v1/admin/**`),
já protegidas pelo JWT (**sem mudança em segurança**), documentadas no Swagger e com erros tratados por
exceções de negócio claras, convertidas em HTTP por um `@RestControllerAdvice` do módulo.

## Requirement Mapping

Pacote raiz: `com.fiap.soat.oficina.cadastro` (abreviado `…cadastro`), em `modules/cadastro/src/main/kotlin/`.

| Spec Ref | Componente / Camada | Nota de Implementação |
|---|---|---|
| `FR-001` | `domain/model/Cliente.kt`, `GerenciarClienteService.cadastrar` | Fábrica `Cliente.criar(documento, nome, email)` exige os campos obrigatórios; nasce ativo |
| `FR-002` | `Cliente.criar` + VO `Documento` (shared-kernel) | `Documento.of` já aceita máscara e rejeita dígitos e repetidos; falha vira `DadoInvalidoException("documento")` |
| `FR-003` | `ClienteRepository.existePorDocumento` + `UNIQUE(documento)` | Checagem na aplicação (mensagem clara) e constraint no banco (concorrência) → `DuplicidadeException` |
| `FR-004` | `Cliente.criar/alterar` + VO `Email` | Nome em branco e e-mail ausente/formato inválido → `DadoInvalidoException` |
| `FR-005` | `Cliente.alterar(nome, email)` | Documento não faz parte da assinatura; DTO de alteração não o recebe |
| `FR-006` | `ConsultarClienteService` (`buscarPorId`, `buscarPorDocumento`, `listar`) | Documento da rota é normalizado pelo VO; buscas aceitam `somenteAtivos` (ver `FR-020`) |
| `FR-007` | `Veiculo.criar`, `GerenciarVeiculoService.cadastrar` | Campos: placa, marca, modelo, ano, clienteId |
| `FR-008` | `Veiculo.criar` + VO `Placa` | `Placa.of` normaliza e valida (antiga e Mercosul) |
| `FR-009` | `VeiculoRepository.existePorPlaca` + `UNIQUE(placa)` | Mesmo mecanismo do `FR-003` |
| `FR-010` | `GerenciarVeiculoService` consulta `ClienteRepository` | Cliente inexistente → `RegistroNaoEncontradoException`; inativo → `ConflitoException` |
| `FR-011` | `Veiculo.criar/alterar` | Marca/modelo em branco → inválido; ano entre 1900 e `anoAtual + 1` (suposição da spec), com `Clock` injetado para testar |
| `FR-012` | `Veiculo.alterar(marca, modelo, ano, clienteId)` | Placa fora da assinatura; troca de dono revalida cliente ativo (`FR-010`) |
| `FR-013` | `ConsultarVeiculoService` (`buscarPorId`, `buscarPorPlaca`, `listar(ativo, clienteId)`) | Filtro opcional por cliente e por `ativo` |
| `FR-014` | `Servico.criar`, `GerenciarServicoService.cadastrar` | Campos: nome, descrição (opcional), preço |
| `FR-015` | `ServicoRepository.existePorNome` + índice único em `lower(nome)` | Comparação sem diferenciar maiúsculas; checa também ao renomear |
| `FR-016` | `Servico.criar/alterar` + VO `Dinheiro` | Nome em branco / preço ausente → inválido; `Dinheiro.of` rejeita negativo e arredonda a 2 casas |
| `FR-017` | `Servico.alterar` | Sem efeito retroativo: o orçamento da OS grava o preço no momento da geração (responsabilidade do módulo `ordemservico`) |
| `FR-018` | `ConsultarServicoService` (`buscarPorId`, `listar`) | Filtro opcional por `ativo` |
| `FR-019` | `Cliente/Veiculo/Servico.inativar()` + `POST …/{id}/inativar` | Sem exclusão física; registro continua consultável |
| `FR-020` | Filtro `ativo` em todas as consultas + campo `ativo` nas respostas | `buscarPorId/Documento/Placa(…, somenteAtivos)` e `listar(ativo)`: com `somenteAtivos = true` um cadastro inativo é tratado como não encontrado. A OS deve consumir sempre com esse filtro. Ver Riscos |
| `FR-021` | `GerenciarClienteService.inativar` + `VeiculoRepository.existeAtivoDoCliente` | Há veículo ativo → `ConflitoException` |
| `FR-022` | `alterar()` dos três modelos | Modelo inativo lança `ConflitoException`; inativar de novo é idempotente |

## Entry Point(s)

Controllers REST em `adapter/inbound/web/`, sob **`/api/v1/admin/**`**. O `SecurityConfig` já exige
autenticação para qualquer rota fora da lista pública (`anyRequest().authenticated()`), então o prefixo novo
fica protegido **sem alteração** no `bootstrap`.

**Versionamento:** a versão vai no caminho (`/api/v1/...`), padrão adotado para os módulos novos.
`/api/auth/login` e `/api/public/**` não são alterados por esta feature (ver Riscos).

| Recurso | Operações |
|---|---|
| `/api/v1/admin/clientes` | `POST` cadastrar · `GET` listar (`?ativo=`) · `GET /{id}` · `GET /documento/{documento}` · `PUT /{id}` alterar · `POST /{id}/inativar` |
| `/api/v1/admin/veiculos` | `POST` · `GET` listar (`?clienteId=&ativo=`) · `GET /{id}` · `GET /placa/{placa}` · `PUT /{id}` (inclui `clienteId`) · `POST /{id}/inativar` |
| `/api/v1/admin/servicos` | `POST` · `GET` listar (`?ativo=`) · `GET /{id}` · `PUT /{id}` · `POST /{id}/inativar` |

Os `GET /{id}`, `/documento/{…}` e `/placa/{…}` aceitam `?ativo=true`: com o filtro, um cadastro inativo
responde `404` (mesma regra do `somenteAtivos` nos casos de uso, `FR-020`).

Campos dos DTOs de entrada são **anuláveis** e validados no domínio (mensagem "X é obrigatório"), em vez de
falhar na desserialização com erro genérico. Não será adicionada a dependência de Bean Validation.

### Tratamento de erros

Segue o padrão do `autenticacao` (exceção de negócio no `domain/error` + `@RestControllerAdvice` no
`adapter/inbound/web`), estendido para uma hierarquia:

- O domínio e a aplicação lançam apenas exceções de negócio com nome claro (`DadoInvalidoException`, `RegistroNaoEncontradoException`, `DuplicidadeException`, `ConflitoException`), todas filhas de `CadastroException`. `IllegalArgumentException` dos VOs do shared-kernel é convertida em `DadoInvalidoException(campo)` dentro do domínio, para o erro indicar o campo.
- `CadastroExceptionHandler` trata `CadastroException` e converte por subtipo: `400` dado inválido · `404` não encontrado · `409` duplicidade ou regra de negócio (cadastro inativo, cliente com veículos ativos). `401` vem do filtro de segurança.
- Corpo no mesmo formato do `autenticacao`, `{"erro": "mensagem"}`, acrescido de `"campo"` quando aplicável. Passa a ser um DTO `ErroResponse` (e não um `Map`) para aparecer no Swagger.
- **Sem handler genérico:** o `@RestControllerAdvice` vale para a aplicação inteira, então o handler do cadastro trata somente `CadastroException`. Não há (nem será criado aqui) um `@ExceptionHandler(Exception::class)` global; um handler global para todo o monolito é decisão do `bootstrap`, fora desta feature.

### Documentação Swagger/OpenAPI

O springdoc (já no `bootstrap`) gera a documentação a partir dos controllers, mas o `autenticacao` não tem
anotações. No `cadastro`, os controllers terão `@Tag`, `@Operation(summary)` e `@ApiResponse` para os
códigos `400/404/409` com o schema `ErroResponse`, usando só o artefato `swagger-annotations-jakarta`
(a versão `2.2.22` é a que o springdoc `2.6.0` já traz). Para testar as rotas protegidas pelo botão
**Authorize** do Swagger UI, o `bootstrap` ganha uma `OpenApiConfig` com o esquema `bearerAuth` (JWT).

## Component Plan

| Camada / Área | Ação | Caminho | Notas |
|---|---|---|---|
| Build | Modificar | `modules/cadastro/build.gradle.kts` | Copiar o padrão do `autenticacao`: plugins `kotlin.spring`, `kotlin.jpa`, `spring.dependency.management` + BOM; deps `starter-web`, `starter-data-jpa` e `swagger-annotations-jakarta` (sem security/jjwt); a lib entra em `gradle/libs.versions.toml` |
| Domain · model | Criar | `…cadastro/domain/model/` | `Cliente`, `Veiculo`, `Servico` (com `ativo`, `criar`, `alterar`, `inativar`), `Email` (value class) e helper de validação por campo |
| Domain · error | Criar | `…cadastro/domain/error/` | `CadastroException` (base) · `DadoInvalidoException(campo)` · `RegistroNaoEncontradoException` · `DuplicidadeException(campo)` · `ConflitoException` |
| Domain · port inbound | Criar | `…cadastro/domain/port/inbound/` | Por agregado, dois contratos: `GerenciarXUseCase` (cadastrar, alterar, inativar) e `ConsultarXUseCase` (buscas e listagens). O `Consultar*` é a porta que a OS consumirá via ACL |
| Domain · port outbound | Criar | `…cadastro/domain/port/outbound/` | `ClienteRepository`, `VeiculoRepository`, `ServicoRepository` (`salvar`, `buscarPorId`, `buscarPor…`, `existePor…`, `listar`, `existeAtivoDoCliente`) |
| Application | Criar | `…cadastro/application/usecase/` | `GerenciarClienteService`, `ConsultarClienteService` e equivalentes para veículo e serviço; classes Kotlin simples, `@Transactional` nos `Gerenciar*` (ver Riscos) |
| Adapter · web | Criar | `…cadastro/adapter/inbound/web/` | `ClienteController`, `VeiculoController`, `ServicoController`, `CadastroExceptionHandler` e DTOs em `dto/` (request/response, `ErroResponse` e mapeamento); anotações OpenAPI só aqui |
| Adapter · persistence | Criar | `…cadastro/adapter/outbound/persistence/` | `ClienteJpaEntity`, `VeiculoJpaEntity`, `ServicoJpaEntity`, `*SpringDataRepository`, `*RepositoryAdapter` (traduz `DataIntegrityViolationException` em `DuplicidadeException`) |
| Adapter · config | Criar | `…cadastro/adapter/config/CadastroBeanConfig.kt` | Instancia os services como beans, no estilo do `AutenticacaoBeanConfig`; fornece `Clock` (`@ConditionalOnMissingBean`) |
| Migration | Criar | `bootstrap/src/main/resources/db/migration/V5__create_cadastro.sql` | Ver abaixo |
| OpenAPI | Criar | `bootstrap/src/main/kotlin/com/fiap/soat/oficina/config/OpenApiConfig.kt` | Bean `OpenAPI` com título e esquema `bearerAuth` (botão Authorize) |
| Docs | Modificar | `README.md` e `docs/arquitetura.md` §8 | Rotas de cadastro; convenção passa a `/api/v1/admin/**` |

`bootstrap/build.gradle.kts` já inclui `:modules:cadastro`; o component scan já cobre o pacote novo. Nenhum
ajuste em `SecurityConfig`.

## Data Model / Persistence

Migration única `V5__create_cadastro.sql`, schema `cadastro` (já criado na V1), mantendo a migration
centralizada no `bootstrap` como a V2 (a discussão #27 sobre migrations por módulo segue em aberto e não é
decidida aqui):

- `cadastro.cliente`: `id` identity PK · `documento` varchar(14) NOT NULL **UNIQUE** (só dígitos) · `nome` varchar(150) NOT NULL · `email` varchar(150) NOT NULL · `ativo` boolean NOT NULL default true
- `cadastro.veiculo`: `id` identity PK · `placa` varchar(7) NOT NULL **UNIQUE** · `marca` varchar(60) NOT NULL · `modelo` varchar(60) NOT NULL · `ano` int NOT NULL · `cliente_id` bigint NOT NULL **FK** → `cadastro.cliente(id)` · `ativo` boolean NOT NULL default true · índice em `cliente_id`
- `cadastro.servico`: `id` identity PK · `nome` varchar(120) NOT NULL · `descricao` varchar(500) NULL · `preco` numeric(12,2) NOT NULL · `ativo` boolean NOT NULL default true · índice **único** em `lower(nome)`

`ddl-auto: validate` exige que as entidades JPA batam com essas colunas. Documento e placa são gravados
normalizados (só dígitos / maiúsculas sem separador). Sem seed de dados.

## Integrations

- **Integrações:** nenhuma externa. Os contratos `Consultar*UseCase` ficam prontos para o módulo `ordemservico` consumir via ACL (`adapter/outbound/acl/`); criar essa ACL e a dependência Gradle da OS em `cadastro` **não** faz parte desta feature.
- **Observabilidade (logs, métricas, tracing): fora de escopo.** O enunciado da Fase 1 não a exige (o "monitoramento" pedido é o tempo médio de execução das OS, do módulo `ordemservico`), e o projeto não tem nada disso hoje. Fica para a evolução em microsserviços. Os únicos "sinais" desta feature são os status HTTP e as mensagens de erro descritos acima. Sem eventos de domínio (o `domain/event` do módulo fica vazio).

## Test Strategy

Meta: gate Jacoco ≥ 80% (LINE) em `domain` + `application`, já aplicado pelo `./gradlew build`.

- **Unitários · domain (JUnit5 + AssertJ):** `Cliente`, `Veiculo`, `Servico`, `Email`: criação válida; campos obrigatórios; documento/placa/e-mail inválidos; ano nos limites (1899, 1900, ano atual+1, +2); preço zero e negativo; `alterar` não muda documento/placa; modelo inativo rejeita `alterar`; `inativar` idempotente.
- **Unitários · application (MockK):** cadastrar com sucesso; duplicidade de documento/placa/nome (inclusive de registro inativo e nome com caixa diferente); veículo com cliente inexistente, inativo e na troca de dono; inativar cliente com e sem veículos ativos; busca por documento/placa normalizados; não encontrado; `somenteAtivos = true` trata inativo como não encontrado.
- **Integração (Testcontainers + MockMvc), em `bootstrap/src/test/…/CadastroIntegrationTest.kt`**, no padrão do `AutenticacaoIntegrationTest` (login do `admin` para obter o token): fluxo completo dos 3 recursos em `/api/v1/admin/**`; 401 sem token; 400/404/409 com o corpo de erro esperado (`erro` e `campo`); duplicidade; inativar cliente com veículo ativo; listagem com filtros `ativo` e `clienteId`; busca por chave com `?ativo=true` em cadastro inativo (404); teste da constraint de unicidade pelo banco; `/v3/api-docs` publica os caminhos de cadastro.
- **Adapter de persistência:** coberto pelo teste de integração (mapeamento entidade ↔ modelo e a tradução do erro de unicidade).
- **BDD/Cucumber:** a infraestrutura ainda não existe no repositório e os `.feature` atuais são da OS; as regras desta spec ficam cobertas pelos testes acima. Não será adicionada a dependência agora.

## Quality / Lint Risks & Mitigation

- **Volume de código repetido** (3 agregados × portas/serviços/adapters). Mitigação: um padrão único e pequeno por agregado; sem abstração genérica prematura.
- **`@Transactional` no application:** os `Gerenciar*` precisam de uma transação para o "verificar e gravar" (ex.: inativar cliente conferindo veículos). O `docs/arquitetura.md` §9 admite `@Transactional` na aplicação, então a anotação fica nos services. O resto do domínio e da aplicação continua sem Spring.
- **Corrida na unicidade:** a checagem prévia não basta sob concorrência; a constraint é a garantia final e o adapter traduz a violação em 409.
- **Cobertura do gate:** `adapter` não conta no gate, mas a lógica de regra fica em `domain`/`application` justamente para ser medida e testada sem Spring.
- **Paginação:** as listagens retornam todos os registros (suficiente para o MVP). Paginar fica como evolução.
- **Convenções Kotlin/JPA:** entidades JPA com valores padrão (`kotlin-jpa` gera o construtor), como `UsuarioJpaEntity`; `@Entity` com `schema = "cadastro"`.

## Alternatives Considered

- **Um use case por operação** (como `AutenticarUseCase`): geraria cerca de 17 interfaces e classes. Descartado em favor de dois contratos por agregado (gerenciar/consultar), que também separa o que a OS consome.
- **Bean Validation nos DTOs:** exigiria nova dependência e espalharia regra no adapter; o enunciado pede validação no domínio.
- **Exclusão física com bloqueio por vínculo:** descartada pela spec (inativação preserva o histórico das OS).
- **Migrations dentro de cada módulo:** adiaria a discussão #27; seguimos a convenção atual (bootstrap).
- **Reativar cadastro:** fora de escopo da spec.
- **Rotas sem versão (`/api/admin/**`):** descartado; o versionamento no caminho evita quebrar clientes quando a API evoluir.
- **Handler global de exceções (`Exception::class`):** descartado aqui; o `@RestControllerAdvice` é global na aplicação e um catch-all do cadastro afetaria os outros módulos.

## Risks / Open Questions

- **Inativos na OS (`FR-020`):** combinado com o time: a OS consome o cadastro sempre com o filtro de ativos (`somenteAtivos = true`), assim nada inativo é usado. A ACL fica no módulo `ordemservico`, fora desta entrega.
- **Versionamento nos outros módulos:** `/api/auth/login` e `/api/public/**` (consulta de status da OS) continuam sem versão. Se o time quiser `/api/v1/public/**` na OS, o `SecurityConfig` precisa liberar o novo caminho. A decisão é do outro desenvolvedor; esta feature só aplica `v1` às rotas de cadastro.
- **Convenção de rotas no `docs/arquitetura.md` §8** diz `/api/admin/**`; será atualizada para `/api/v1/admin/**` nesta feature.
- **Swagger:** o `autenticacao` ainda não tem anotações OpenAPI. Não alteramos esse módulo; fica como melhoria sua, se o time quiser.
- **Tamanhos de coluna** (nome 150, marca/modelo 60 etc.) são suposições razoáveis; valores acima do limite retornam 400.
- **`@ConditionalOnMissingBean` para `Clock`:** se o módulo `ordemservico` também precisar de `Clock`, o bean pode migrar para o `bootstrap`.
