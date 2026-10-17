plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared-kernel"))
}

tasks.test {
    useJUnitPlatform()
}
