# Análise de Vulnerabilidades

A análise de dependências usa o **Trivy**, que varre as dependências (incluindo transitivas,
dentro do fat jar) contra a base de vulnerabilidades e gera um relatório.

## Rodar localmente

```bash
./gradlew :bootstrap:bootJar
trivy fs --scanners vuln --severity HIGH,CRITICAL bootstrap/build/libs
```

(Instale o Trivy: https://trivy.dev — ex.: `brew install trivy`.)

## CI

O workflow `.github/workflows/security.yml` roda o Trivy em push para `main`, semanalmente e sob demanda
(`workflow_dispatch`) e publica o resultado como artefato `trivy-report` do run. Não precisa de chave/segredo.

A `main` é protegida (só recebe PR), então o workflow não commita. Para atualizar a evidência versionada em
`docs/security/trivy-report.txt`, baixe o artefato do último run e abra um PR:

```bash
gh run download --name trivy-report --dir docs/security $(gh run list --workflow Security --limit 1 --json databaseId -q '.[0].databaseId')
```

> A action é pinada no commit SHA da v0.36.0 — as tags 0.0.1–0.34.2 do `trivy-action` foram comprometidas
> num ataque de supply-chain (mar/2026).

## Política

A verificação é informativa por padrão (`exit-code: 0`, não quebra o build). Para torná-la bloqueante,
mude `exit-code` para `1` no workflow (falha se houver vulnerabilidade HIGH/CRITICAL).
