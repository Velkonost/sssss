plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories { mavenCentral() }

dependencies {
    implementation(project(":core:common"))
    
    // Kotlinx Serialization for JSON schemas
    implementation(libs.kotlinx.serialization.json)
    
    // Validation libraries
    implementation(libs.hibernate.validator)
    implementation(libs.jakarta.el)
    
    // Testing
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)
}
