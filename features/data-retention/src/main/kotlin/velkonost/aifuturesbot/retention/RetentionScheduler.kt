package velkonost.aifuturesbot.retention

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Интерфейс для планировщика задач очистки
 */
interface RetentionScheduler {
    fun start()
    fun stop()
    fun isRunning(): Boolean
    suspend fun runCleanupNow(dataType: DataType? = null, dryRun: Boolean = false): List<CleanupResult>
}

/**
 * Реализация планировщика задач очистки
 */
class CronRetentionScheduler(
    private val config: RetentionConfig,
    private val cleanupService: DataCleanupService,
    private val archiveService: ArchiveService
) : RetentionScheduler {
    
    private val logger = LoggerFactory.getLogger(CronRetentionScheduler::class.java)
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var isStarted = false
    
    override fun start() {
        if (isStarted) {
            logger.warn("Retention scheduler is already started")
            return
        }
        
        if (!config.cleanupConfig.enabled) {
            logger.info("Cleanup is disabled in configuration")
            return
        }
        
        logger.info("Starting retention scheduler with cron: ${config.cleanupConfig.scheduleCron}")
        
        // Парсим cron выражение (упрощенная версия)
        val cronParts = config.cleanupConfig.scheduleCron.split(" ")
        if (cronParts.size != 5) {
            logger.error("Invalid cron expression: ${config.cleanupConfig.scheduleCron}")
            return
        }
        
        val (minute, hour, dayOfMonth, month, dayOfWeek) = cronParts
        
        // Вычисляем интервал до следующего выполнения
        val nextExecutionDelay = calculateNextExecutionDelay(minute, hour, dayOfMonth, month, dayOfWeek)
        
        // Запускаем задачу
        scheduler.scheduleAtFixedRate(
            { 
                scope.launch {
                    runScheduledCleanup()
                }
            },
            nextExecutionDelay,
            24 * 60 * 60 * 1000L, // Повторяем каждые 24 часа
            TimeUnit.MILLISECONDS
        )
        
        isStarted = true
        logger.info("Retention scheduler started successfully")
    }
    
    override fun stop() {
        if (!isStarted) {
            logger.warn("Retention scheduler is not started")
            return
        }
        
        logger.info("Stopping retention scheduler...")
        
        scheduler.shutdown()
        scope.cancel()
        
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
                logger.warn("Retention scheduler did not terminate gracefully")
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
        
        isStarted = false
        logger.info("Retention scheduler stopped")
    }
    
    override fun isRunning(): Boolean = isStarted && !scheduler.isShutdown
    
    override suspend fun runCleanupNow(dataType: DataType?, dryRun: Boolean): List<CleanupResult> {
        logger.info("Running manual cleanup${if (dryRun) " (DRY RUN)" else ""} for ${dataType ?: "all data types"}")
        
        val results = mutableListOf<CleanupResult>()
        
        try {
            val dataTypesToProcess = if (dataType != null) {
                listOf(dataType)
            } else {
                DataType.values().filter { config.isCleanupEnabled(it) }
            }
            
            for (dt in dataTypesToProcess) {
                try {
                    // Сначала архивируем теплые данные
                    val archiveResult = cleanupService.archiveAndCleanup(dt, dryRun)
                    results.add(archiveResult)
                    
                    // Затем очищаем холодные данные
                    val cleanupResult = cleanupService.cleanupExpiredData(dt, dryRun)
                    results.add(cleanupResult)
                    
                } catch (e: Exception) {
                    logger.error("Failed to cleanup $dt", e)
                    results.add(CleanupResult(
                        dataType = dt,
                        recordsProcessed = 0,
                        recordsArchived = 0,
                        recordsDeleted = 0,
                        executionTimeMs = 0,
                        success = false,
                        error = e.message
                    ))
                }
            }
            
            // Логируем результаты
            logCleanupResults(results, dryRun)
            
        } catch (e: Exception) {
            logger.error("Failed to run cleanup", e)
        }
        
        return results
    }
    
    private suspend fun runScheduledCleanup() {
        logger.info("Running scheduled cleanup at ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
        
        try {
            val results = runCleanupNow(dryRun = false)
            
            // Отправляем уведомления если включены
            if (config.cleanupConfig.notificationEnabled) {
                sendCleanupNotification(results)
            }
            
        } catch (e: Exception) {
            logger.error("Scheduled cleanup failed", e)
        }
    }
    
    private fun calculateNextExecutionDelay(minute: String, hour: String, dayOfMonth: String, month: String, dayOfWeek: String): Long {
        val now = LocalDateTime.now()
        
        // Упрощенная логика - считаем что задача выполняется ежедневно в указанное время
        val targetHour = if (hour == "*") now.hour else hour.toInt()
        val targetMinute = if (minute == "*") now.minute else minute.toInt()
        
        var nextExecution = now.withHour(targetHour).withMinute(targetMinute).withSecond(0).withNano(0)
        
        // Если время уже прошло сегодня, планируем на завтра
        if (nextExecution.isBefore(now)) {
            nextExecution = nextExecution.plusDays(1)
        }
        
        val delayMs = java.time.Duration.between(now, nextExecution).toMillis()
        logger.info("Next cleanup scheduled at $nextExecution (in ${delayMs / 1000 / 60} minutes)")
        
        return delayMs
    }
    
    private fun logCleanupResults(results: List<CleanupResult>, dryRun: Boolean) {
        val totalProcessed = results.sumOf { it.recordsProcessed }
        val totalArchived = results.sumOf { it.recordsArchived }
        val totalDeleted = results.sumOf { it.recordsDeleted }
        val totalTime = results.sumOf { it.executionTimeMs }
        val successCount = results.count { it.success }
        
        logger.info("Cleanup completed${if (dryRun) " (DRY RUN)" else ""}: " +
            "processed=$totalProcessed, archived=$totalArchived, deleted=$totalDeleted, " +
            "success=$successCount/${results.size}, time=${totalTime}ms")
        
        // Детальные результаты по типам данных
        results.forEach { result ->
            logger.info("${result.dataType}: processed=${result.recordsProcessed}, " +
                "archived=${result.recordsArchived}, deleted=${result.recordsDeleted}, " +
                "time=${result.executionTimeMs}ms, success=${result.success}")
            
            if (!result.success && result.error != null) {
                logger.error("${result.dataType} cleanup failed: ${result.error}")
            }
        }
    }
    
    private suspend fun sendCleanupNotification(results: List<CleanupResult>) {
        try {
            // Здесь можно добавить отправку уведомлений (email, Slack, Telegram и т.д.)
            val summary = results.groupBy { it.success }
            val successCount = summary[true]?.size ?: 0
            val failureCount = summary[false]?.size ?: 0
            
            logger.info("Cleanup notification: $successCount successful, $failureCount failed")
            
            // TODO: Реализовать отправку уведомлений через Telegram или другой сервис
            
        } catch (e: Exception) {
            logger.error("Failed to send cleanup notification", e)
        }
    }
}
