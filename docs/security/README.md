# Análise de Vulnerabilidades

A análise de dependências usa o **Trivy**, que varre a imagem Docker (pacotes do sistema) e as dependências
dentro do fat jar, incluindo as transitivas, contra a base de vulnerabilidades.

## Situação atual

Sem vulnerabilidades HIGH ou CRITICAL abertas na imagem nem no `app.jar`. Ficam só os dois riscos aceitos abaixo.
O relatório versionado é [`trivy-report.txt`](trivy-report.txt).

Algumas bibliotecas usam versão acima da que o Spring Boot fixa, porque o patch de segurança saiu depois. As versões
ficam em `gradle/libs.versions.toml` e valem para todos os módulos (`build.gradle.kts` raiz):

| Biblioteca (propriedade do BOM) | Motivo |
|---|---|
| Tomcat (`tomcat.version`) | CVE-2026-65182, CVE-2026-65637, CVE-2026-65905, CVE-2026-68525 |
| Jackson (`jackson-bom.version`) | CVE-2026-89407, CVE-2026-89425, CVE-2026-68497, CVE-2026-91776, CVE-2026-91777 |
| Driver PostgreSQL (`postgresql.version`) | CVE-2026-54291 |

Ao subir o Spring Boot, confira se o BOM novo já traz versões iguais ou maiores; se sim, remova a sobrescrita.

## Riscos aceitos

As duas CVEs estão no `spring-webmvc` 6.2.19, a última versão aberta da linha 6.2. A correção só existe
no Spring Framework 7.0.9, que exige migrar para Spring Boot 4. Nenhuma das duas é explorável nesta aplicação:

| CVE | Condição para explorar | Por que não se aplica |
|-----|------------------------|-----------------------|
| CVE-2026-47884 (SSRF/RCE) | Uso de `XsltView` com mapeamento `/**` que renderiza view sem nome explícito | A API é REST e só devolve JSON; não há `XsltView`, `ViewResolver` nem `ModelAndView` no código |
| CVE-2026-47890 (corrupção de stream) | Server-Sent Events com fragmentos de view | Não há SSE (`SseEmitter`) nem fragmentos de view |

Ficam no [`.trivyignore`](../../.trivyignore) e devem ser revistas na migração para Spring Boot 4. O relatório lista as duas na seção *Suppressed Vulnerabilities*, para ficarem visíveis.

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
