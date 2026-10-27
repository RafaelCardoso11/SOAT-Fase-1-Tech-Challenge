import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    jacoco
}

tasks.withType<Test>().configureEach {
    finalizedBy(tasks.withType<JacocoReport>())
}

tasks.withType<JacocoReport>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.withType<JacocoCoverageVerification>().configureEach {
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    include("**/domain/**", "**/application/**")
                }
            },
        ),
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

plugins.withId("java") {
    tasks.named("check") {
        dependsOn(tasks.withType<JacocoCoverageVerification>())
    }
}
