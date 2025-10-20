plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories { mavenCentral() }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":features:db"))
    implementation(project(":features:data-schemas"))
    
    // Kotlin/Coroutines
    implementation(libs.kotlinx.coroutines.core)
    
    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Koin for DI
    implementation(libs.koin.core)
    
    // Exposed for DB operations
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    
    // Testing
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)
}
