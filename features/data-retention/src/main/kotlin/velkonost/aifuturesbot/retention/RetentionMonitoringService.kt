package velkonost.aifuturesbot.retention

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Интерфейс для мониторинга ретеншена
 */
interface RetentionMonitoringService {
    suspend fun recordCleanupOperation(result: CleanupResult)
    suspend fun recordArchiveOperation(result: ArchiveResult)
    suspend fun getRetentionMetrics(): RetentionMetrics
    suspend fun getDataGrowthTrend(dataType: DataType, days: Int = 30): List<DataGrowthPoint>
    suspend fun getStorageUsage(): StorageUsage
}

/**
 * Метрики ретеншена
 */
@Serializable
data class RetentionMetrics(
    val totalCleanupOperations: Long,
    val totalArchiveOperations: Long,
    val totalRecordsProcessed: Long,
    val totalRecordsArchived: Long,
    val totalRecordsDeleted: Long,
    val averageCleanupTimeMs: Double,
    val averageArchiveTimeMs: Double,
    val successRate: Double,
    val lastCleanupTime: Long?,
    val lastArchiveTime: Long?,
    val dataTypeMetrics: Map<DataType, DataTypeMetrics>
)

/**
 * Метрики по типу данных
 */
@Serializable
data class DataTypeMetrics(
    val dataType: DataType,
    val cleanupOperations: Long,
    val archiveOperations: Long,
    val recordsProcessed: Long,
    val recordsArchived: Long,
    val recordsDeleted: Long,
    val averageCleanupTimeMs: Double,
    val averageArchiveTimeMs: Double,
    val successRate: Double,
    val lastOperationTime: Long?
)

/**
 * Точка роста данных
 */
@Serializable
data class DataGrowthPoint(
    val timestamp: Long,
    val recordCount: Long,
    val sizeBytes: Long
)

/**
 * Использование хранилища
 */
@Serializable
data class StorageUsage(
    val totalArchives: Int,
    val totalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val compressionRatio: Double,
    val dataTypeUsage: Map<DataType, DataTypeStorageUsage>
)

/**
 * Использование хранилища по типу данных
 */
@Serializable
data class DataTypeStorageUsage(
    val dataType: DataType,
    val archiveCount: Int,
    val totalSizeBytes: Long,
    val averageFileSizeBytes: Long,
    val oldestArchive: Long?,
    val newestArchive: Long?
)

/**
 * Реализация сервиса мониторинга
 */
class InMemoryRetentionMonitoringService(
    private val archiveService: ArchiveService
) : RetentionMonitoringService {
    
    private val logger = LoggerFactory.getLogger(InMemoryRetentionMonitoringService::class.java)
    
    // Метрики
    private val totalCleanupOperations = AtomicLong(0)
    private val totalArchiveOperations = AtomicLong(0)
    private val totalRecordsProcessed = AtomicLong(0)
    private val totalRecordsArchived = AtomicLong(0)
    private val totalRecordsDeleted = AtomicLong(0)
    private val totalCleanupTimeMs = AtomicLong(0)
    private val totalArchiveTimeMs = AtomicLong(0)
    private val successfulCleanupOperations = AtomicLong(0)
    private val successfulArchiveOperations = AtomicLong(0)
    
    // Метрики по типам данных
    private val dataTypeMetrics = ConcurrentHashMap<DataType, DataTypeMetrics>()
    
    // История операций
    private val cleanupHistory = ConcurrentHashMap<String, CleanupResult>()
    private val archiveHistory = ConcurrentHashMap<String, ArchiveResult>()
    
    // История роста данных
    private val dataGrowthHistory = ConcurrentHashMap<DataType, MutableList<DataGrowthPoint>>()
    
    override suspend fun recordCleanupOperation(result: CleanupResult) = withContext(Dispatchers.IO) {
        totalCleanupOperations.incrementAndGet()
        totalRecordsProcessed.addAndGet(result.recordsProcessed.toLong())
        totalRecordsArchived.addAndGet(result.recordsArchived.toLong())
        totalRecordsDeleted.addAndGet(result.recordsDeleted.toLong())
        totalCleanupTimeMs.addAndGet(result.executionTimeMs)
        
        if (result.success) {
            successfulCleanupOperations.incrementAndGet()
        }
        
        // Обновляем метрики по типу данных
        updateDataTypeMetrics(result.dataType, result)
        
        // Сохраняем в историю
        val operationId = generateOperationId(result.dataType, "cleanup")
        cleanupHistory[operationId] = result
        
        logger.debug("Recorded cleanup operation for ${result.dataType}: ${result.recordsProcessed} processed")
    }
    
    override suspend fun recordArchiveOperation(result: ArchiveResult) = withContext(Dispatchers.IO) {
        totalArchiveOperations.incrementAndGet()
        totalArchiveTimeMs.addAndGet(System.currentTimeMillis()) // Приблизительное время
        
        if (result.success) {
            successfulArchiveOperations.incrementAndGet()
        }
        
        // Сохраняем в историю
        val operationId = generateOperationId(DataType.RAW_CANDLES, "archive") // TODO: определить тип из результата
        archiveHistory[operationId] = result
        
        logger.debug("Recorded archive operation: ${result.recordCount} records")
    }
    
    override suspend fun getRetentionMetrics(): RetentionMetrics = withContext(Dispatchers.IO) {
        val cleanupOps = totalCleanupOperations.get()
        val archiveOps = totalArchiveOperations.get()
        
        val avgCleanupTime = if (cleanupOps > 0) totalCleanupTimeMs.get().toDouble() / cleanupOps else 0.0
        val avgArchiveTime = if (archiveOps > 0) totalArchiveTimeMs.get().toDouble() / archiveOps else 0.0
        
        val cleanupSuccessRate = if (cleanupOps > 0) successfulCleanupOperations.get().toDouble() / cleanupOps else 0.0
        val archiveSuccessRate = if (archiveOps > 0) successfulArchiveOperations.get().toDouble() / archiveOps else 0.0
        
        val overallSuccessRate = if (cleanupOps + archiveOps > 0) {
            (successfulCleanupOperations.get() + successfulArchiveOperations.get()).toDouble() / (cleanupOps + archiveOps)
        } else 0.0
        
        val lastCleanupTime = cleanupHistory.values.maxOfOrNull { System.currentTimeMillis() }
        val lastArchiveTime = archiveHistory.values.maxOfOrNull { System.currentTimeMillis() }
        
        RetentionMetrics(
            totalCleanupOperations = cleanupOps,
            totalArchiveOperations = archiveOps,
            totalRecordsProcessed = totalRecordsProcessed.get(),
            totalRecordsArchived = totalRecordsArchived.get(),
            totalRecordsDeleted = totalRecordsDeleted.get(),
            averageCleanupTimeMs = avgCleanupTime,
            averageArchiveTimeMs = avgArchiveTime,
            successRate = overallSuccessRate,
            lastCleanupTime = lastCleanupTime,
            lastArchiveTime = lastArchiveTime,
            dataTypeMetrics = dataTypeMetrics.toMap()
        )
    }
    
    override suspend fun getDataGrowthTrend(dataType: DataType, days: Int): List<DataGrowthPoint> = withContext(Dispatchers.IO) {
        val history = dataGrowthHistory[dataType] ?: emptyList()
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        
        history.filter { it.timestamp >= cutoffTime }
            .sortedBy { it.timestamp }
    }
    
    override suspend fun getStorageUsage(): StorageUsage = withContext(Dispatchers.IO) {
        val archives = archiveService.listArchives()
        
        val totalArchives = archives.size
        val totalSizeBytes = archives.sumOf { it.fileSizeBytes }
        val compressedArchives = archives.filter { it.compressed }
        val compressedSizeBytes = compressedArchives.sumOf { it.fileSizeBytes }
        val compressionRatio = if (totalSizeBytes > 0) compressedSizeBytes.toDouble() / totalSizeBytes else 1.0
        
        val dataTypeUsage = archives.groupBy { it.dataType }
            .mapValues { (dataType, archiveList) ->
                val totalSize = archiveList.sumOf { it.fileSizeBytes }
                val avgFileSize = if (archiveList.isNotEmpty()) totalSize / archiveList.size else 0L
                val oldestArchive = archiveList.minOfOrNull { it.createdAt }
                val newestArchive = archiveList.maxOfOrNull { it.createdAt }
                
                DataTypeStorageUsage(
                    dataType = dataType,
                    archiveCount = archiveList.size,
                    totalSizeBytes = totalSize,
                    averageFileSizeBytes = avgFileSize,
                    oldestArchive = oldestArchive,
                    newestArchive = newestArchive
                )
            }
        
        StorageUsage(
            totalArchives = totalArchives,
            totalSizeBytes = totalSizeBytes,
            compressedSizeBytes = compressedSizeBytes,
            compressionRatio = compressionRatio,
            dataTypeUsage = dataTypeUsage
        )
    }
    
    private fun updateDataTypeMetrics(dataType: DataType, result: CleanupResult) {
        val current = dataTypeMetrics[dataType] ?: DataTypeMetrics(
            dataType = dataType,
            cleanupOperations = 0,
            archiveOperations = 0,
            recordsProcessed = 0,
            recordsArchived = 0,
            recordsDeleted = 0,
            averageCleanupTimeMs = 0.0,
            averageArchiveTimeMs = 0.0,
            successRate = 0.0,
            lastOperationTime = null
        )
        
        val newCleanupOps = current.cleanupOperations + 1
        val newRecordsProcessed = current.recordsProcessed + result.recordsProcessed
        val newRecordsArchived = current.recordsArchived + result.recordsArchived
        val newRecordsDeleted = current.recordsDeleted + result.recordsDeleted
        
        val newAvgCleanupTime = if (newCleanupOps > 0) {
            (current.averageCleanupTimeMs * current.cleanupOperations + result.executionTimeMs) / newCleanupOps
        } else 0.0
        
        val newSuccessRate = if (newCleanupOps > 0) {
            val successfulOps = if (result.success) current.cleanupOperations + 1 else current.cleanupOperations
            successfulOps.toDouble() / newCleanupOps
        } else 0.0
        
        dataTypeMetrics[dataType] = current.copy(
            cleanupOperations = newCleanupOps,
            recordsProcessed = newRecordsProcessed,
            recordsArchived = newRecordsArchived,
            recordsDeleted = newRecordsDeleted,
            averageCleanupTimeMs = newAvgCleanupTime,
            successRate = newSuccessRate,
            lastOperationTime = System.currentTimeMillis()
        )
    }
    
    private fun generateOperationId(dataType: DataType, operationType: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val timestamp = formatter.format(Instant.now().atZone(java.time.ZoneId.systemDefault()))
        return "${dataType.name.lowercase()}_${operationType}_$timestamp"
    }
    
    /**
     * Записать точку роста данных
     */
    suspend fun recordDataGrowth(dataType: DataType, recordCount: Long, sizeBytes: Long) = withContext(Dispatchers.IO) {
        val point = DataGrowthPoint(
            timestamp = System.currentTimeMillis(),
            recordCount = recordCount,
            sizeBytes = sizeBytes
        )
        
        dataGrowthHistory.computeIfAbsent(dataType) { mutableListOf() }.add(point)
        
        // Ограничиваем историю последними 1000 точками
        val history = dataGrowthHistory[dataType]!!
        if (history.size > 1000) {
            history.removeAt(0)
        }
        
        logger.debug("Recorded data growth for $dataType: $recordCount records, ${sizeBytes}bytes")
    }
}
