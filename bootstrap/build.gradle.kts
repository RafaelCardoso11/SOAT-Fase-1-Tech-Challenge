plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":modules:autenticacao"))
    implementation(project(":modules:cadastro"))
    implementation(project(":modules:estoque"))
    implementation(project(":modules:ordemservico"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.springdoc.openapi.webmvc.ui)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform.engine)
    testImplementation(libs.junit.platform.suite)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
    workingDir = rootProject.projectDir
    // docker-java (via Testcontainers) assume a API Docker 1.32 por padrão; o daemon do runner exige >= 1.40 (#60).
    systemProperty("api.version", "1.44")
}
