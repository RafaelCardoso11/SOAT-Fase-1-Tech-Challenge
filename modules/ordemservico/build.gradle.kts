plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared-kernel"))

    // Contextos fornecedores (upstream): o ordemservico (core/downstream) só enxerga as
    // PORTAS DE ENTRADA publicadas por eles; a tradução acontece no adapter/outbound/acl.
    implementation(project(":modules:cadastro"))
    implementation(project(":modules:estoque"))
}

tasks.test {
    useJUnitPlatform()
}
