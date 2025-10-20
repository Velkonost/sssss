package velkonost.aifuturesbot.db

import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.assertEquals
import velkonost.aifuturesbot.db.repositories.SignalsRepository
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Requires Testcontainers or external PostgreSQL")
class SignalsRepositoryIT {

    private lateinit var db: Database
    private lateinit var repo: SignalsRepository

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
        repo = SignalsRepository(db)
    }

    @Test
    fun insertAndQueryByRange() {
        val s1 = SignalsRepository.Signal(
            sourceType = "telegram",
            sourceId = "msg-1",
            symbol = "ETHUSDT",
            timeframe = "5m",
            signalType = "long",
            weight = BigDecimal("1.0"),
            confidence = BigDecimal("0.8"),
            payloadJson = "{\"text\":\"buy\"}"
        )
        val id = repo.insert(s1)
        require(id > 0)

        val list = repo.find("ETHUSDT", "5m", 0L, System.currentTimeMillis())
        assertEquals(1, list.size)
        assertEquals("long", list.first().signalType)
    }
}


