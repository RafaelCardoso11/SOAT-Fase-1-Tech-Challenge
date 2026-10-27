# Autenticação e proteção das APIs administrativas

**Issue:** #17
**Status:** Ready for Plan

## Overview / Problema

O sistema da oficina expõe operações administrativas (abrir OS, diagnóstico, orçamento, execução,
cadastros) que devem ser restritas aos usuários internos, enquanto o cliente só pode **consultar** o
status da OS de forma pública. Hoje nada é protegido. Precisamos autenticar os usuários internos para
que apenas pessoas identificadas executem operações administrativas.

## Goals & Non-Goals

**Goals:**
- Autenticar usuários internos por credenciais e emitir um token de acesso.
- Exigir token de acesso válido para qualquer operação administrativa.
- Manter a consulta pública de status da OS acessível sem autenticação.
- Prover usuários de demonstração para avaliação.

**Non-Goals:**
- Autorização granular por perfil/permissão — qualquer usuário autenticado acessa todas as operações administrativas.
- Registro público ou gestão (CRUD) de usuários.
- Renovação de sessão por refresh token.
- Recuperação de senha, múltiplo fator (MFA), SSO.

## User Stories

- Como **usuário interno**, quero autenticar com minhas credenciais e receber um token, para acessar as operações administrativas.
- Como **usuário interno**, quero que as operações administrativas exijam um token válido, para que apenas pessoas autenticadas as executem.
- Como **cliente**, quero consultar o status da minha OS sem precisar autenticar.
- Como **avaliador**, quero credenciais de demonstração prontas, para testar o sistema rapidamente.

Comportamento (BDD):
- Dadas credenciais válidas / Quando autentico / Então recebo um token de acesso.
- Dadas credenciais inválidas / Quando autentico / Então sou rejeitado e não recebo token.
- Dada uma operação administrativa sem token / Quando a solicito / Então é negada.
- Dada uma operação administrativa com token válido / Quando a solicito / Então é permitida.
- Dado um token expirado / Quando solicito uma operação administrativa / Então é negada.
- Dada a consulta pública de status / Quando a solicito sem token / Então é permitida.

## Functional Requirements

- `FR-001`: O sistema MUST autenticar um usuário a partir de credenciais (identificador + senha) e, quando válidas, emitir um token de acesso.
- `FR-002`: O sistema MUST rejeitar credenciais inválidas sem emitir token.
- `FR-003`: O sistema MUST exigir um token de acesso válido para executar qualquer operação administrativa.
- `FR-004`: O sistema MUST negar a operação administrativa quando o token estiver ausente, inválido ou expirado.
- `FR-005`: O sistema MUST permitir a consulta pública de status da OS sem autenticação.
- `FR-006`: O token de acesso MUST possuir prazo de expiração; expirado, MUST exigir nova autenticação.
- `FR-007`: O sistema MUST prover usuários de demonstração pré-provisionados na inicialização.
- `FR-008`: O sistema MUST armazenar as senhas de forma protegida (nunca em texto puro).
- `FR-009`: As falhas de autenticação MUST NOT revelar se o erro foi usuário inexistente ou senha incorreta (mensagem genérica).

## Non-Functional Requirements

- **Segurança:** senhas com hash; token assinado e com expiração; o segredo de assinatura fica fora do código (configuração/ambiente).
- **Restrição do enunciado:** o padrão do token de acesso é **JWT** — o mecanismo é detalhado na fase de plano.

## Edge Cases

- Credenciais ausentes ou vazias.
- Usuário inexistente vs. senha incorreta → resposta genérica (FR-009).
- Token ausente, malformado, com assinatura inválida ou expirado → operação negada.
- Operação pública permanece acessível mesmo se um token inválido for enviado.

## Acceptance Criteria

- [ ] Login com credenciais válidas retorna um token; credenciais inválidas são rejeitadas.
- [ ] Operação administrativa sem token, ou com token inválido/expirado, é negada; com token válido é permitida.
- [ ] Consulta pública de status acessível sem autenticação.
- [ ] Usuários de demonstração existem e estão documentados.
- [ ] Senhas não são armazenadas em texto puro.

## Open Questions

- `[NEEDS CLARIFICATION: formato do identificador de login — nome de usuário ou e-mail?]` (assumindo **nome de usuário** para o MVP; não bloqueia).
- `[NEEDS CLARIFICATION: quais usuários de demonstração e credenciais exatas?]` (definir no plano/implementação).
- `[NEEDS CLARIFICATION: prazo de expiração padrão do token?]` (configurável; sugestão de default: 60 minutos).
