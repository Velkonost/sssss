plugins {
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
    id("io.gitlab.arturbosch.detekt") version libs.versions.detekt apply false
}

repositories {
    mavenCentral()
}

subprojects {
    repositories {
        mavenCentral()
    }

    // Apply Detekt to all Kotlin modules
    plugins.apply("io.gitlab.arturbosch.detekt")

    // Configure Detekt
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension>("detekt") {
        buildUponDefaultConfig = true
        config.setFrom(files(rootProject.file("detekt.yml")))
        parallel = true
    }
}
