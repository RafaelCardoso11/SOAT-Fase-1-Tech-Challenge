# Análise de Vulnerabilidades

A análise de dependências do projeto usa o **OWASP Dependency-Check**, que cruza as dependências
(transitivas incluídas) com a base de CVEs da NVD e gera um relatório.

## Rodar localmente

```bash
./gradlew dependencyCheckAggregate
```

O relatório é gerado em:

```
build/reports/dependency-check-report.html   # leitura humana
build/reports/dependency-check-report.json   # máquina
build/reports/dependency-check-report.sarif  # integração com GitHub Security
```

## Chave da NVD (recomendado)

Sem chave, a NVD limita severamente as requisições e a primeira execução fica muito lenta.
Gere uma chave gratuita em https://nvd.nist.gov/developers/request-an-api-key e exporte:

```bash
export NVD_API_KEY=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
./gradlew dependencyCheckAggregate
```

No CI, cadastre a chave como secret do repositório com o nome `NVD_API_KEY`.

## CI

O workflow `.github/workflows/security.yml` roda o scan em push para `main`, semanalmente e sob demanda
(`workflow_dispatch`), publicando o relatório como artifact. Não roda em cada PR para manter o pipeline
de PR rápido.

## Política

A verificação é informativa por padrão (não quebra o build). Para transformar em gate bloqueante,
defina um limite de severidade em `build.gradle.kts`:

```kotlin
dependencyCheck {
    failBuildOnCVSS = 7.0f
}
```
