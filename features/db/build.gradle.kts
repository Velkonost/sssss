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
}
