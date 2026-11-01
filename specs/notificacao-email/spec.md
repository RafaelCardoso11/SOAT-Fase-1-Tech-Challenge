# Notificação por E-mail do Orçamento (SMTP/Mailpit)

**Issue:** #66 (Refs #39)
**Status:** Ready for Plan

## Overview / Problema
Quando o orçamento de uma OS é gerado (UC03), o cliente deve ser notificado. Hoje o `NotificacaoPort`
é um stub (no-op). Esta entrega implementa o envio **real por e-mail (SMTP)**, apanhado em
desenvolvimento por um **Mailpit** (SMTP fake + UI web), sem depender de um provedor externo.

## Goals & Non-Goals
**Goals:**
- Enviar um e-mail quando o orçamento é gerado, via SMTP (`JavaMailSender`).
- Rodar localmente com **Mailpit** no docker-compose (catcher de SMTP + UI).
- Manter os testes verdes sem SMTP: o envio real é ativado por configuração; o stub continua o padrão.

**Non-Goals:**
- Descobrir o **e-mail real do cliente** (depende da integração com `cadastro`) — usa um destinatário configurável por ora.
- Outros canais (SMS), outros eventos de notificação, templates HTML elaborados.
- Fechar o #39 (segue aberto pela integração com `estoque`).

## User Stories
- Como **cliente**, quero receber um e-mail quando meu orçamento for gerado, para saber que posso aprová-lo.
- Como **desenvolvedor**, quero ver os e-mails enviados numa UI local (Mailpit), para validar sem enviar de verdade.

## Functional Requirements
- `FR-001`: Ao gerar o orçamento, o sistema MUST enviar um e-mail de notificação (assunto + corpo com o nº da OS e o valor total).
- `FR-002`: O envio MUST usar SMTP configurável por ambiente (host/porta), apontando para o Mailpit em desenvolvimento.
- `FR-003`: O envio real MUST ser ativável por configuração (`notificacao.email.enabled`); quando desligado, o sistema MUST usar o stub (no-op), mantendo os testes sem SMTP.
- `FR-004`: O docker-compose MUST prover o Mailpit (SMTP + UI) e ligar a aplicação a ele.

## Non-Functional Requirements
- Sem segredos no repositório: host/porta/remetente via env (com defaults de dev).
- Falha no envio não deve corromper o estado da OS (o orçamento já foi persistido antes da notificação).

## Edge Cases
- SMTP indisponível → o envio falha; `[NEEDS CLARIFICATION]` sobre política de retry (ver Open Questions).
- `notificacao.email.enabled=false` (padrão de teste) → nenhum e-mail, stub ativo.

## Acceptance Criteria
- [ ] Com a app rodando via docker-compose, gerar um orçamento faz aparecer um e-mail no Mailpit (UI em :8025).
- [ ] Com o envio desligado, o build e os testes seguem verdes (stub).
- [ ] Docs atualizados: Mailpit no lugar de MailHog.

## Open Questions
- `[NEEDS CLARIFICATION: retry/falha de SMTP]` — assumido **sem retry** por ora (o orçamento já está persistido; a notificação é best-effort). Não bloqueia.
- `[NEEDS CLARIFICATION: e-mail do cliente]` — assumido **destinatário configurável** até a integração com `cadastro`. Não bloqueia.
