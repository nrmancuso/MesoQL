import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

dependencies {
    testImplementation(project(":app"))
    testImplementation(platform(libs.spring.boot.bom))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.web)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    systemProperty("mesoql.repo.root", rootProject.projectDir.absolutePath)
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
