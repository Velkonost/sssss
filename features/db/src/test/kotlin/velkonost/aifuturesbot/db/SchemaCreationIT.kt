package velkonost.aifuturesbot.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Requires Testcontainers or external PostgreSQL")
class SchemaCreationIT {

    private var container: PostgreSQLContainer<Nothing>? = null
    private lateinit var db: Database

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
                    createSchema = true,
                    sslCertPath = System.getenv("AIFB_POSTGRES_SSL_CERT_PATH"),
                    sslMode = System.getenv("AIFB_POSTGRES_SSL_MODE")
                )
            )
        }
    }

    @AfterAll
    fun tearDown() {
        container?.stop()
    }

    @Test
    fun tablesAreCreated() {
        val expectedTables = setOf(
            "candles",
            "signals",
            "aggregated_signals",
            "analysis_results",
            "audit_logs"
        )

        val existing = mutableSetOf<String>()
        transaction(db) {
            exec(
                """
                SELECT tablename FROM pg_catalog.pg_tables 
                WHERE schemaname = current_schema()
                """.trimIndent()
            ) { rs ->
                while (rs.next()) {
                    existing += rs.getString(1)
                }
            }
        }

        expectedTables.forEach { table ->
            assertTrue(existing.contains(table), "Table '$table' should exist, existing: $existing")
        }
    }
}


