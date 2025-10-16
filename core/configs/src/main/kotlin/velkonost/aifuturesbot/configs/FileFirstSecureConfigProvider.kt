package velkonost.aifuturesbot.configs

import java.io.File
import java.util.Properties

/**
 * Secure configuration provider that prefers values from a local properties file
 * and falls back to environment variables / JVM properties.
 *
 * This is intended for developer machines or single-node deployments where
 * a local, non-committed credentials file is used.
 */
class FileFirstSecureConfigProvider(
    credentialsFilePath: String = DEFAULT_FILE_PATH,
    private val delegate: SecureConfigProvider = EnvSecureConfigProvider()
) : SecureConfigProvider {

    private val properties: Properties by lazy {
        val resolvedPath = resolveCredentialsPath(credentialsFilePath)
        loadProperties(resolvedPath)
    }

    override fun getRequired(name: String): String {
        val fromFile = getFromFile(name)
        if (!fromFile.isNullOrBlank()) return fromFile
        return delegate.getRequired(name)
    }

    override fun getOptional(name: String, defaultValue: String?): String? {
        val fromFile = getFromFile(name)
        if (!fromFile.isNullOrBlank()) return fromFile
        return delegate.getOptional(name, defaultValue)
    }

    private fun getFromFile(name: String): String? {
        if (properties.isEmpty) return null
        // Accept both plain key and prefixed variant
        val raw = properties.getProperty(name) ?: properties.getProperty("AIFB_$name")
        return raw?.takeIf { it.isNotBlank() }
    }

    private fun loadProperties(path: String): Properties {
        val props = Properties()
        val file = File(path)
        if (file.exists() && file.isFile && file.canRead()) {
            file.inputStream().use { props.load(it) }
        }
        return props
    }

    private fun resolveCredentialsPath(defaultPath: String): String {
        var resolved: String? = null

        // Highest priority: explicit override via env or system property
        val override = System.getenv("AIFB_CREDENTIALS_FILE")
            ?: System.getProperty("AIFB_CREDENTIALS_FILE")
        if (!override.isNullOrBlank()) {
            resolved = override
        }

        // Try as-is relative to current working dir
        if (resolved == null) {
            val direct = File(defaultPath)
            if (direct.exists()) {
                resolved = direct.path
            }
        }

        // Search upwards up to 4 levels for configs/db/credentials.properties
        if (resolved == null) {
            val cwd = File(System.getProperty("user.dir"))
            var current: File? = cwd
            repeat(4) {
                val candidate = File(current, DEFAULT_FILE_PATH)
                if (candidate.exists()) {
                    resolved = candidate.path
                    return@repeat
                }
                current = current?.parentFile
            }
        }

        // Fallback to default
        return resolved ?: defaultPath
    }

    companion object {
        const val DEFAULT_FILE_PATH: String = "configs/db/credentials.properties"
    }
}


