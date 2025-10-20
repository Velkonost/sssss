package velkonost.aifuturesbot.retention

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RetentionConfigTest {
    
    private val json = Json { prettyPrint = true }
    
    @Test
    fun `should create default retention config`() {
        val config = RetentionConfigUtils.createDefaultConfig()
        
        assertNotNull(config)
        assertTrue(config.policies.isNotEmpty())
        assertEquals(6, config.policies.size)
        assertTrue(config.archiveConfig.enabled)
        assertTrue(config.cleanupConfig.enabled)
    }
    
    @Test
    fun `should validate retention policy constraints`() {
        // Валидная политика
        val validPolicy = RetentionPolicy(
            dataType = DataType.RAW_CANDLES,
            hotRetentionDays = 7,
            warmRetentionDays = 30,
            coldRetentionDays = 90,
            batchSize = 1000
        )
        
        assertDoesNotThrow { validPolicy }
        
        // Невалидная политика - hot > warm
        assertThrows(IllegalArgumentException::class.java) {
            RetentionPolicy(
                dataType = DataType.RAW_CANDLES,
                hotRetentionDays = 30,
                warmRetentionDays = 7,
                coldRetentionDays = 90,
                batchSize = 1000
            )
        }
        
        // Невалидная политика - warm > cold
        assertThrows(IllegalArgumentException::class.java) {
            RetentionPolicy(
                dataType = DataType.RAW_CANDLES,
                hotRetentionDays = 7,
                warmRetentionDays = 90,
                coldRetentionDays = 30,
                batchSize = 1000
            )
        }
        
        // Невалидная политика - отрицательный hot retention
        assertThrows(IllegalArgumentException::class.java) {
            RetentionPolicy(
                dataType = DataType.RAW_CANDLES,
                hotRetentionDays = -1,
                warmRetentionDays = 30,
                coldRetentionDays = 90,
                batchSize = 1000
            )
        }
    }
    
    @Test
    fun `should calculate correct thresholds`() {
        val policy = RetentionPolicy(
            dataType = DataType.RAW_CANDLES,
            hotRetentionDays = 7,
            warmRetentionDays = 30,
            coldRetentionDays = 90,
            batchSize = 1000
        )
        
        val hotThreshold = policy.getHotDataThreshold()
        val warmThreshold = policy.getWarmDataThreshold()
        val coldThreshold = policy.getColdDataThreshold()
        
        assertTrue(hotThreshold < warmThreshold)
        assertTrue(warmThreshold < coldThreshold)
        
        // Проверяем что разница примерно соответствует дням
        val hotToWarmDiff = warmThreshold - hotThreshold
        val warmToColdDiff = coldThreshold - warmThreshold
        
        // Разница должна быть примерно 23 дня (30-7) и 60 дней (90-30)
        assertTrue(hotToWarmDiff > 20 * 24 * 60 * 60 * 1000L) // > 20 дней в мс
        assertTrue(hotToWarmDiff < 25 * 24 * 60 * 60 * 1000L) // < 25 дней в мс
        assertTrue(warmToColdDiff > 55 * 24 * 60 * 60 * 1000L) // > 55 дней в мс
        assertTrue(warmToColdDiff < 65 * 24 * 60 * 60 * 1000L) // < 65 дней в мс
    }
    
    @Test
    fun `should validate retention config`() {
        val config = RetentionConfigUtils.createDefaultConfig()
        val errors = RetentionConfigUtils.validate(config)
        
        assertTrue(errors.isEmpty(), "Default config should be valid, but got errors: ${errors.joinToString(", ")}")
    }
    
    @Test
    fun `should detect missing retention policy`() {
        val incompleteConfig = RetentionConfig(
            policies = mapOf(
                DataType.RAW_CANDLES to RetentionPolicy(
                    dataType = DataType.RAW_CANDLES,
                    hotRetentionDays = 7,
                    warmRetentionDays = 30,
                    coldRetentionDays = 90
                )
            ),
            archiveConfig = ArchiveConfig(),
            cleanupConfig = CleanupConfig()
        )
        
        val errors = RetentionConfigUtils.validate(incompleteConfig)
        
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Missing retention policy") })
    }
    
    @Test
    fun `should serialize and deserialize retention config`() {
        val originalConfig = RetentionConfigUtils.createDefaultConfig()
        
        val jsonString = json.encodeToString(originalConfig)
        assertNotNull(jsonString)
        assertTrue(jsonString.isNotEmpty())
        
        val deserializedConfig = json.decodeFromString<RetentionConfig>(jsonString)
        
        assertEquals(originalConfig.policies.size, deserializedConfig.policies.size)
        assertEquals(originalConfig.archiveConfig.enabled, deserializedConfig.archiveConfig.enabled)
        assertEquals(originalConfig.cleanupConfig.enabled, deserializedConfig.cleanupConfig.enabled)
    }
    
    @Test
    fun `should check archive and cleanup enabled flags`() {
        val config = RetentionConfigUtils.createDefaultConfig()
        
        // Проверяем что архивирование включено для всех типов данных
        DataType.values().forEach { dataType ->
            val policy = config.getPolicy(dataType)
            assertNotNull(policy, "Policy should exist for $dataType")
            
            if (policy != null) {
                assertEquals(config.isArchiveEnabled(dataType), config.archiveConfig.enabled && policy.archiveEnabled)
                assertEquals(config.isCleanupEnabled(dataType), config.cleanupConfig.enabled && policy.cleanupEnabled)
            }
        }
    }
    
    @Test
    fun `should handle archive config validation`() {
        // Валидная конфигурация архивирования
        val validArchiveConfig = ArchiveConfig(
            enabled = true,
            storageType = StorageType.FILE_SYSTEM,
            compressionEnabled = true,
            basePath = "/archive",
            maxFileSizeMB = 100,
            retentionYears = 7
        )
        
        assertDoesNotThrow { validArchiveConfig }
        
        // Невалидная конфигурация - пустой путь
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveConfig(
                enabled = true,
                basePath = "",
                maxFileSizeMB = 100,
                retentionYears = 7
            )
        }
        
        // Невалидная конфигурация - отрицательный размер файла
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveConfig(
                enabled = true,
                basePath = "/archive",
                maxFileSizeMB = -1,
                retentionYears = 7
            )
        }
    }
    
    @Test
    fun `should handle cleanup config validation`() {
        // Валидная конфигурация очистки
        val validCleanupConfig = CleanupConfig(
            enabled = true,
            scheduleCron = "0 2 * * *",
            batchSize = 1000,
            maxExecutionTimeMinutes = 30,
            dryRunEnabled = false,
            notificationEnabled = true
        )
        
        assertDoesNotThrow { validCleanupConfig }
        
        // Невалидная конфигурация - отрицательный batch size
        assertThrows(IllegalArgumentException::class.java) {
            CleanupConfig(
                enabled = true,
                scheduleCron = "0 2 * * *",
                batchSize = -1,
                maxExecutionTimeMinutes = 30
            )
        }
        
        // Невалидная конфигурация - отрицательное время выполнения
        assertThrows(IllegalArgumentException::class.java) {
            CleanupConfig(
                enabled = true,
                scheduleCron = "0 2 * * *",
                batchSize = 1000,
                maxExecutionTimeMinutes = -1
            )
        }
    }
}
