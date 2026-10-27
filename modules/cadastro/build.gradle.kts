plugins {
    alias(libs.plugins.kotlin.jvm)
    id("oficina.jacoco-convention")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared-kernel"))

    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
