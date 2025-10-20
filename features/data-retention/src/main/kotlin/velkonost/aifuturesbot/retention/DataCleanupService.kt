package velkonost.aifuturesbot.retention

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.LongIdTable
import org.slf4j.LoggerFactory
import velkonost.aifuturesbot.db.*
import java.time.Instant

/**
 * Интерфейс для очистки данных в БД
 */
interface DataCleanupService {
    suspend fun cleanupExpiredData(dataType: DataType, dryRun: Boolean = false): CleanupResult
    suspend fun archiveAndCleanup(dataType: DataType, dryRun: Boolean = false): CleanupResult
    suspend fun getDataStatistics(dataType: DataType): DataStatistics
}

/**
 * Результат очистки данных
 */
data class CleanupResult(
    val dataType: DataType,
    val recordsProcessed: Int,
    val recordsArchived: Int,
    val recordsDeleted: Int,
    val executionTimeMs: Long,
    val success: Boolean,
    val error: String? = null,
    val dryRun: Boolean = false
)

/**
 * Статистика данных
 */
data class DataStatistics(
    val dataType: DataType,
    val totalRecords: Int,
    val hotRecords: Int,
    val warmRecords: Int,
    val coldRecords: Int,
    val oldestRecord: Long?,
    val newestRecord: Long?,
    val totalSizeBytes: Long
)

/**
 * Реализация сервиса очистки данных
 */
class DatabaseCleanupService(
    private val config: RetentionConfig,
    private val archiveService: ArchiveService
) : DataCleanupService {
    
    private val logger = LoggerFactory.getLogger(DatabaseCleanupService::class.java)
    
    override suspend fun cleanupExpiredData(dataType: DataType, dryRun: Boolean): CleanupResult = 
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            
            try {
                val policy = config.getPolicy(dataType) ?: run {
                    logger.warn("No retention policy found for $dataType")
                    return@withContext CleanupResult(
                        dataType = dataType,
                        recordsProcessed = 0,
                        recordsArchived = 0,
                        recordsDeleted = 0,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        success = false,
                        error = "No retention policy found"
                    )
                }
                
                val coldThreshold = policy.getColdDataThreshold()
                val recordsToCleanup = getRecordsToCleanup(dataType, coldThreshold)
                
                if (recordsToCleanup.isEmpty()) {
                    logger.info("No expired records found for $dataType")
                    return@withContext CleanupResult(
                        dataType = dataType,
                        recordsProcessed = 0,
                        recordsArchived = 0,
                        recordsDeleted = 0,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        success = true,
                        dryRun = dryRun
                    )
                }
                
                logger.info("Found ${recordsToCleanup.size} expired records for $dataType")
                
                if (dryRun) {
                    logger.info("DRY RUN: Would cleanup ${recordsToCleanup.size} records for $dataType")
                    return@withContext CleanupResult(
                        dataType = dataType,
                        recordsProcessed = recordsToCleanup.size,
                        recordsArchived = 0,
                        recordsDeleted = 0,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        success = true,
                        dryRun = true
                    )
                }
                
                // Архивируем данные перед удалением
                val archiveResult = if (config.isArchiveEnabled(dataType)) {
                    archiveService.archiveData(dataType, recordsToCleanup)
                } else {
                    ArchiveResult("", 0, 0, 0.0, true)
                }
                
                if (!archiveResult.success && config.isArchiveEnabled(dataType)) {
                    logger.error("Failed to archive data for $dataType: ${archiveResult.error}")
                    return@withContext CleanupResult(
                        dataType = dataType,
                        recordsProcessed = recordsToCleanup.size,
                        recordsArchived = 0,
                        recordsDeleted = 0,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        success = false,
                        error = "Archive failed: ${archiveResult.error}"
                    )
                }
                
                // Удаляем данные из БД
                val deletedCount = deleteRecords(dataType, recordsToCleanup.map { it.id })
                
                logger.info("Cleanup completed for $dataType: archived=${archiveResult.recordCount}, deleted=$deletedCount")
                
                CleanupResult(
                    dataType = dataType,
                    recordsProcessed = recordsToCleanup.size,
                    recordsArchived = archiveResult.recordCount,
                    recordsDeleted = deletedCount,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    success = true
                )
                
            } catch (e: Exception) {
                logger.error("Failed to cleanup data for $dataType", e)
                CleanupResult(
                    dataType = dataType,
                    recordsProcessed = 0,
                    recordsArchived = 0,
                    recordsDeleted = 0,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    success = false,
                    error = e.message
                )
            }
        }
    
    override suspend fun archiveAndCleanup(dataType: DataType, dryRun: Boolean): CleanupResult = 
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            
            try {
                val policy = config.getPolicy(dataType) ?: run {
                    logger.warn("No retention policy found for $dataType")
                    return@withContext CleanupResult(
                        dataType = dataType,
                        recordsProcessed = 0,
                        recordsArchived = 0,
                        recordsDeleted = 0,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        success = false,
                        error = "No retention policy found"
                    )
                }
                
                val warmThreshold = policy.getWarmDataThreshold()
                val coldThreshold = policy.getColdDataThreshold()
                
                // Получаем теплые данные для архивирования
                val warmRecords = getRecordsToArchive(dataType, warmThreshold, coldThreshold)
                
                if (warmRecords.isEmpty()) {
                    logger.info("No warm records found for $dataType")
                    return@withContext CleanupResult(
                        dataType = dataType,
                        recordsProcessed = 0,
                        recordsArchived = 0,
                        recordsDeleted = 0,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        success = true,
                        dryRun = dryRun
                    )
                }
                
                logger.info("Found ${warmRecords.size} warm records for $dataType")
                
                if (dryRun) {
                    logger.info("DRY RUN: Would archive ${warmRecords.size} warm records for $dataType")
                    return@withContext CleanupResult(
                        dataType = dataType,
                        recordsProcessed = warmRecords.size,
                        recordsArchived = 0,
                        recordsDeleted = 0,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        success = true,
                        dryRun = true
                    )
                }
                
                // Архивируем теплые данные
                val archiveResult = archiveService.archiveData(dataType, warmRecords)
                
                if (!archiveResult.success) {
                    logger.error("Failed to archive warm data for $dataType: ${archiveResult.error}")
                    return@withContext CleanupResult(
                        dataType = dataType,
                        recordsProcessed = warmRecords.size,
                        recordsArchived = 0,
                        recordsDeleted = 0,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        success = false,
                        error = "Archive failed: ${archiveResult.error}"
                    )
                }
                
                // Удаляем заархивированные данные из БД
                val deletedCount = deleteRecords(dataType, warmRecords.map { it.id })
                
                logger.info("Archive and cleanup completed for $dataType: archived=${archiveResult.recordCount}, deleted=$deletedCount")
                
                CleanupResult(
                    dataType = dataType,
                    recordsProcessed = warmRecords.size,
                    recordsArchived = archiveResult.recordCount,
                    recordsDeleted = deletedCount,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    success = true
                )
                
            } catch (e: Exception) {
                logger.error("Failed to archive and cleanup data for $dataType", e)
                CleanupResult(
                    dataType = dataType,
                    recordsProcessed = 0,
                    recordsArchived = 0,
                    recordsDeleted = 0,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    success = false,
                    error = e.message
                )
            }
        }
    
    override suspend fun getDataStatistics(dataType: DataType): DataStatistics = 
        withContext(Dispatchers.IO) {
            try {
                val policy = config.getPolicy(dataType) ?: run {
                    logger.warn("No retention policy found for $dataType")
                    return@withContext DataStatistics(
                        dataType = dataType,
                        totalRecords = 0,
                        hotRecords = 0,
                        warmRecords = 0,
                        coldRecords = 0,
                        oldestRecord = null,
                        newestRecord = null,
                        totalSizeBytes = 0
                    )
                }
                
                val hotThreshold = policy.getHotDataThreshold()
                val warmThreshold = policy.getWarmDataThreshold()
                val coldThreshold = policy.getColdDataThreshold()
                
                val table = getTableForDataType(dataType)
                val timestampColumn = getTimestampColumnForDataType(dataType)
                
                val totalRecords = getRecordCount(table)
                val hotRecords = getRecordCount(table, timestampColumn greater hotThreshold)
                val warmRecords = getRecordCount(table, 
                    timestampColumn greater warmThreshold and (timestampColumn less hotThreshold))
                val coldRecords = getRecordCount(table, timestampColumn less warmThreshold)
                
                val oldestRecord = getOldestTimestamp(table, timestampColumn)
                val newestRecord = getNewestTimestamp(table, timestampColumn)
                
                // Приблизительный размер данных
                val totalSizeBytes = estimateDataSize(table, totalRecords)
                
                DataStatistics(
                    dataType = dataType,
                    totalRecords = totalRecords,
                    hotRecords = hotRecords,
                    warmRecords = warmRecords,
                    coldRecords = coldRecords,
                    oldestRecord = oldestRecord,
                    newestRecord = newestRecord,
                    totalSizeBytes = totalSizeBytes
                )
                
            } catch (e: Exception) {
                logger.error("Failed to get data statistics for $dataType", e)
                DataStatistics(
                    dataType = dataType,
                    totalRecords = 0,
                    hotRecords = 0,
                    warmRecords = 0,
                    coldRecords = 0,
                    oldestRecord = null,
                    newestRecord = null,
                    totalSizeBytes = 0
                )
            }
        }
    
    private suspend fun getRecordsToCleanup(dataType: DataType, threshold: Long): List<ArchivableData> {
        val table = getTableForDataType(dataType) as LongIdTable
        val timestampColumn = getTimestampColumnForDataType(dataType)
        
        return transaction {
            val query = table.select(timestampColumn less threshold)
            query.limit(config.cleanupConfig.batchSize)
            query.map { row: ResultRow ->
                ArchivableData(
                    id = row[table.id].value,
                    data = rowToMap(row, table as Table),
                    timestamp = row[timestampColumn]
                )
                }
        }
    }
    
    private suspend fun getRecordsToArchive(dataType: DataType, warmThreshold: Long, coldThreshold: Long): List<ArchivableData> {
        val table = getTableForDataType(dataType) as LongIdTable
        val timestampColumn = getTimestampColumnForDataType(dataType)
        
        return transaction {
            val query = table.select(
                timestampColumn less warmThreshold and (timestampColumn greater coldThreshold)
            )
            query.limit(config.cleanupConfig.batchSize)
            query.map { row: ResultRow ->
                ArchivableData(
                    id = row[table.id].value,
                    data = rowToMap(row, table as Table),
                    timestamp = row[timestampColumn]
                )
            }
        }
    }
    
    private suspend fun deleteRecords(dataType: DataType, ids: List<Long>): Int {
        val table = getTableForDataType(dataType) as LongIdTable
        
        return transaction {
            table.deleteWhere { id inList ids }
        }
    }
    
    private suspend fun getRecordCount(table: Table, condition: Op<Boolean>? = null): Int {
        return transaction {
            if (condition != null) {
                table.select(condition).count().toInt()
            } else {
                table.selectAll().count().toInt()
            }
        }
    }
    
    private suspend fun getOldestTimestamp(table: Table, timestampColumn: Column<Long>): Long? {
        return transaction {
            table.selectAll()
                .orderBy(timestampColumn to SortOrder.ASC)
                .limit(1)
                .singleOrNull()?.get(timestampColumn)
        }
    }
    
    private suspend fun getNewestTimestamp(table: Table, timestampColumn: Column<Long>): Long? {
        return transaction {
            table.selectAll()
                .orderBy(timestampColumn to SortOrder.DESC)
                .limit(1)
                .singleOrNull()?.get(timestampColumn)
        }
    }
    
    private suspend fun estimateDataSize(table: Table, recordCount: Int): Long {
        // Приблизительная оценка размера данных
        return when (table) {
            CandlesTable -> recordCount * 200L // ~200 bytes per candle
            SignalsTable -> recordCount * 150L // ~150 bytes per signal
            AggregatedSignalsTable -> recordCount * 100L // ~100 bytes per aggregated signal
            AnalysisResultsTable -> recordCount * 300L // ~300 bytes per analysis result
            AuditLogsTable -> recordCount * 250L // ~250 bytes per audit log
            else -> recordCount * 200L // default estimate
        }
    }
    
    private fun getTableForDataType(dataType: DataType): Table {
        return when (dataType) {
            DataType.RAW_CANDLES, DataType.AGGREGATED_CANDLES -> CandlesTable
            DataType.SIGNALS -> SignalsTable
            DataType.AGGREGATED_SIGNALS -> AggregatedSignalsTable
            DataType.ANALYSIS_RESULTS -> AnalysisResultsTable
            DataType.AUDIT_LOGS -> AuditLogsTable
        }
    }
    
    private fun getTimestampColumnForDataType(dataType: DataType): Column<Long> {
        return when (dataType) {
            DataType.RAW_CANDLES, DataType.AGGREGATED_CANDLES -> CandlesTable.insertedAt
            DataType.SIGNALS -> SignalsTable.createdAt
            DataType.AGGREGATED_SIGNALS -> AggregatedSignalsTable.createdAt
            DataType.ANALYSIS_RESULTS -> AnalysisResultsTable.createdAt
            DataType.AUDIT_LOGS -> AuditLogsTable.createdAt
        }
    }
    
    private fun rowToMap(row: ResultRow, table: Table): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        
        table.columns.forEach { column ->
            val value = row[column]
            map[column.name] = value ?: "null"
        }
        
        return map
    }
}
