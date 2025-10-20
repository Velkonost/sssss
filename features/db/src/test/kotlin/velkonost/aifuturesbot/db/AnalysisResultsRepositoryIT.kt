package velkonost.aifuturesbot.db

import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.assertTrue
import velkonost.aifuturesbot.db.repositories.AnalysisResultsRepository

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Requires Testcontainers or external PostgreSQL")
class AnalysisResultsRepositoryIT {

    private lateinit var db: Database
    private lateinit var repo: AnalysisResultsRepository

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
        repo = AnalysisResultsRepository(db)
    }

    @Test
    fun insertResult() {
        val id = repo.insert(
            AnalysisResultsRepository.Result(
                symbol = "BTCUSDT",
                timeframe = "15m",
                analysisType = "indicators",
                inputsJson = "{}",
                outputsJson = "{\"rsi\":30}"
            )
        )
        assertTrue(id > 0)
    }
}


