package velkonost.aifuturesbot.retention

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Files
import kotlin.io.path.exists

class FileSystemArchiveServiceTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var config: RetentionConfig
    private lateinit var archiveService: FileSystemArchiveService
    
    @BeforeEach
    fun setUp() {
        config = RetentionConfigUtils.createDefaultConfig().copy(
            archiveConfig = ArchiveConfig(
                enabled = true,
                storageType = StorageType.FILE_SYSTEM,
                compressionEnabled = true,
                basePath = tempDir.toString(),
                maxFileSizeMB = 100,
                retentionYears = 7
            )
        )
        archiveService = FileSystemArchiveService(config)
    }
    
    @Test
    fun `should archive data successfully`() = runBlocking {
        val testData = listOf(
            ArchivableData(
                id = 1L,
                data = mapOf("symbol" to "BTCUSDT", "price" to 50000.0),
                timestamp = System.currentTimeMillis()
            ),
            ArchivableData(
                id = 2L,
                data = mapOf("symbol" to "ETHUSDT", "price" to 3000.0),
                timestamp = System.currentTimeMillis()
            )
        )
        
        val result = archiveService.archiveData(DataType.RAW_CANDLES, testData)
        
        assertTrue(result.success)
        assertEquals(2, result.recordCount)
        assertTrue(result.fileSizeBytes > 0)
        assertTrue(result.compressionRatio > 0.0)
        assertTrue(result.compressionRatio <= 1.0)
        assertNotNull(result.archiveId)
        assertTrue(result.archiveId.isNotEmpty())
    }
    
    @Test
    fun `should handle empty data list`() = runBlocking {
        val result = archiveService.archiveData(DataType.RAW_CANDLES, emptyList())
        
        assertTrue(result.success)
        assertEquals(0, result.recordCount)
        assertEquals(0, result.fileSizeBytes)
        assertEquals(0.0, result.compressionRatio)
    }
    
    @Test
    fun `should restore archived data`() = runBlocking {
        val originalData = listOf(
            ArchivableData(
                id = 1L,
                data = mapOf("symbol" to "BTCUSDT", "price" to 50000.0),
                timestamp = System.currentTimeMillis()
            ),
            ArchivableData(
                id = 2L,
                data = mapOf("symbol" to "ETHUSDT", "price" to 3000.0),
                timestamp = System.currentTimeMillis()
            )
        )
        
        // Архивируем данные
        val archiveResult = archiveService.archiveData(DataType.RAW_CANDLES, originalData)
        assertTrue(archiveResult.success)
        
        // Восстанавливаем данные
        val restoredData = archiveService.restoreData(archiveResult.archiveId)
        
        assertNotNull(restoredData)
        restoredData!!
        assertEquals(originalData.size, restoredData.size)
        
        // Проверяем что данные совпадают
        originalData.forEachIndexed { index, original ->
            val restored = restoredData[index]
            assertEquals(original.id, restored.id)
            assertEquals(original.timestamp, restored.timestamp)
            assertEquals(original.data, restored.data)
        }
    }
    
    @Test
    fun `should return null for non-existent archive`() = runBlocking {
        val restoredData = archiveService.restoreData("non-existent-archive-id")
        
        assertNull(restoredData)
    }
    
    @Test
    fun `should delete archive successfully`() = runBlocking {
        val testData = listOf(
            ArchivableData(
                id = 1L,
                data = mapOf("symbol" to "BTCUSDT", "price" to 50000.0),
                timestamp = System.currentTimeMillis()
            )
        )
        
        // Архивируем данные
        val archiveResult = archiveService.archiveData(DataType.RAW_CANDLES, testData)
        assertTrue(archiveResult.success)
        
        // Проверяем что архив существует
        val archivesBefore = archiveService.listArchives(DataType.RAW_CANDLES)
        assertTrue(archivesBefore.any { it.archiveId == archiveResult.archiveId })
        
        // Удаляем архив
        val deleted = archiveService.deleteArchive(archiveResult.archiveId)
        assertTrue(deleted)
        
        // Проверяем что архив удален
        val archivesAfter = archiveService.listArchives(DataType.RAW_CANDLES)
        assertFalse(archivesAfter.any { it.archiveId == archiveResult.archiveId })
    }
    
    @Test
    fun `should return false when deleting non-existent archive`() = runBlocking {
        val deleted = archiveService.deleteArchive("non-existent-archive-id")
        assertFalse(deleted)
    }
    
    @Test
    fun `should list archives correctly`() = runBlocking {
        val testData1 = listOf(
            ArchivableData(
                id = 1L,
                data = mapOf("symbol" to "BTCUSDT", "price" to 50000.0),
                timestamp = System.currentTimeMillis()
            )
        )
        
        val testData2 = listOf(
            ArchivableData(
                id = 2L,
                data = mapOf("symbol" to "ETHUSDT", "price" to 3000.0),
                timestamp = System.currentTimeMillis()
            )
        )
        
        // Архивируем данные разных типов
        val archive1 = archiveService.archiveData(DataType.RAW_CANDLES, testData1)
        val archive2 = archiveService.archiveData(DataType.SIGNALS, testData2)
        
        assertTrue(archive1.success)
        assertTrue(archive2.success)
        
        // Получаем список всех архивов
        val allArchives = archiveService.listArchives()
        assertTrue(allArchives.size >= 2)
        
        // Получаем список архивов для конкретного типа данных
        val candleArchives = archiveService.listArchives(DataType.RAW_CANDLES)
        assertTrue(candleArchives.any { it.archiveId == archive1.archiveId })
        assertTrue(candleArchives.none { it.archiveId == archive2.archiveId })
        
        val signalArchives = archiveService.listArchives(DataType.SIGNALS)
        assertTrue(signalArchives.any { it.archiveId == archive2.archiveId })
        assertTrue(signalArchives.none { it.archiveId == archive1.archiveId })
    }
    
    @Test
    fun `should create correct archive file structure`() = runBlocking {
        val testData = listOf(
            ArchivableData(
                id = 1L,
                data = mapOf("symbol" to "BTCUSDT", "price" to 50000.0),
                timestamp = System.currentTimeMillis()
            )
        )
        
        val result = archiveService.archiveData(DataType.RAW_CANDLES, testData)
        assertTrue(result.success)
        
        // Проверяем что файл создан в правильной директории
        val expectedPath = tempDir.resolve("raw_candles").resolve("${result.archiveId}.json.gz")
        assertTrue(expectedPath.exists())
        
        // Проверяем что файл не пустой
        assertTrue(Files.size(expectedPath) > 0)
    }
    
    @Test
    fun `should handle compression correctly`() = runBlocking {
        val testData = listOf(
            ArchivableData(
                id = 1L,
                data = mapOf(
                    "symbol" to "BTCUSDT",
                    "price" to 50000.0,
                    "volume" to 1000000.0,
                    "description" to "Bitcoin to USDT trading pair with high volume"
                ),
                timestamp = System.currentTimeMillis()
            )
        )
        
        val result = archiveService.archiveData(DataType.RAW_CANDLES, testData)
        assertTrue(result.success)
        
        // Проверяем что сжатие работает (коэффициент сжатия должен быть меньше 1.0)
        assertTrue(result.compressionRatio < 1.0)
        assertTrue(result.compressionRatio > 0.0)
    }
    
    @Test
    fun `should generate unique archive IDs`() = runBlocking {
        val testData = listOf(
            ArchivableData(
                id = 1L,
                data = mapOf("symbol" to "BTCUSDT", "price" to 50000.0),
                timestamp = System.currentTimeMillis()
            )
        )
        
        val result1 = archiveService.archiveData(DataType.RAW_CANDLES, testData)
        val result2 = archiveService.archiveData(DataType.RAW_CANDLES, testData)
        
        assertTrue(result1.success)
        assertTrue(result2.success)
        
        // Archive IDs должны быть разными
        assertNotEquals(result1.archiveId, result2.archiveId)
        
        // Archive IDs должны содержать тип данных
        assertTrue(result1.archiveId.contains("raw_candles"))
        assertTrue(result2.archiveId.contains("raw_candles"))
    }
}
