# Technical Plan: Autenticação JWT e proteção das APIs administrativas

**Spec Ref:** `specs/auth-jwt/spec.md`
**Status:** Draft

## Summary

Implementar autenticação por **JWT** no módulo `autenticacao` (subdomínio genérico), seguindo a
arquitetura hexagonal do projeto. O login valida credenciais contra usuários persistidos no schema
`autenticacao` e emite um **access token** assinado (HMAC) com expiração. Um filtro de segurança valida
o token e adota postura **secure-by-default**: tudo sob `/api/**` exige token, exceto prefixos públicos
(`/api/auth/**`, `/api/public/**`) e os endpoints de documentação/health. Senhas são armazenadas com
**hash BCrypt**. Usuários de demonstração são semeados por migração Flyway.

Open questions do spec resolvidas:
- Identificador de login: **nome de usuário** (string única).
- Expiração do token: **configurável** via `JWT_EXPIRATION_MINUTES` (default 60), e `JWT_SECRET` para a assinatura (ambos já previstos no `.env.example`).
- Usuários de demonstração: semeados via migração (credenciais documentadas no README na implementação).

## Requirement Mapping

| Spec Ref | Componente / Camada | Nota de Implementação |
|---|---|---|
| `FR-001` | `autenticacao/application/usecase/AutenticarService.kt` + `adapter/inbound/web/AutenticacaoController.kt` | Recebe credenciais, valida e emite token |
| `FR-002` | `AutenticarService` | Credenciais inválidas → falha de autenticação, sem token |
| `FR-003` | `autenticacao/adapter/inbound/web/SecurityConfig.kt` + `JwtAuthenticationFilter.kt` | Exige token válido em `/api/**` (exceto públicos) |
| `FR-004` | `JwtAuthenticationFilter` + `JwtTokenProvider` | Token ausente/ inválido/ expirado → 401 |
| `FR-005` | `SecurityConfig` | Libera `/api/public/**` (onde morará a consulta de status — UC12) |
| `FR-006` | `adapter/outbound/seguranca/JwtTokenProvider.kt` | Expiração no token; configurável por ambiente |
| `FR-007` | `autenticacao/adapter/config/SeedUsuariosDemo.kt` | Usuários de demonstração semeados no boot (hash via codificador) |
| `FR-008` | `adapter/outbound/seguranca/BCryptCodificadorDeSenha.kt` | Hash BCrypt; nunca texto puro |
| `FR-009` | `AutenticarService` | Falha genérica (não distingue usuário × senha) |

## Entry Point(s)

- **POST `/api/auth/login`** (público): entrada `{ nomeUsuario, senha }`; saída `{ token, tipo: "Bearer", expiraEmSegundos }`. Credenciais inválidas → **401**.
- **Filtro de autenticação** em todas as requisições `/api/**`: lê o cabeçalho `Authorization: Bearer <token>`, valida e popula o contexto de segurança. Sem/with token inválido em rota protegida → **401**.
- **Convenção de rotas (secure-by-default):**
  - Públicas: `/api/auth/**`, `/api/public/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`.
  - Autenticadas: todo o resto sob `/api/**`.
  - > A consulta pública da OS (UC12) deverá ser exposta sob `/api/public/**`.

## Component Plan

| Camada / Área | Ação | Caminho (em `modules/autenticacao/src/main/kotlin/.../autenticacao/`) | Notas |
|---|---|---|---|
| Domain (model) | Criar | `domain/model/Usuario.kt` | Raiz: id, nomeUsuario, senhaHash, ativo |
| Domain (port in) | Criar | `domain/port/inbound/AutenticarUseCase.kt` | `autenticar(nomeUsuario, senha): ResultadoAutenticacao` |
| Domain (port out) | Criar | `domain/port/outbound/UsuarioRepository.kt` | `buscarPorNomeUsuario(nome): Usuario?` |
| Domain (port out) | Criar | `domain/port/outbound/CodificadorDeSenha.kt` | `codificar(senha): Hash`, `confere(senha, hash): Boolean` |
| Domain (port out) | Criar | `domain/port/outbound/TokenProvider.kt` | `emitir(usuario): TokenEmitido`, `validar(token): Identidade?` |
| Application | Criar | `application/usecase/AutenticarService.kt` | Orquestra: busca usuário → confere senha → emite token; falha genérica |
| Adapter (in/web) | Criar | `adapter/inbound/web/AutenticacaoController.kt` (+ DTOs `LoginRequest`/`LoginResponse`) | Rota POST login, mapeia erro → 401 |
| Adapter (in/web) | Criar | `adapter/inbound/web/SecurityConfig.kt` | `SecurityFilterChain`: regras público × autenticado, stateless |
| Adapter (in/web) | Criar | `adapter/inbound/web/JwtAuthenticationFilter.kt` | Valida token por requisição e popula o contexto |
| Adapter (out/persist) | Criar | `adapter/outbound/persistence/{UsuarioJpaEntity,UsuarioSpringDataRepository,UsuarioRepositoryAdapter}.kt` | Mapeia Usuario ↔ tabela `autenticacao.usuario` |
| Adapter (out/seguranca) | Criar | `adapter/outbound/seguranca/JwtTokenProvider.kt` | Implementa `TokenProvider` (JWT HMAC, jjwt) |
| Adapter (out/seguranca) | Criar | `adapter/outbound/seguranca/BCryptCodificadorDeSenha.kt` | Implementa `CodificadorDeSenha` (BCrypt) |

**Build (`modules/autenticacao/build.gradle.kts`):** este módulo passa a ter adapters Spring, então aplica
os plugins `kotlin-spring` + `spring-dependency-management` (import do BOM do Spring Boot para versões
gerenciadas) além do `kotlin-jvm` + `jacoco`. Dependências: `spring-boot-starter-web`,
`spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `jjwt` (api + impl + jackson). Isso
**estabelece o padrão** para os demais módulos de domínio que tiverem adapters Spring (ex.: `ordemservico`).
Novas libs no catálogo `gradle/libs.versions.toml`: `jjwt-api`, `jjwt-impl`, `jjwt-jackson`.

## Data Model / Persistence

Schema `autenticacao` (já criado pela baseline `V1`). Novas migrações em `bootstrap/src/main/resources/db/migration/`:

- `V2__create_usuario.sql`: tabela `autenticacao.usuario` (`id` PK, `nome_usuario` único/not null, `senha_hash` not null, `ativo` boolean).

Os **usuários de demonstração** são semeados por um **inicializador de aplicação** (`SeedUsuariosDemo`, `ApplicationRunner`) que usa o `CodificadorDeSenha` para gerar o hash no boot — garante que o hash corresponde à senha documentada (evita hash pré-computado frágil). Idempotente (só cria se ausente).

Entidade JPA `UsuarioJpaEntity` mapeada ao schema `autenticacao`. `ddl-auto` permanece `validate` (schema é do Flyway).

## Integrations & Observability

- **Integrações:** nenhuma externa. O módulo expõe, para os demais, apenas a **postura de segurança** (filtro + convenção de rotas); não há porta de saída para outros contextos.
- **Observabilidade:**
  - Log de login bem-sucedido (nome de usuário) e de falha (sem indicar usuário × senha).
  - Respostas: `200` (login ok), `401` (credenciais inválidas; token ausente/ inválido/ expirado).
  - Segredo de assinatura (`JWT_SECRET`) e expiração (`JWT_EXPIRATION_MINUTES`) vêm do ambiente.

## Test Strategy

- **Unitários (JUnit5 + MockK), no módulo `autenticacao`:**
  - `AutenticarService`: credenciais válidas → emite token; senha incorreta → falha; usuário inexistente → **mesma** falha genérica (FR-009); usuário inativo → falha.
  - `JwtTokenProvider`: emitir → validar (round-trip); token expirado → inválido; assinatura adulterada → inválido.
  - `BCryptCodificadorDeSenha`: `confere` verdadeiro p/ hash da própria senha; falso caso contrário.
- **Integração (Spring + Testcontainers/Postgres), no `bootstrap`:**
  - `POST /api/auth/login` com usuário semeado válido → 200 + token; inválido → 401.
  - Rota protegida (`/api/**`) sem token → 401; com token válido → acessa.
  - Rota pública (`/api/public/**` de teste) → acessível sem token.
- **BDD/Cucumber:** não há `.feature` para autenticação (é transversal, não um UC); cobertura via unit + integração.
- **Gate:** `./gradlew build` com cobertura Jacoco ≥ 80% em `domain` + `application` do módulo.

## Quality / Lint Risks & Mitigation

- **Setup Spring no módulo:** primeira vez que um módulo de domínio ganha Spring; mitigação: documentar o padrão (plugins + BOM) para reuso nos próximos módulos.
- **Secure-by-default:** risco de bloquear rota pública por engano; mitigação: convenção explícita de prefixos (`/api/public/**`) e teste de integração cobrindo rota pública.
- **Segredo/hash:** `JWT_SECRET` nunca no código (vem do ambiente); hash de seed pré-computado deve corresponder à senha documentada — teste de integração de login cobre isso.
- **Vazamento de informação:** FR-009 exige mensagem genérica — teste unitário garante.

## Alternatives Considered

- **Spring Security OAuth2 Resource Server (Nimbus):** mais "padrão de mercado", porém configuração mais pesada p/ um fluxo de login próprio; escolhido **jjwt + filtro custom** por simplicidade e controle.
- **Autorização por perfil (RBAC):** descartada neste escopo (Non-Goal do spec) — modelo mantém espaço p/ evoluir.
- **Seed via código (ApplicationRunner):** alternativa ao seed por migração; optou-se por **Flyway** para determinismo e rastreabilidade.

## Risks / Open Questions

- Credenciais exatas dos usuários de demonstração: definidas na implementação e documentadas no README.
- Confirmar o prefixo público definitivo para a consulta da OS (UC12) ao implementar aquele caso de uso (`/api/public/**`).
