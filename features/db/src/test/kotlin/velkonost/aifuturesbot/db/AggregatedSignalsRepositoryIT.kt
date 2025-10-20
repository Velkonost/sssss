package velkonost.aifuturesbot.db

import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.assertEquals
import velkonost.aifuturesbot.db.repositories.AggregatedSignalsRepository
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Requires Testcontainers or external PostgreSQL")
class AggregatedSignalsRepositoryIT {

    private lateinit var db: Database
    private lateinit var repo: AggregatedSignalsRepository

    @BeforeAll
    fun setup() {
        val tc = System.getenv("TEST_USE_TC") == "true"
        db = if (tc) {
            val container = org.testcontainers.containers.PostgreSQLContainer("postgres:16-alpine").apply { start() }
            DatabaseFactory.initWith(
                DatabaseConfig(
                    jdbcUrl = container.jdbcUrl,
                    user = container.username,
                    password = container.password,
                    createSchema = true
                )
            )
        } else {
            val url = System.getenv("AIFB_POSTGRES_URL") ?: error("AIFB_POSTGRES_URL not set")
            val user = System.getenv("AIFB_POSTGRES_USER") ?: error("AIFB_POSTGRES_USER not set")
            val pass = System.getenv("AIFB_POSTGRES_PASSWORD") ?: error("AIFB_POSTGRES_PASSWORD not set")
            DatabaseFactory.initWith(
                DatabaseConfig(
                    jdbcUrl = url,
                    user = user,
                    password = pass,
                    createSchema = true
                )
            )
        }
        repo = AggregatedSignalsRepository(db)
    }

    @Test
    fun upsertAndGetLatest() {
        val list = listOf(
            AggregatedSignalsRepository.Aggregated(
                symbol = "BTCUSDT",
                timeframe = "1h",
                windowStart = 0L,
                windowEnd = 1000L,
                score = BigDecimal("0.5"),
                decision = "hold",
                basisJson = "{}"
            ),
            AggregatedSignalsRepository.Aggregated(
                symbol = "BTCUSDT",
                timeframe = "1h",
                windowStart = 1000L,
                windowEnd = 2000L,
                score = BigDecimal("0.7"),
                decision = "long",
                basisJson = "{}"
            )
        )
        repo.upsertIgnore(list)
        val latest = repo.latest("BTCUSDT", "1h")
        assertEquals("long", latest?.decision)
        assertEquals(BigDecimal("0.7"), latest?.score)
    }
}


