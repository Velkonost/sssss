plugins {
    kotlin("jvm")
}

repositories { mavenCentral() }

dependencies {
    api(project(":core:common"))
    api(libs.ktor.client.core)
    api(libs.ktor.client.cio)
    api(libs.ktor.client.logging)
}
