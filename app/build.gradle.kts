plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ingestion"))
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter)
}

tasks.bootJar {
    archiveBaseName.set("mesoql")
}
