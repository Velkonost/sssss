plugins {
    kotlin("jvm")
    application
}

repositories { mavenCentral() }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:configs"))
    implementation(project(":core:network"))
    implementation(project(":features:db"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.core)
    implementation(libs.bundles.exposed)
    implementation(libs.dataframe)

    implementation(libs.binance.sdk)
    implementation(libs.telegram.listener.sdk)
    implementation(libs.technical.analysis)

    implementation(libs.slf4j.simple)
}

application {
    mainClass.set("velkonost.aifuturesbot.app.MainKt")
}
