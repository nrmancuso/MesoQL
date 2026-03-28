import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    dependsOn(":app:bootJar")
    systemProperty("mesoql.repo.root", rootProject.projectDir.absolutePath)
    systemProperty("mesoql.jar.path", rootProject.file("app/build/libs/mesoql-0.1.0.jar").absolutePath)
    testLogging {
        events("started", "passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
    onlyIf {
        val requestedTasks = gradle.startParameter.taskNames
        requestedTasks.any { requested ->
            requested == ":integration-tests:test" || requested == "integration-tests:test"
        }
    }
}
