# Tasks: Notificação por E-mail (SMTP/Mailpit)

**Plan:** specs/notificacao-email/plan.md

## Build

- [ ] T001 - Adicionar `spring-boot-starter-mail` ao `gradle/libs.versions.toml` e como `implementation` em `modules/ordemservico/build.gradle.kts`

## Adapter

- [ ] T002 - Criar `adapter/outbound/notificacao/NotificacaoPorEmail` (`@Component @ConditionalOnProperty notificacao.email.enabled=true`; `JavaMailSender`; assunto/corpo com nº da OS + total; destinatário configurável — `shortcut:`)
- [ ] T003 - Em `NotificacaoPortStub`: `@ConditionalOnProperty(enabled=false, matchIfMissing=true)` para ser o padrão

## Config / Infra

- [ ] T004 - `application.yml`: `spring.mail.*` (host/porta via `SMTP_HOST/SMTP_PORT`) + `notificacao.email.{enabled,remetente,destinatario-padrao}`
- [ ] T005 - `docker/docker-compose.yml`: trocar `mailhog` por `mailpit` (axllent/mailpit, portas 1025/8025) e setar `NOTIFICACAO_EMAIL_ENABLED=true` no app
- [ ] T006 - Atualizar docs: `docs/arquitetura.md` (e DDD/README se citarem) MailHog → Mailpit

## Tests

- [ ] T007 - Unit `NotificacaoPorEmailTest` (JavaMailSender mockado; verifica envio com remetente/assunto/corpo)

## Finalize

- [ ] T008 - `./gradlew build` verde (testes usam o stub, sem SMTP)
- [ ] T009 - Verificação manual: `docker compose up` → gerar orçamento → e-mail aparece no Mailpit (:8025)
