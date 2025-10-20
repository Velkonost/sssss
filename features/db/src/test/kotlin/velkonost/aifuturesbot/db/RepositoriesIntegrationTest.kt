package velkonost.aifuturesbot.db

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import velkonost.aifuturesbot.db.repositories.CandlesRepository
import java.math.BigDecimal

@Disabled("Requires Testcontainers or external PostgreSQL")
class RepositoriesIntegrationTest {
    companion object {
        private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
        private lateinit var db: org.jetbrains.exposed.sql.Database

        @JvmStatic
        @BeforeAll
        fun setup() {
            postgres.start()
            val url = postgres.jdbcUrl
            val user = postgres.username
            val pass = postgres.password
            db = DatabaseFactory.init(createSchema = true).also {
                // Override default by connecting directly (bypassing env provider) for tests
                org.jetbrains.exposed.sql.Database.connect(url, user = user, password = pass)
            }
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            postgres.stop()
        }
    }

    @Test
    fun insertAndReadCandles() {
        val repo = CandlesRepository(
            org.jetbrains.exposed.sql.Database.connect(
                postgres.jdbcUrl,
                user = postgres.username,
                password = postgres.password
            )
        )
        repo.batchInsertIgnore(
            listOf(
                CandlesRepository.Candle(
                    symbol = "BTCUSDT",
                    interval = "1m",
                    openTime = 1L,
                    closeTime = 60L,
                    open = BigDecimal("100"),
                    high = BigDecimal("110"),
                    low = BigDecimal("90"),
                    close = BigDecimal("105"),
                    volume = BigDecimal("1.23"),
                    source = "binance"
                )
            )
        )
        val out = repo.findRange("BTCUSDT", "1m", 1L, 1L)
        assertEquals(1, out.size)
        assertEquals(BigDecimal("105"), out.first().close)
    }
}
