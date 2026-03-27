plugins {
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.4")
    }
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.opensearch.client:opensearch-java:2.6.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.opencsv:opencsv:5.9")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
