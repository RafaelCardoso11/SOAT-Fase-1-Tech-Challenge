# ---------- Stage 1: build ----------
FROM gradle:8.10-jdk17 AS build
WORKDIR /app

# Cache de dependências: copia só os arquivos de build primeiro
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY shared-kernel/build.gradle.kts ./shared-kernel/
COPY bootstrap/build.gradle.kts ./bootstrap/
COPY modules ./modules

# Código-fonte
COPY . .

RUN gradle :bootstrap:bootJar --no-daemon

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd -r -u 1001 appuser
COPY --from=build /app/bootstrap/build/libs/*.jar /app/app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
