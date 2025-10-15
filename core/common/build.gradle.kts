plugins {
    kotlin("jvm")
}

repositories { mavenCentral() }

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.koin.core)
}
