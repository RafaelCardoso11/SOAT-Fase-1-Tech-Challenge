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
    // Monta o monolito: agrega todos os bounded contexts num único deployable.
    implementation(project(":shared-kernel"))
    implementation(project(":modules:autenticacao"))
    implementation(project(":modules:cadastro"))
    implementation(project(":modules:estoque"))
    implementation(project(":modules:ordemservico"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.springdoc.openapi.webmvc.ui)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.spring.boot.starter.test)
}

tasks.test {
    useJUnitPlatform()
}
