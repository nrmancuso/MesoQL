plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":parser"))
    implementation(project(":core"))
    implementation(project(":ingestion"))
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter)
    implementation(libs.picocli.spring.boot.starter)
    implementation(libs.opensearch.java)
    implementation(libs.jline)
}

tasks.bootJar {
    archiveBaseName.set("mesoql")
}
