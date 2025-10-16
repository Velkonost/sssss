package velkonost.aifuturesbot.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import velkonost.aifuturesbot.configs.EnvSecureConfigProvider
import velkonost.aifuturesbot.configs.SecretKeys
import java.io.File

/**
 * Database configuration for connection setup
 */
data class DatabaseConfig(
    val jdbcUrl: String,
    val user: String,
    val password: String,
    val createSchema: Boolean = true,
    val sslCertPath: String? = null,
    val sslMode: String? = null
)

object DatabaseFactory {
    fun init(createSchema: Boolean = true): Database {
        val provider = EnvSecureConfigProvider()
        val config = DatabaseConfig(
            jdbcUrl = provider.getRequired(SecretKeys.POSTGRES_URL),
            user = provider.getRequired(SecretKeys.POSTGRES_USER),
            password = provider.getRequired(SecretKeys.POSTGRES_PASSWORD),
            createSchema = createSchema,
            sslCertPath = provider.getOptional(SecretKeys.POSTGRES_SSL_CERT_PATH),
            sslMode = provider.getOptional(SecretKeys.POSTGRES_SSL_MODE, "require")
        )
        return initWith(config)
    }

    fun initWith(config: DatabaseConfig): Database {
        val ds = HikariDataSource(hikari(config))
        val db = Database.connect(ds)

        if (config.createSchema) {
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

    private fun hikari(config: DatabaseConfig): HikariConfig = HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        username = config.user
        password = config.password
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        
        // SSL configuration
        if (config.sslMode != null) {
            addDataSourceProperty("sslmode", config.sslMode)
        }
        
        if (config.sslCertPath != null && File(config.sslCertPath).exists()) {
            addDataSourceProperty("sslcert", config.sslCertPath)
            addDataSourceProperty("sslrootcert", config.sslCertPath)
        }
        
        validate()
    }
}


