package velkonost.aifuturesbot.retention

import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

/**
 * Конфигурация политик ретеншена для различных типов данных
 */
@Serializable
data class RetentionPolicy(
    val dataType: DataType,
    val hotRetentionDays: Int,
    val warmRetentionDays: Int,
    val coldRetentionDays: Int,
    val archiveEnabled: Boolean = true,
    val cleanupEnabled: Boolean = true,
    val batchSize: Int = 1000
) {
    init {
        require(hotRetentionDays > 0) { "Hot retention must be positive" }
        require(warmRetentionDays >= hotRetentionDays) { "Warm retention must be >= hot retention" }
        require(coldRetentionDays >= warmRetentionDays) { "Cold retention must be >= warm retention" }
        require(batchSize > 0) { "Batch size must be positive" }
    }
    
    /**
     * Получить timestamp для горячих данных
     */
    fun getHotDataThreshold(): Long = 
        Instant.now().minus(Duration.ofDays(hotRetentionDays.toLong())).toEpochMilli()
    
    /**
     * Получить timestamp для теплых данных
     */
    fun getWarmDataThreshold(): Long = 
        Instant.now().minus(Duration.ofDays(warmRetentionDays.toLong())).toEpochMilli()
    
    /**
     * Получить timestamp для холодных данных
     */
    fun getColdDataThreshold(): Long = 
        Instant.now().minus(Duration.ofDays(coldRetentionDays.toLong())).toEpochMilli()
}

@Serializable
enum class DataType {
    /**
     * Сырые данные свечей - высокий объем, короткий ретеншен
     */
    RAW_CANDLES,
    
    /**
     * Агрегированные свечи (1h, 4h, 1d) - средний объем, средний ретеншен
     */
    AGGREGATED_CANDLES,
    
    /**
     * Торговые сигналы - средний объем, длинный ретеншен
     */
    SIGNALS,
    
    /**
     * Агрегированные сигналы - низкий объем, длинный ретеншен
     */
    AGGREGATED_SIGNALS,
    
    /**
     * Результаты анализа - средний объем, средний ретеншен
     */
    ANALYSIS_RESULTS,
    
    /**
     * Аудит логи - высокий объем, длинный ретеншен
     */
    AUDIT_LOGS
}

/**
 * Конфигурация архивирования
 */
@Serializable
data class ArchiveConfig(
    val enabled: Boolean = true,
    val storageType: StorageType = StorageType.FILE_SYSTEM,
    val compressionEnabled: Boolean = true,
    val encryptionEnabled: Boolean = false,
    val basePath: String = "/archive",
    val maxFileSizeMB: Int = 100,
    val retentionYears: Int = 7
) {
    init {
        require(maxFileSizeMB > 0) { "Max file size must be positive" }
        require(retentionYears > 0) { "Retention years must be positive" }
    }
}

@Serializable
enum class StorageType {
    FILE_SYSTEM,
    S3,
    AZURE_BLOB,
    GCS
}

/**
 * Конфигурация очистки БД
 */
@Serializable
data class CleanupConfig(
    val enabled: Boolean = true,
    val scheduleCron: String = "0 2 * * *", // Ежедневно в 2:00
    val batchSize: Int = 1000,
    val maxExecutionTimeMinutes: Int = 30,
    val dryRunEnabled: Boolean = false,
    val notificationEnabled: Boolean = true
) {
    init {
        require(batchSize > 0) { "Batch size must be positive" }
        require(maxExecutionTimeMinutes > 0) { "Max execution time must be positive" }
    }
}

/**
 * Глобальная конфигурация ретеншена
 */
@Serializable
data class RetentionConfig(
    val policies: Map<DataType, RetentionPolicy>,
    val archiveConfig: ArchiveConfig,
    val cleanupConfig: CleanupConfig,
    val monitoringEnabled: Boolean = true,
    val metricsEnabled: Boolean = true
) {
    init {
        require(policies.isNotEmpty()) { "At least one retention policy must be defined" }
    }
    
    /**
     * Получить политику для типа данных
     */
    fun getPolicy(dataType: DataType): RetentionPolicy? = policies[dataType]
    
    /**
     * Проверить, включено ли архивирование для типа данных
     */
    fun isArchiveEnabled(dataType: DataType): Boolean {
        val policy = getPolicy(dataType) ?: return false
        return archiveConfig.enabled && policy.archiveEnabled
    }
    
    /**
     * Проверить, включена ли очистка для типа данных
     */
    fun isCleanupEnabled(dataType: DataType): Boolean {
        val policy = getPolicy(dataType) ?: return false
        return cleanupConfig.enabled && policy.cleanupEnabled
    }
}

/**
 * Утилиты для работы с конфигурацией ретеншена
 */
object RetentionConfigUtils {
    
    /**
     * Создать конфигурацию по умолчанию
     */
    fun createDefaultConfig(): RetentionConfig {
        val policies = mapOf(
            DataType.RAW_CANDLES to RetentionPolicy(
                dataType = DataType.RAW_CANDLES,
                hotRetentionDays = 7,      // 7 дней горячих данных
                warmRetentionDays = 30,    // 30 дней теплых данных
                coldRetentionDays = 90,    // 90 дней холодных данных
                batchSize = 5000
            ),
            DataType.AGGREGATED_CANDLES to RetentionPolicy(
                dataType = DataType.AGGREGATED_CANDLES,
                hotRetentionDays = 30,     // 30 дней горячих данных
                warmRetentionDays = 90,    // 90 дней теплых данных
                coldRetentionDays = 365,   // 1 год холодных данных
                batchSize = 2000
            ),
            DataType.SIGNALS to RetentionPolicy(
                dataType = DataType.SIGNALS,
                hotRetentionDays = 30,     // 30 дней горячих данных
                warmRetentionDays = 90,    // 90 дней теплых данных
                coldRetentionDays = 365,   // 1 год холодных данных
                batchSize = 1000
            ),
            DataType.AGGREGATED_SIGNALS to RetentionPolicy(
                dataType = DataType.AGGREGATED_SIGNALS,
                hotRetentionDays = 90,     // 90 дней горячих данных
                warmRetentionDays = 365,   // 1 год теплых данных
                coldRetentionDays = 1095,  // 3 года холодных данных
                batchSize = 500
            ),
            DataType.ANALYSIS_RESULTS to RetentionPolicy(
                dataType = DataType.ANALYSIS_RESULTS,
                hotRetentionDays = 30,     // 30 дней горячих данных
                warmRetentionDays = 90,    // 90 дней теплых данных
                coldRetentionDays = 365,   // 1 год холодных данных
                batchSize = 1000
            ),
            DataType.AUDIT_LOGS to RetentionPolicy(
                dataType = DataType.AUDIT_LOGS,
                hotRetentionDays = 30,     // 30 дней горячих данных
                warmRetentionDays = 90,    // 90 дней теплых данных
                coldRetentionDays = 2555,  // 7 лет холодных данных (требования аудита)
                batchSize = 2000
            )
        )
        
        return RetentionConfig(
            policies = policies,
            archiveConfig = ArchiveConfig(
                enabled = true,
                storageType = StorageType.FILE_SYSTEM,
                compressionEnabled = true,
                encryptionEnabled = false,
                basePath = "/var/archive/ai-futures-bot",
                maxFileSizeMB = 100,
                retentionYears = 7
            ),
            cleanupConfig = CleanupConfig(
                enabled = true,
                scheduleCron = "0 2 * * *", // Ежедневно в 2:00
                batchSize = 1000,
                maxExecutionTimeMinutes = 30,
                dryRunEnabled = false,
                notificationEnabled = true
            ),
            monitoringEnabled = true,
            metricsEnabled = true
        )
    }
    
    /**
     * Валидация конфигурации
     */
    fun validate(config: RetentionConfig): List<String> {
        val errors = mutableListOf<String>()
        
        // Проверка политик
        DataType.values().forEach { dataType ->
            if (!config.policies.containsKey(dataType)) {
                errors.add("Missing retention policy for $dataType")
            }
        }
        
        // Проверка архивирования
        if (config.archiveConfig.enabled) {
            if (config.archiveConfig.basePath.isBlank()) {
                errors.add("Archive base path cannot be blank when archiving is enabled")
            }
        }
        
        // Проверка очистки
        if (config.cleanupConfig.enabled) {
            if (config.cleanupConfig.scheduleCron.isBlank()) {
                errors.add("Cleanup schedule cannot be blank when cleanup is enabled")
            }
        }
        
        return errors
    }
}
