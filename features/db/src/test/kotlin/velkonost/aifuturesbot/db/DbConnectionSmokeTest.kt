package velkonost.aifuturesbot.db

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.jetbrains.exposed.sql.transactions.transaction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DbConnectionSmokeTest {

    @Test
    fun connect_whenEnabledByFlag() {
        val enabled = System.getenv("TEST_DB_SMOKE") == "true"
        assumeTrue(enabled, "Smoke test disabled. Set TEST_DB_SMOKE=true to enable.")

        assertDoesNotThrow {
            val db = DatabaseFactory.init(createSchema = false)
            transaction(db) { /* no-op */ }
        }
    }
}


