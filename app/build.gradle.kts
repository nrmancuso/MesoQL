plugins {
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation(project(":parser"))
    implementation(project(":core"))
    implementation(project(":ingestion"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("info.picocli:picocli-spring-boot-starter:4.7.5")
    implementation("org.opensearch.client:opensearch-java:2.6.0")
    implementation("org.jline:jline:3.27.1")
}

configurations {
    runtimeClasspath {
        exclude(group = "commons-logging", module = "commons-logging")
    }
}

tasks.bootJar {
    archiveBaseName.set("mesoql")
}
