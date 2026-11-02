# Análise de Vulnerabilidades

A análise de dependências usa o **Trivy**, que varre a imagem Docker (pacotes do sistema) e as dependências
dentro do fat jar, incluindo as transitivas, contra a base de vulnerabilidades.

## Resultado

Severidades HIGH e CRITICAL, no `app.jar` da aplicação. A imagem base (`eclipse-temurin:17-jre`) não tem nenhuma.

| | Spring Boot | HIGH | CRITICAL | Total |
|---|---|---|---|---|
| Antes (`trivy-report` de 02/11/2026) | 3.3.5 | 34 | 9 | 43 |
| Depois | 3.5.16 + patches | 0 | 2 (riscos aceitos, abaixo) | 2 |

O que foi feito:

- **Spring Boot 3.3.5 → 3.5.16**, com springdoc 2.8.17 e plugin de dependency-management 1.1.7. Corrige Spring
  Framework, Spring Security, Spring Data, Micrometer e a maior parte do Tomcat e do Jackson.
- **Versões acima do BOM do Spring Boot**, com patches de segurança da mesma linha (em `gradle/libs.versions.toml`,
  aplicadas a todos os módulos no `build.gradle.kts` raiz):

  | Biblioteca | BOM 3.5.16 | Usada | CVEs corrigidas |
  |---|---|---|---|
  | Tomcat (`tomcat.version`) | 10.1.55 | 10.1.61 | 4 CRITICAL (CVE-2026-65182, CVE-2026-65637, CVE-2026-65905, CVE-2026-68525) |
  | Jackson (`jackson-bom.version`) | 2.21.4 | 2.21.8 | 5 HIGH (CVE-2026-89407, CVE-2026-89425, CVE-2026-68497, CVE-2026-91776, CVE-2026-91777) |
  | Driver PostgreSQL (`postgresql.version`) | 42.7.11 | 42.7.14 | 1 HIGH (CVE-2026-54291) |

## Riscos aceitos

As duas CVEs restantes estão no `spring-webmvc` 6.2.19, a última versão aberta da linha 6.2. A correção só existe
no Spring Framework 7.0.9, que exige migrar para Spring Boot 4. Nenhuma das duas é explorável nesta aplicação:

| CVE | Condição para explorar | Por que não se aplica |
|-----|------------------------|-----------------------|
| CVE-2026-47884 (SSRF/RCE) | Uso de `XsltView` com mapeamento `/**` que renderiza view sem nome explícito | A API é REST e só devolve JSON; não há `XsltView`, `ViewResolver` nem `ModelAndView` no código |
| CVE-2026-47890 (corrupção de stream) | Server-Sent Events com fragmentos de view | Não há SSE (`SseEmitter`) nem fragmentos de view |

Ficam no [`.trivyignore`](../../.trivyignore) e devem ser revistas na migração para Spring Boot 4.

## Rodar localmente

```bash
./gradlew :bootstrap:bootJar
trivy rootfs --scanners vuln --severity HIGH,CRITICAL bootstrap/build/libs
```

Use `rootfs`: o `trivy fs` não abre arquivos `.jar` e mostra zero vulnerabilidades sem ter analisado nada. Rodado
na raiz do projeto, o Trivy aplica o `.trivyignore`. Instale com `brew install trivy` (ou veja https://trivy.dev).

## CI

O workflow `.github/workflows/security.yml` roda o Trivy na imagem Docker em push para `main`, semanalmente e sob
demanda (`workflow_dispatch`), e publica o resultado como artefato `trivy-report` do run. Não precisa de
chave/segredo.

A `main` é protegida (só recebe PR), então o workflow não commita. Para atualizar a evidência versionada em
`docs/security/trivy-report.txt`, baixe o artefato do último run e abra um PR:

```bash
gh run download --name trivy-report --dir docs/security $(gh run list --workflow Security --limit 1 --json databaseId -q '.[0].databaseId')
```

> A action é pinada no commit SHA da v0.36.0 — as tags 0.0.1–0.34.2 do `trivy-action` foram comprometidas
> num ataque de supply-chain (mar/2026).

## Política

A verificação é **bloqueante** (`exit-code: 1`): o workflow falha se aparecer uma vulnerabilidade HIGH ou CRITICAL
fora do `.trivyignore`. Para aceitar um risco novo, registre a CVE no `.trivyignore` e a justificativa na tabela de
riscos aceitos acima.
