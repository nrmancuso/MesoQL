dependencies {
    implementation(project(":parser"))
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter)
    implementation(libs.bundles.opensearch.core)
    implementation(libs.httpcore5)
    implementation(libs.jackson.databind)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
