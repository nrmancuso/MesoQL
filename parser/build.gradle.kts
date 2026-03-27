import com.github.spotbugs.snom.SpotBugsTask
import org.gradle.api.plugins.quality.Checkstyle

plugins {
    antlr
    idea
}

dependencies {
    antlr(libs.antlr)
    api(libs.antlr.runtime)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.generateGrammarSource {
    arguments = listOf("-visitor", "-no-listener", "-package", "com.mesoql.parser")
    outputDirectory = file("src/generated/antlr")
}

tasks.compileJava {
    dependsOn(tasks.generateGrammarSource)
}

tasks.named<Checkstyle>("checkstyleMain") {
    source = fileTree("src/main/java")
}

tasks.named<SpotBugsTask>("spotbugsMain") {
    excludeFilter.set(rootProject.file("config/spotbugs/parser-generated-excludes.xml"))
    onlyAnalyze.addAll(
        "com.mesoql.ast.QueryAST*",
        "com.mesoql.parser.ThrowingErrorListener",
        "com.mesoql.parser.MesoQLParserHelper",
        "com.mesoql.parser.MesoQLSyntaxException"
    )
}

sourceSets {
    main {
        java {
            srcDir("src/generated/antlr")
        }
    }
}
