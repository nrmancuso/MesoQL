import com.github.spotbugs.snom.SpotBugsTask
import org.gradle.api.plugins.quality.Checkstyle

plugins {
    antlr
    idea
}

dependencies {
    antlr("org.antlr:antlr4:4.13.1")
    api("org.antlr:antlr4-runtime:4.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
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
