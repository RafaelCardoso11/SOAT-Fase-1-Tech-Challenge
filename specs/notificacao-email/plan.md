# Technical Plan: Notificação por E-mail (SMTP/Mailpit)

**Spec Ref:** `specs/notificacao-email/spec.md`
**Status:** Draft

## Summary
Implementa um adapter real do `NotificacaoPort` (definido no `ordemservico`) usando `JavaMailSender`
(Spring Mail) sobre SMTP. Em desenvolvimento o SMTP é o **Mailpit** (docker-compose). O adapter real é
ativado por `notificacao.email.enabled=true`; caso contrário o `NotificacaoPortStub` (no-op) continua o
padrão, de modo que os testes rodam sem SMTP. Troca a referência a MailHog por Mailpit (compose + docs).

## Requirement Mapping
| Spec Ref | Componente / Camada | Nota |
|---|---|---|
| `FR-001` | `adapter/outbound/notificacao/NotificacaoPorEmail` | monta e envia o `SimpleMailMessage` |
| `FR-002` | `bootstrap/.../application.yml` (`spring.mail.*`) + env `SMTP_HOST/SMTP_PORT` | SMTP configurável |
| `FR-003` | `NotificacaoPorEmail` (`@ConditionalOnProperty enabled=true`) + `NotificacaoPortStub` (default) | ativação por config |
| `FR-004` | `docker/docker-compose.yml` (serviço `mailpit`) | SMTP + UI de dev |

## Component Plan
| Camada / Área | Ação | Caminho | Notas |
|---|---|---|---|
| Build | Modificar | `gradle/libs.versions.toml` + `modules/ordemservico/build.gradle.kts` | add `spring-boot-starter-mail` |
| Adapter (out) | Criar | `modules/ordemservico/.../adapter/outbound/notificacao/NotificacaoPorEmail.kt` | `@Component @ConditionalOnProperty(notificacao.email.enabled=true)`; usa `JavaMailSender` |
| Adapter (out) | Modificar | `.../adapter/outbound/notificacao/NotificacaoPortStub.kt` | `@ConditionalOnProperty(enabled=false, matchIfMissing=true)` → padrão |
| Config | Modificar | `bootstrap/src/main/resources/application.yml` | `spring.mail.*` + `notificacao.email.*` |
| Infra | Modificar | `docker/docker-compose.yml` | `mailhog` → `mailpit` (axllent/mailpit), `NOTIFICACAO_EMAIL_ENABLED=true` |
| Docs | Modificar | `docs/arquitetura.md` (e DDD, se citar) | MailHog → Mailpit |

## Entry Point(s)
Sem novo endpoint. O gatilho é o `GerarOrcamentoService` chamando `NotificacaoPort.notificarOrcamentoGerado`.

## Data Model / Persistence
Nenhuma mudança de banco.

## Integrations & Observability
- **SMTP** via `JavaMailSender` (auto-config do Spring Boot quando `spring.mail.host` presente). Dev: Mailpit (`SMTP_HOST:SMTP_PORT` = `mailpit:1025`; UI em `:8025`).
- Envio best-effort; falha de SMTP não desfaz o orçamento (já persistido).

## Test Strategy
- **Unit (MockK):** `NotificacaoPorEmailTest` — com `JavaMailSender` mockado, verifica que `send` é chamado com remetente/assunto/corpo esperados (nº da OS + total).
- **Build:** os `@SpringBootTest` existentes rodam com `notificacao.email.enabled` ausente → stub; sem SMTP, seguem verdes.
- Verificação fim-a-fim (e-mail no Mailpit) é **manual** via docker-compose (critério de aceite), para não depender de imagem externa no CI.

## Quality / Lint Risks & Mitigation
- **Dois beans do mesmo port** → condições `@ConditionalOnProperty` mutuamente exclusivas (exatamente um ativo).
- **Autoconfig de mail em teste** → `JavaMailSender` é criado mas não conecta (lazy); stub é quem roda. Sem impacto.
- **Destinatário real ausente** → `shortcut:` no adapter: usa destinatário configurável até a integração com `cadastro`.

## Alternatives Considered
- **MailHog** (citado nos docs): descartado — projeto arquivado/sem manutenção. **Mailpit** é o sucessor mantido, mesmo papel.
- **Adapter no `bootstrap`:** descartado — o port e o stub vivem no `ordemservico`; o adapter real acompanha.

## Risks / Open Questions
- Sem retry em falha de SMTP (best-effort) — spec Open Questions.
- Destinatário configurável até `cadastro` fornecer o e-mail do cliente.
