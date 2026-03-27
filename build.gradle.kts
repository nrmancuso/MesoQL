import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("com.github.spotbugs") version "6.2.4" apply false
}

allprojects {
    group = "com.mesoql"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "checkstyle")
    apply(plugin = "com.github.spotbugs")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    configure<CheckstyleExtension> {
        toolVersion = "13.3.0"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
    }

    configure<SpotBugsExtension> {
        toolVersion = "4.9.3"
        effort = Effort.DEFAULT
        reportLevel = Confidence.DEFAULT
        omitVisitors = listOf("FormatStringChecker")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.named("check") {
        dependsOn("spotbugsMain")
    }
}
