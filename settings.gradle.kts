plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
    id("com.gradle.develocity") version "3.17.6"
}

develocity {
    buildScan {
        // Disable publishing build scans to avoid Terms of Use prompts in CI
        publishing.onlyIf { false }
        // Provide defaults to avoid accidental prompts in dev too
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}

rootProject.name = "ai-bot"

include(
    ":app",
    ":core:common",
    ":core:configs",
    ":core:network",
    ":features:db",
    ":features:data-schemas",
    ":features:logger",
    ":features:tg-sender",
    ":features:incoming-signals",
    ":features:outgoing-signals",
    ":features:market-data-handler",
    ":features:trade-time-control",
    ":features:symbol-bot",
    ":features:symbol-bot-final-decision",
    ":features:statistics",
    ":features:profit-calculation",
    ":features:open-trade-decision",
    ":features:reports",
    ":features:ta-strategies"
)
