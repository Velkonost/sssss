package velkonost.aifuturesbot.retention

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * Интерфейс для архивирования данных
 */
interface ArchiveService {
    suspend fun archiveData(dataType: DataType, data: List<ArchivableData>): ArchiveResult
    suspend fun restoreData(archiveId: String): List<ArchivableData>?
    suspend fun deleteArchive(archiveId: String): Boolean
    suspend fun listArchives(dataType: DataType? = null): List<ArchiveInfo>
}

/**
 * Данные для архивирования
 */
data class ArchivableData(
    val id: Long,
    val data: Map<String, Any>,
    val timestamp: Long
)

/**
 * Результат архивирования
 */
data class ArchiveResult(
    val archiveId: String,
    val recordCount: Int,
    val fileSizeBytes: Long,
    val compressionRatio: Double,
    val success: Boolean,
    val error: String? = null
)

/**
 * Информация об архиве
 */
data class ArchiveInfo(
    val archiveId: String,
    val dataType: DataType,
    val recordCount: Int,
    val fileSizeBytes: Long,
    val createdAt: Long,
    val compressed: Boolean
)

/**
 * Реализация сервиса архивирования на файловой системе
 */
class FileSystemArchiveService(
    private val config: RetentionConfig
) : ArchiveService {
    
    private val logger = LoggerFactory.getLogger(FileSystemArchiveService::class.java)
    private val json = Json { prettyPrint = true }
    
    override suspend fun archiveData(dataType: DataType, data: List<ArchivableData>): ArchiveResult = 
        withContext(Dispatchers.IO) {
            try {
                if (data.isEmpty()) {
                    return@withContext ArchiveResult(
                        archiveId = "",
                        recordCount = 0,
                        fileSizeBytes = 0,
                        compressionRatio = 0.0,
                        success = true
                    )
                }
                
                val archiveId = generateArchiveId(dataType)
                val archivePath = getArchivePath(dataType, archiveId)
                
                // Создаем директорию если не существует
                archivePath.parent.createDirectories()
                
                val originalSize = data.sumOf { it.data.toString().length.toLong() }
                
                val archiveFile = if (config.archiveConfig.compressionEnabled) {
                    createCompressedArchive(archivePath, data)
                } else {
                    createUncompressedArchive(archivePath, data)
                }
                
                val compressedSize = Files.size(archiveFile)
                val compressionRatio = if (originalSize > 0) compressedSize.toDouble() / originalSize else 1.0
                
                logger.info("Archived ${data.size} records of $dataType to $archiveId, " +
                    "compression ratio: ${"%02f".format(compressionRatio)}")
                
                ArchiveResult(
                    archiveId = archiveId,
                    recordCount = data.size,
                    fileSizeBytes = compressedSize,
                    compressionRatio = compressionRatio,
                    success = true
                )
                
            } catch (e: Exception) {
                logger.error("Failed to archive data for $dataType", e)
                ArchiveResult(
                    archiveId = "",
                    recordCount = 0,
                    fileSizeBytes = 0,
                    compressionRatio = 0.0,
                    success = false,
                    error = e.message
                )
            }
        }
    
    override suspend fun restoreData(archiveId: String): List<ArchivableData>? = 
        withContext(Dispatchers.IO) {
            try {
                val archiveFile = findArchiveFile(archiveId)
                if (archiveFile == null || !archiveFile.exists()) {
                    logger.warn("Archive file not found: $archiveId")
                    return@withContext null
                }
                
                val data = if (archiveFile.fileName.toString().endsWith(".gz")) {
                    restoreCompressedArchive(archiveFile)
                } else {
                    restoreUncompressedArchive(archiveFile)
                }
                
                logger.info("Restored ${data.size} records from archive $archiveId")
                data
                
            } catch (e: Exception) {
                logger.error("Failed to restore data from archive $archiveId", e)
                null
            }
        }
    
    override suspend fun deleteArchive(archiveId: String): Boolean = 
        withContext(Dispatchers.IO) {
            try {
                val archiveFile = findArchiveFile(archiveId)
                if (archiveFile == null) {
                    return@withContext false
                }
                
                val deleted = Files.deleteIfExists(archiveFile)
                if (deleted) {
                    logger.info("Deleted archive: $archiveId")
                }
                deleted
                
            } catch (e: Exception) {
                logger.error("Failed to delete archive $archiveId", e)
                false
            }
        }
    
    override suspend fun listArchives(dataType: DataType?): List<ArchiveInfo> = 
        withContext(Dispatchers.IO) {
            try {
                val basePath = Paths.get(config.archiveConfig.basePath)
                if (!basePath.exists()) {
                    return@withContext emptyList()
                }
                
                val archives = mutableListOf<ArchiveInfo>()
                
                Files.walk(basePath)
                    .filter { Files.isRegularFile(it) }
                    .forEach { file ->
                        try {
                            val archiveInfo = parseArchiveInfo(file)
                            if (dataType == null || archiveInfo.dataType == dataType) {
                                archives.add(archiveInfo)
                            }
                        } catch (e: Exception) {
                            logger.warn("Failed to parse archive info for ${file.fileName}", e)
                        }
                    }
                
                archives.sortedByDescending { it.createdAt }
                
            } catch (e: Exception) {
                logger.error("Failed to list archives", e)
                emptyList()
            }
        }
    
    private fun generateArchiveId(dataType: DataType): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val timestamp = formatter.format(Instant.now().atZone(java.time.ZoneId.systemDefault()))
        val random = (1000..9999).random()
        return "${dataType.name.lowercase()}_${timestamp}_${random}"
    }
    
    private fun getArchivePath(dataType: DataType, archiveId: String): Path {
        val basePath = Paths.get(config.archiveConfig.basePath)
        val dataTypePath = basePath.resolve(dataType.name.lowercase())
        val extension = if (config.archiveConfig.compressionEnabled) "json.gz" else "json"
        return dataTypePath.resolve("$archiveId.$extension")
    }
    
    private fun createCompressedArchive(path: Path, data: List<ArchivableData>): Path {
        GZIPOutputStream(FileOutputStream(path.toFile())).use { gzipOut ->
            data.forEach { archivableData ->
                val jsonData = json.encodeToString(archivableData)
                gzipOut.write(jsonData.toByteArray())
                gzipOut.write("\n".toByteArray())
            }
        }
        return path
    }
    
    private fun createUncompressedArchive(path: Path, data: List<ArchivableData>): Path {
        Files.write(path, data.map { json.encodeToString(it) })
        return path
    }
    
    private fun restoreCompressedArchive(file: Path): List<ArchivableData> {
        val data = mutableListOf<ArchivableData>()
        
        java.util.zip.GZIPInputStream(Files.newInputStream(file)).use { gzipIn ->
            gzipIn.bufferedReader().forEachLine { line ->
                if (line.isNotBlank()) {
                    val archivableData = json.decodeFromString<ArchivableData>(line)
                    data.add(archivableData)
                }
            }
        }
        
        return data
    }
    
    private fun restoreUncompressedArchive(file: Path): List<ArchivableData> {
        return Files.readAllLines(file)
            .filter { it.isNotBlank() }
            .map { json.decodeFromString<ArchivableData>(it) }
    }
    
    private fun findArchiveFile(archiveId: String): Path? {
        val basePath = Paths.get(config.archiveConfig.basePath)
        
        var result: Path? = null
        Files.walk(basePath)
            .filter { Files.isRegularFile(it) }
            .forEach { file ->
                if (file.fileName.toString().startsWith(archiveId)) {
                    result = file
                }
            }
        
        return result
    }
    
    private fun parseArchiveInfo(file: Path): ArchiveInfo {
        val fileName = file.fileName.toString()
        val parts = fileName.split("_")
        
        if (parts.size < 3) {
            throw IllegalArgumentException("Invalid archive file name: $fileName")
        }
        
        val dataType = DataType.valueOf(parts[0].uppercase())
        val compressed = fileName.endsWith(".gz")
        val fileSize = Files.size(file)
        
        // Парсим количество записей из файла (приблизительно)
        val recordCount = estimateRecordCount(file, compressed)
        
        // Парсим время создания из имени файла
        val createdAt = parseCreationTime(parts[1], parts[2])
        
        return ArchiveInfo(
            archiveId = fileName.substringBeforeLast("."),
            dataType = dataType,
            recordCount = recordCount,
            fileSizeBytes = fileSize,
            createdAt = createdAt,
            compressed = compressed
        )
    }
    
    private fun estimateRecordCount(file: Path, compressed: Boolean): Int {
        return try {
            if (compressed) {
                java.util.zip.GZIPInputStream(Files.newInputStream(file)).use { gzipIn ->
                    gzipIn.bufferedReader().use { reader ->
                        reader.lineSequence().count().toInt()
                    }
                }
            } else {
                Files.readAllLines(file).size
            }
        } catch (e: Exception) {
            logger.warn("Failed to estimate record count for ${file.fileName}", e)
            0
        }
    }
    
    private fun parseCreationTime(datePart: String, timePart: String): Long {
        return try {
            val dateTime = "${datePart}_${timePart}"
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            val zonedDateTime = java.time.ZonedDateTime.parse(dateTime, formatter.withZone(java.time.ZoneId.systemDefault()))
            zonedDateTime.toInstant().toEpochMilli()
        } catch (e: Exception) {
            logger.warn("Failed to parse creation time from ${datePart}_${timePart}", e)
            System.currentTimeMillis()
        }
    }
}
