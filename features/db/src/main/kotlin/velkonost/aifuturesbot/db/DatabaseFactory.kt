package velkonost.aifuturesbot.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import velkonost.aifuturesbot.configs.EnvSecureConfigProvider
import velkonost.aifuturesbot.configs.SecretKeys

object DatabaseFactory {
    fun init(createSchema: Boolean = true): Database {
        val provider = EnvSecureConfigProvider()
        val jdbcUrl = provider.getRequired(SecretKeys.POSTGRES_URL)
        val user = provider.getRequired(SecretKeys.POSTGRES_USER)
        val password = provider.getRequired(SecretKeys.POSTGRES_PASSWORD)
        return initWith(jdbcUrl, user, password, createSchema)
    }

    fun initWith(jdbcUrl: String, user: String, password: String, createSchema: Boolean = true): Database {
        val ds = HikariDataSource(hikari(jdbcUrl, user, password))
        val db = Database.connect(ds)

        if (createSchema) {
            transaction(db) {
                SchemaUtils.createMissingTablesAndColumns(
                    CandlesTable,
                    SignalsTable,
                    AggregatedSignalsTable,
                    AnalysisResultsTable,
                    AuditLogsTable
                )
            }
        }

        return db
    }

    private fun hikari(jdbcUrl: String, user: String, password: String): HikariConfig = HikariConfig().apply {
        jdbcUrl.also { this.jdbcUrl = it }
        username = user
        this.password = password
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
}


