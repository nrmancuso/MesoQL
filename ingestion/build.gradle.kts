dependencies {
    implementation(project(":core"))
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter)
    implementation(libs.opensearch.java)
    implementation(libs.jackson.databind)
    implementation(libs.opencsv)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
