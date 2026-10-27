plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.owasp.dependencycheck)
}

subprojects {
    group = "com.fiap.soat.oficina"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

dependencyCheck {
    formats = listOf("HTML", "JSON", "SARIF")
    nvd {
        apiKey = System.getenv("NVD_API_KEY")
    }
}
