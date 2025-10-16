package velkonost.aifuturesbot.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import velkonost.aifuturesbot.configs.FileFirstSecureConfigProvider
import velkonost.aifuturesbot.configs.SecretKeys
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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
        val provider = FileFirstSecureConfigProvider()
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
        
        resolveSslCertPath(config.sslCertPath)?.let { certPath ->
            addDataSourceProperty("sslrootcert", certPath)
        }
        
        validate()
    }

    private fun resolveSslCertPath(rawPath: String?): String? {
        if (rawPath.isNullOrBlank()) return null
        return if (rawPath.startsWith("classpath:")) {
            val resourceName = rawPath.removePrefix("classpath:")
            resolveClasspathCert(resourceName)
        } else {
            resolveFileCert(rawPath)
        }
    }

    private fun resolveClasspathCert(resourceName: String): String? {
        val stream = this::class.java.classLoader.getResourceAsStream(resourceName) ?: return null
        val temp = Files.createTempFile("aifb-ca-", ".pem").toFile().apply { deleteOnExit() }
        stream.use { Files.copy(it, temp.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        return temp.absolutePath
    }

    private fun resolveFileCert(path: String): String? {
        val file = File(path)
        return if (file.exists()) file.absolutePath else null
    }
}


