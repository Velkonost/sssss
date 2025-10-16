package velkonost.aifuturesbot.db

import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import velkonost.aifuturesbot.db.repositories.CandlesRepository
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CandlesRepositoryIT {

    private var container: PostgreSQLContainer<Nothing>? = null
    private lateinit var db: Database
    private lateinit var repo: CandlesRepository

    @BeforeAll
    fun setup() {
        val useTc = System.getenv("TEST_USE_TC") == "true"
        if (useTc) {
            container = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply { start() }
            db = DatabaseFactory.initWith(
                DatabaseConfig(
                    jdbcUrl = container!!.jdbcUrl,
                    user = container!!.username,
                    password = container!!.password,
                    createSchema = true
                )
            )
        } else {
            val url = System.getenv("AIFB_POSTGRES_URL") ?: error("AIFB_POSTGRES_URL not set")
            val user = System.getenv("AIFB_POSTGRES_USER") ?: error("AIFB_POSTGRES_USER not set")
            val pass = System.getenv("AIFB_POSTGRES_PASSWORD") ?: error("AIFB_POSTGRES_PASSWORD not set")
            db = DatabaseFactory.initWith(
                DatabaseConfig(
                    jdbcUrl = url,
                    user = user,
                    password = pass,
                    createSchema = true
                )
            )
        }
        repo = CandlesRepository(db)
    }

    @AfterAll
    fun tearDown() {
        container?.stop()
    }

    @Test
    fun insertAndQueryRange() {
        val items = listOf(
            CandlesRepository.Candle(
                symbol = "BTCUSDT",
                interval = "1m",
                openTime = 1000L,
                closeTime = 1600L,
                open = BigDecimal("100"),
                high = BigDecimal("110"),
                low = BigDecimal("90"),
                close = BigDecimal("105"),
                volume = BigDecimal("12.34"),
                source = "test"
            ),
            CandlesRepository.Candle(
                symbol = "BTCUSDT",
                interval = "1m",
                openTime = 2000L,
                closeTime = 2600L,
                open = BigDecimal("105"),
                high = BigDecimal("115"),
                low = BigDecimal("95"),
                close = BigDecimal("108"),
                volume = BigDecimal("10.00"),
                source = "test"
            )
        )

        repo.batchInsertIgnore(items)

        val result = repo.findRange("BTCUSDT", "1m", 1000L, 3000L)
        assertEquals(2, result.size)
        assertEquals(1000L, result.first().openTime)
        assertEquals(2000L, result.last().openTime)
    }
}
