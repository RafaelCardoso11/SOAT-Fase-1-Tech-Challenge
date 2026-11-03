plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

subprojects {
    group = "com.fiap.soat.oficina"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }

    extra["tomcat.version"] = rootProject.libs.versions.tomcat.get()
    extra["jackson-bom.version"] = rootProject.libs.versions.jackson.get()
    extra["postgresql.version"] = rootProject.libs.versions.postgresql.get()
}
