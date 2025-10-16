package velkonost.aifuturesbot.db

import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import velkonost.aifuturesbot.db.repositories.AuditLogEntry
import velkonost.aifuturesbot.db.repositories.AuditLogRepository

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditLogRepositoryIT {

    private lateinit var db: Database
    private lateinit var repo: AuditLogRepository

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
        repo = AuditLogRepository(db)
    }

    @Test
    fun writeAuditLog() {
        assertDoesNotThrow {
            repo.log(
                AuditLogEntry(
                    eventType = "decision",
                    refType = "signal",
                    refId = 1L,
                    level = "INFO",
                    message = "Decision taken",
                    metaJson = "{}"
                )
            )
        }
    }
}


