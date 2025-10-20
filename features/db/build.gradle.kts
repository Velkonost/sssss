plugins {
    kotlin("jvm")
}

repositories { mavenCentral() }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:configs"))

    implementation(libs.bundles.exposed)
    implementation(libs.postgres.driver)
    implementation(libs.hikari.cp)

    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
}

tasks.test {
    useJUnitPlatform()
}
