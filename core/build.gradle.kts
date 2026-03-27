plugins {
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.4")
    }
}

dependencies {
    implementation(project(":parser"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.opensearch.client:opensearch-java:2.6.0")
    implementation("org.opensearch.client:opensearch-rest-client:2.11.0")
    implementation("org.apache.httpcomponents.core5:httpcore5:5.2.4")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
