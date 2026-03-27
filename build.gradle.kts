import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.spotbugs) apply false
}

allprojects {
    group = "com.mesoql"
    version = "0.1.0"
}

subprojects {
    val libsCatalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

    apply(plugin = "java")
    apply(plugin = "checkstyle")
    apply(plugin = "com.github.spotbugs")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    configure<CheckstyleExtension> {
        toolVersion = libsCatalog.findVersion("checkstyle-tool").get().requiredVersion
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
    }

    configure<SpotBugsExtension> {
        toolVersion = libsCatalog.findVersion("spotbugs-tool").get().requiredVersion
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
