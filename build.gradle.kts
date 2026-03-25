plugins {
    java
    antlr
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.mesoql"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.13.1")
    implementation("org.antlr:antlr4-runtime:4.13.1")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("info.picocli:picocli-spring-boot-starter:4.7.5")
    implementation("org.opensearch.client:opensearch-java:2.6.0")
    implementation("org.opensearch.client:opensearch-rest-client:2.11.0")
    implementation("org.apache.httpcomponents.core5:httpcore5:5.2.4")
    implementation("com.opencsv:opencsv:5.9")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.generateGrammarSource {
    arguments = listOf("-visitor", "-no-listener", "-package", "com.mesoql.parser")
    outputDirectory = file("build/generated-sources/antlr/main/java/com/mesoql/parser")
}

tasks.compileJava {
    dependsOn(tasks.generateGrammarSource)
}

sourceSets {
    main {
        java {
            srcDir("build/generated-sources/antlr/main/java")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
