# Tasks: Autenticação JWT e proteção das APIs administrativas

**Plan:** specs/auth-jwt/plan.md

## Setup / build

- [ ] T001 Catálogo `gradle/libs.versions.toml`: adicionar `jjwt` (api/impl/jackson) e alias de plugin `kotlin-jpa`.
- [ ] T002 `modules/autenticacao/build.gradle.kts`: aplicar `kotlin-spring` + `kotlin-jpa` + `spring-dependency-management` (import do BOM do Spring Boot); deps `spring-boot-starter-web`, `-security`, `-data-jpa`, `jjwt`; escopar o gate Jacoco a `domain` + `application`.
- [ ] T003 `bootstrap/.../application.yml`: `jwt.secret` e `jwt.expiration-minutes` (de `JWT_SECRET`/`JWT_EXPIRATION_MINUTES`).

## Domain (`modules/autenticacao/.../domain/`)

- [ ] T004 [P] `model/Usuario.kt` (id, nomeUsuario, senhaHash, ativo).
- [ ] T005 [P] `model/TokenEmitido.kt` (token, expiraEmSegundos).
- [ ] T006 [P] `error/CredenciaisInvalidasException.kt`.
- [ ] T007 `port/inbound/AutenticarUseCase.kt` (`autenticar(nomeUsuario, senha): TokenEmitido`).
- [ ] T008 `port/outbound/{UsuarioRepository,CodificadorDeSenha,TokenProvider}.kt`.

## Application

- [ ] T009 `application/usecase/AutenticarService.kt` (busca → confere senha → emite token; falha genérica FR-009).

## Adapters — persistência (`adapter/outbound/persistence/`)

- [ ] T010 `UsuarioJpaEntity.kt` (schema `autenticacao`, tabela `usuario`) + `UsuarioSpringDataRepository.kt` (`findByNomeUsuario`).
- [ ] T011 `UsuarioRepositoryAdapter.kt` (implementa `UsuarioRepository`).
- [ ] T012 Migração `bootstrap/.../db/migration/V2__create_usuario.sql` (tabela `autenticacao.usuario`).
- [ ] T013 `adapter/config/SeedUsuariosDemo.kt` (inicializador: cria usuários demo via `CodificadorDeSenha` se ausentes).

## Adapters — segurança (`adapter/outbound/seguranca/` e `adapter/inbound/web/`)

- [ ] T014 [P] `seguranca/BCryptCodificadorDeSenha.kt` (implementa `CodificadorDeSenha`).
- [ ] T015 `seguranca/JwtTokenProvider.kt` (implementa `TokenProvider`; jjwt HMAC; expiração configurável).
- [ ] T016 `inbound/web/JwtAuthenticationFilter.kt` (valida `Authorization: Bearer` e popula o contexto).
- [ ] T017 `inbound/web/SecurityConfig.kt` (stateless; públicos: `/api/auth/**`, `/api/public/**`, `/api/ping`, swagger, `actuator/health`; resto autenticado).
- [ ] T018 `inbound/web/AutenticacaoController.kt` + DTOs `LoginRequest`/`LoginResponse` + `AutenticacaoExceptionHandler` (→ 401).
- [ ] T019 `adapter/config/AutenticacaoBeanConfig.kt` (wire do `AutenticarService` como `AutenticarUseCase`).

## Tests

- [ ] T020 Unit `AutenticarServiceTest` (MockK): credenciais válidas → token; senha incorreta → falha; usuário inexistente → mesma falha genérica; usuário inativo → falha.
- [ ] T021 [P] Unit `JwtTokenProviderTest`: emitir→validar (round-trip); token expirado → inválido; assinatura adulterada → inválido.
- [ ] T022 [P] Unit `BCryptCodificadorDeSenhaTest`: `confere` true p/ hash da própria senha; false caso contrário.
- [ ] T023 Integração `AutenticacaoIntegrationTest` (`bootstrap`, Testcontainers): login demo → 200 + token; inválido → 401; rota `/api/**` sem token → 401; com token válido → não-401; rota pública (`/api/ping`) → 200.

## Documentação

- [ ] T024 README: seção **Autenticação** com credenciais de demonstração e exemplo de login + uso do token.

## Finalize

- [ ] T025 Rodar `./gradlew build` (unit + integração + gate Jacoco ≥ 80%) e corrigir falhas.
- [ ] T026 Abrir PR (`## Objetivo / O que entra / Como validar / Fora de escopo` + `Closes #17`).
