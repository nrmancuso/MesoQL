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

sourceSets {
    main {
        java {
            srcDir("src/generated/antlr")
        }
    }
}
