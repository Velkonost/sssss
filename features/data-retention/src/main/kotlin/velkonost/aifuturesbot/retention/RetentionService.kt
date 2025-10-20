package velkonost.aifuturesbot.retention

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Главный сервис управления ретеншеном данных
 */
class RetentionService(
    private val config: RetentionConfig,
    private val archiveService: ArchiveService,
    private val cleanupService: DataCleanupService,
    private val scheduler: RetentionScheduler,
    private val monitoringService: RetentionMonitoringService
) {
    
    private val logger = LoggerFactory.getLogger(RetentionService::class.java)
    
    /**
     * Инициализация сервиса ретеншена
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        logger.info("Initializing retention service...")
        
        // Валидируем конфигурацию
        val validationErrors = RetentionConfigUtils.validate(config)
        if (validationErrors.isNotEmpty()) {
            logger.error("Retention configuration validation failed: ${validationErrors.joinToString(", ")}")
            throw IllegalArgumentException("Invalid retention configuration: ${validationErrors.joinToString(", ")}")
        }
        
        // Запускаем планировщик
        scheduler.start()
        
        logger.info("Retention service initialized successfully")
    }
    
    /**
     * Остановка сервиса ретеншена
     */
    suspend fun shutdown() = withContext(Dispatchers.IO) {
        logger.info("Shutting down retention service...")
        
        scheduler.stop()
        
        logger.info("Retention service shutdown completed")
    }
    
    /**
     * Выполнить полную очистку всех данных
     */
    suspend fun runFullCleanup(dryRun: Boolean = false): RetentionOperationResult = withContext(Dispatchers.IO) {
        logger.info("Starting full cleanup${if (dryRun) " (DRY RUN)" else ""}...")
        
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<CleanupResult>()
        
        try {
            // Получаем все типы данных с включенной очисткой
            val dataTypesToProcess = DataType.values().filter { config.isCleanupEnabled(it) }
            
            logger.info("Processing ${dataTypesToProcess.size} data types: ${dataTypesToProcess.joinToString()}")
            
            for (dataType in dataTypesToProcess) {
                try {
                    logger.info("Processing $dataType...")
                    
                    // Сначала архивируем теплые данные
                    val archiveResult = cleanupService.archiveAndCleanup(dataType, dryRun)
                    results.add(archiveResult)
                    monitoringService.recordCleanupOperation(archiveResult)
                    
                    // Затем очищаем холодные данные
                    val cleanupResult = cleanupService.cleanupExpiredData(dataType, dryRun)
                    results.add(cleanupResult)
                    monitoringService.recordCleanupOperation(cleanupResult)
                    
                    logger.info("Completed $dataType: archived=${archiveResult.recordsArchived}, deleted=${cleanupResult.recordsDeleted}")
                    
                } catch (e: Exception) {
                    logger.error("Failed to process $dataType", e)
                    val errorResult = CleanupResult(
                        dataType = dataType,
                        recordsProcessed = 0,
                        recordsArchived = 0,
                        recordsDeleted = 0,
                        executionTimeMs = 0,
                        success = false,
                        error = e.message
                    )
                    results.add(errorResult)
                    monitoringService.recordCleanupOperation(errorResult)
                }
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            val totalProcessed = results.sumOf { it.recordsProcessed }
            val totalArchived = results.sumOf { it.recordsArchived }
            val totalDeleted = results.sumOf { it.recordsDeleted }
            val successCount = results.count { it.success }
            
            logger.info("Full cleanup completed${if (dryRun) " (DRY RUN)" else ""}: " +
                "processed=$totalProcessed, archived=$totalArchived, deleted=$totalDeleted, " +
                "success=$successCount/${results.size}, time=${executionTime}ms")
            
            RetentionOperationResult(
                operationType = "FULL_CLEANUP",
                dataTypesProcessed = dataTypesToProcess,
                results = results,
                totalExecutionTimeMs = executionTime,
                success = successCount == results.size
            )
            
        } catch (e: Exception) {
            logger.error("Full cleanup failed", e)
            RetentionOperationResult(
                operationType = "FULL_CLEANUP",
                dataTypesProcessed = emptyList(),
                results = results,
                totalExecutionTimeMs = System.currentTimeMillis() - startTime,
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Выполнить очистку для конкретного типа данных
     */
    suspend fun runCleanupForDataType(dataType: DataType, dryRun: Boolean = false): RetentionOperationResult = withContext(Dispatchers.IO) {
        logger.info("Starting cleanup for $dataType${if (dryRun) " (DRY RUN)" else ""}...")
        
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<CleanupResult>()
        
        try {
            if (!config.isCleanupEnabled(dataType)) {
                logger.warn("Cleanup is disabled for $dataType")
                return@withContext RetentionOperationResult(
                    operationType = "SINGLE_CLEANUP",
                    dataTypesProcessed = listOf(dataType),
                    results = emptyList(),
                    totalExecutionTimeMs = System.currentTimeMillis() - startTime,
                    success = true,
                    error = "Cleanup disabled for $dataType"
                )
            }
            
            // Архивируем теплые данные
            val archiveResult = cleanupService.archiveAndCleanup(dataType, dryRun)
            results.add(archiveResult)
            monitoringService.recordCleanupOperation(archiveResult)
            
            // Очищаем холодные данные
            val cleanupResult = cleanupService.cleanupExpiredData(dataType, dryRun)
            results.add(cleanupResult)
            monitoringService.recordCleanupOperation(cleanupResult)
            
            val executionTime = System.currentTimeMillis() - startTime
            val success = results.all { it.success }
            
            logger.info("Cleanup completed for $dataType: " +
                "archived=${archiveResult.recordsArchived}, deleted=${cleanupResult.recordsDeleted}, " +
                "time=${executionTime}ms, success=$success")
            
            RetentionOperationResult(
                operationType = "SINGLE_CLEANUP",
                dataTypesProcessed = listOf(dataType),
                results = results,
                totalExecutionTimeMs = executionTime,
                success = success
            )
            
        } catch (e: Exception) {
            logger.error("Cleanup failed for $dataType", e)
            RetentionOperationResult(
                operationType = "SINGLE_CLEANUP",
                dataTypesProcessed = listOf(dataType),
                results = results,
                totalExecutionTimeMs = System.currentTimeMillis() - startTime,
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Получить статистику по всем типам данных
     */
    suspend fun getDataStatistics(): Map<DataType, DataStatistics> = withContext(Dispatchers.IO) {
        logger.debug("Retrieving data statistics...")
        
        val statistics = mutableMapOf<DataType, DataStatistics>()
        
        DataType.values().forEach { dataType ->
            try {
                val stats = cleanupService.getDataStatistics(dataType)
                statistics[dataType] = stats
            } catch (e: Exception) {
                logger.error("Failed to get statistics for $dataType", e)
            }
        }
        
        statistics
    }
    
    /**
     * Получить метрики ретеншена
     */
    suspend fun getRetentionMetrics(): RetentionMetrics = withContext(Dispatchers.IO) {
        monitoringService.getRetentionMetrics()
    }
    
    /**
     * Получить информацию об использовании хранилища
     */
    suspend fun getStorageUsage(): StorageUsage = withContext(Dispatchers.IO) {
        monitoringService.getStorageUsage()
    }
    
    /**
     * Получить тренд роста данных
     */
    suspend fun getDataGrowthTrend(dataType: DataType, days: Int = 30): List<DataGrowthPoint> = withContext(Dispatchers.IO) {
        monitoringService.getDataGrowthTrend(dataType, days)
    }
    
    /**
     * Проверить статус планировщика
     */
    fun isSchedulerRunning(): Boolean = scheduler.isRunning()
    
    /**
     * Запустить очистку вручную
     */
    suspend fun runManualCleanup(dataType: DataType? = null, dryRun: Boolean = false): List<CleanupResult> = withContext(Dispatchers.IO) {
        scheduler.runCleanupNow(dataType, dryRun)
    }
}

/**
 * Результат операции ретеншена
 */
data class RetentionOperationResult(
    val operationType: String,
    val dataTypesProcessed: List<DataType>,
    val results: List<CleanupResult>,
    val totalExecutionTimeMs: Long,
    val success: Boolean,
    val error: String? = null
) {
    val totalRecordsProcessed: Int get() = results.sumOf { it.recordsProcessed }
    val totalRecordsArchived: Int get() = results.sumOf { it.recordsArchived }
    val totalRecordsDeleted: Int get() = results.sumOf { it.recordsDeleted }
    val successCount: Int get() = results.count { it.success }
    val failureCount: Int get() = results.count { !it.success }
}
