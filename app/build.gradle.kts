plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ingestion"))
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.graphql)
    implementation(libs.graphql.extended.scalars)
    testImplementation(libs.spring.boot.starter.test)
}

tasks.bootJar {
    archiveBaseName.set("mesoql")
}
