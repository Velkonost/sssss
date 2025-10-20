package velkonost.aifuturesbot.configs

/**
 * Secure configuration provider for Linux/JVM environments.
 *
 * Reads secrets exclusively from process environment variables or JVM system properties.
 * Never commit secrets to the repository. Prefer setting variables via the host OS or CI secrets.
 */
interface SecureConfigProvider {
    fun getRequired(name: String): String
    fun getOptional(name: String, defaultValue: String? = null): String?
}

class EnvSecureConfigProvider(
    private val keyPrefix: String = "AIFB_"
) : SecureConfigProvider {

    override fun getRequired(name: String): String {
        val resolved = resolve(name)
        require(!resolved.isNullOrBlank()) {
            "Missing required secret: ${effectiveKey(name)}"
        }
        return resolved!!
    }

    override fun getOptional(name: String, defaultValue: String?): String? {
        return resolve(name) ?: defaultValue
    }

    private fun resolve(name: String): String? {
        val key = effectiveKey(name)
        // Priority: Env var -> JVM system property
        return System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: System.getProperty(key)?.takeIf { it.isNotBlank() }
    }

    private fun effectiveKey(name: String): String =
        if (name.startsWith(keyPrefix)) name else keyPrefix + name
}

/**
 * Canonical secret keys used across the application. Keep names uppercase and without prefix.
 * The provider will apply the configured prefix automatically (default: AIFB_).
 */
object SecretKeys {
    const val BINANCE_API_KEY = "BINANCE_API_KEY"
    const val BINANCE_API_SECRET = "BINANCE_API_SECRET"
    const val TELEGRAM_BOT_TOKEN = "TELEGRAM_BOT_TOKEN"

    const val POSTGRES_URL = "POSTGRES_URL" // e.g. jdbc:postgresql://host:5432/db
    const val POSTGRES_USER = "POSTGRES_USER"
    const val POSTGRES_PASSWORD = "POSTGRES_PASSWORD"
    
    // SSL configuration for PostgreSQL
    const val POSTGRES_SSL_CERT_PATH = "POSTGRES_SSL_CERT_PATH" // Path to SSL certificate file
    const val POSTGRES_SSL_MODE = "POSTGRES_SSL_MODE" // SSL mode: verify-full, require, etc.
}


