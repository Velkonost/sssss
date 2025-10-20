# Data Retention Module

Модуль `features:data-retention` обеспечивает управление жизненным циклом данных в системе AiFuturesBot, включая архивирование, очистку и мониторинг данных.

## Основные компоненты

### 1. Конфигурация ретеншена (`RetentionConfig`)

Определяет политики хранения данных для различных типов:

- **RAW_CANDLES** - сырые данные свечей (7 дней горячих, 30 дней теплых, 90 дней холодных)
- **AGGREGATED_CANDLES** - агрегированные свечи (30 дней горячих, 90 дней теплых, 1 год холодных)
- **SIGNALS** - торговые сигналы (30 дней горячих, 90 дней теплых, 1 год холодных)
- **AGGREGATED_SIGNALS** - агрегированные сигналы (90 дней горячих, 1 год теплых, 3 года холодных)
- **ANALYSIS_RESULTS** - результаты анализа (30 дней горячих, 90 дней теплых, 1 год холодных)
- **AUDIT_LOGS** - аудит логи (30 дней горячих, 90 дней теплых, 7 лет холодных)

### 2. Сервис архивирования (`ArchiveService`)

Обеспечивает архивирование данных в холодное хранилище:

- **Сжатие данных** - GZIP сжатие для экономии места
- **Файловая система** - хранение архивов в структурированных директориях
- **Восстановление данных** - возможность восстановления заархивированных данных
- **Управление архивами** - создание, удаление, список архивов

### 3. Сервис очистки (`DataCleanupService`)

Выполняет очистку устаревших данных:

- **Архивирование теплых данных** - перемещение данных из БД в архив
- **Удаление холодных данных** - полное удаление данных после архивирования
- **Батчевая обработка** - обработка данных порциями для оптимизации производительности
- **Статистика данных** - получение информации о количестве и размере данных

### 4. Планировщик (`RetentionScheduler`)

Автоматизирует выполнение задач очистки:

- **Cron расписание** - настраиваемое расписание выполнения (по умолчанию ежедневно в 2:00)
- **Ручной запуск** - возможность запуска очистки вручную
- **Dry run режим** - тестовый режим без фактического удаления данных
- **Уведомления** - отправка уведомлений о результатах очистки

### 5. Мониторинг (`RetentionMonitoringService`)

Отслеживает метрики и статистику:

- **Метрики операций** - количество успешных/неудачных операций
- **Статистика данных** - количество записей по типам данных
- **Использование хранилища** - размер архивов и коэффициент сжатия
- **Тренды роста** - динамика роста данных во времени

## Использование

### Инициализация

```kotlin
// Создание конфигурации по умолчанию
val config = RetentionConfigUtils.createDefaultConfig()

// Инициализация сервиса ретеншена
val retentionService = RetentionService(
    config = config,
    archiveService = FileSystemArchiveService(config),
    cleanupService = DatabaseCleanupService(config, archiveService),
    scheduler = CronRetentionScheduler(config, cleanupService, archiveService),
    monitoringService = InMemoryRetentionMonitoringService(archiveService)
)

// Запуск сервиса
retentionService.initialize()
```

### Ручная очистка

```kotlin
// Полная очистка всех данных
val result = retentionService.runFullCleanup(dryRun = false)

// Очистка конкретного типа данных
val result = retentionService.runCleanupForDataType(DataType.RAW_CANDLES, dryRun = false)

// Тестовый запуск без удаления данных
val dryRunResult = retentionService.runFullCleanup(dryRun = true)
```

### Получение статистики

```kotlin
// Статистика по всем типам данных
val statistics = retentionService.getDataStatistics()

// Метрики ретеншена
val metrics = retentionService.getRetentionMetrics()

// Использование хранилища
val storageUsage = retentionService.getStorageUsage()

// Тренд роста данных
val growthTrend = retentionService.getDataGrowthTrend(DataType.RAW_CANDLES, days = 30)
```

### Архивирование данных

```kotlin
val archiveService = FileSystemArchiveService(config)

// Архивирование данных
val testData = listOf(
    ArchivableData(
        id = 1L,
        data = mapOf("symbol" to "BTCUSDT", "price" to 50000.0),
        timestamp = System.currentTimeMillis()
    )
)

val archiveResult = archiveService.archiveData(DataType.RAW_CANDLES, testData)

// Восстановление данных
val restoredData = archiveService.restoreData(archiveResult.archiveId)

// Удаление архива
val deleted = archiveService.deleteArchive(archiveResult.archiveId)

// Список архивов
val archives = archiveService.listArchives(DataType.RAW_CANDLES)
```

## Конфигурация

### Политики ретеншена

```kotlin
val policy = RetentionPolicy(
    dataType = DataType.RAW_CANDLES,
    hotRetentionDays = 7,      // Горячие данные - 7 дней
    warmRetentionDays = 30,    // Теплые данные - 30 дней
    coldRetentionDays = 90,    // Холодные данные - 90 дней
    archiveEnabled = true,      // Включено архивирование
    cleanupEnabled = true,     // Включена очистка
    batchSize = 1000          // Размер батча для обработки
)
```

### Конфигурация архивирования

```kotlin
val archiveConfig = ArchiveConfig(
    enabled = true,                    // Включено архивирование
    storageType = StorageType.FILE_SYSTEM, // Тип хранилища
    compressionEnabled = true,         // Включено сжатие
    encryptionEnabled = false,         // Шифрование отключено
    basePath = "/var/archive/ai-futures-bot", // Базовый путь
    maxFileSizeMB = 100,              // Максимальный размер файла
    retentionYears = 7                // Хранение архивов 7 лет
)
```

### Конфигурация очистки

```kotlin
val cleanupConfig = CleanupConfig(
    enabled = true,                    // Включена очистка
    scheduleCron = "0 2 * * *",       // Расписание (ежедневно в 2:00)
    batchSize = 1000,                 // Размер батча
    maxExecutionTimeMinutes = 30,      // Максимальное время выполнения
    dryRunEnabled = false,            // Dry run отключен
    notificationEnabled = true         // Уведомления включены
)
```

## Мониторинг

### Метрики операций

- `totalCleanupOperations` - общее количество операций очистки
- `totalArchiveOperations` - общее количество операций архивирования
- `totalRecordsProcessed` - общее количество обработанных записей
- `totalRecordsArchived` - общее количество заархивированных записей
- `totalRecordsDeleted` - общее количество удаленных записей
- `averageCleanupTimeMs` - среднее время выполнения очистки
- `averageArchiveTimeMs` - среднее время выполнения архивирования
- `successRate` - процент успешных операций

### Статистика данных

- `totalRecords` - общее количество записей
- `hotRecords` - количество горячих записей
- `warmRecords` - количество теплых записей
- `coldRecords` - количество холодных записей
- `oldestRecord` - timestamp самой старой записи
- `newestRecord` - timestamp самой новой записи
- `totalSizeBytes` - общий размер данных в байтах

### Использование хранилища

- `totalArchives` - общее количество архивов
- `totalSizeBytes` - общий размер архивов
- `compressedSizeBytes` - размер сжатых архивов
- `compressionRatio` - коэффициент сжатия
- `dataTypeUsage` - использование по типам данных

## Тестирование

```bash
# Запуск тестов модуля
./gradlew :features:data-retention:test

# Запуск конкретного теста
./gradlew :features:data-retention:test --tests "RetentionConfigTest"
```

## Архитектурные решения

### Принципы проектирования

1. **Модульность** - каждый компонент имеет четко определенную ответственность
2. **Расширяемость** - легко добавить новые типы хранилищ и политик
3. **Надежность** - обработка ошибок и восстановление после сбоев
4. **Производительность** - батчевая обработка и асинхронные операции
5. **Мониторинг** - полная видимость операций и метрик

### Паттерны

- **Strategy** - различные типы хранилищ (файловая система, S3, Azure)
- **Observer** - мониторинг и уведомления о событиях
- **Template Method** - общий алгоритм очистки с настраиваемыми шагами
- **Factory** - создание конфигураций и сервисов

### Интеграция

Модуль интегрируется с:

- **features:db** - для доступа к данным БД
- **features:data-schemas** - для валидации данных
- **core:common** - для общих утилит
- **features:logger** - для логирования операций

## Безопасность

- **Валидация данных** - проверка корректности данных перед архивированием
- **Безопасное удаление** - гарантия удаления данных после архивирования
- **Аудит операций** - логирование всех операций очистки и архивирования
- **Контроль доступа** - ограничение доступа к архивам

## Производительность

- **Батчевая обработка** - обработка данных порциями для оптимизации памяти
- **Асинхронные операции** - использование корутин для неблокирующих операций
- **Сжатие данных** - GZIP сжатие для экономии места на диске
- **Индексирование** - эффективный поиск архивов по метаданным

## Расширения

### Поддерживаемые типы хранилищ

- **FILE_SYSTEM** - файловая система (реализовано)
- **S3** - Amazon S3 (планируется)
- **AZURE_BLOB** - Azure Blob Storage (планируется)
- **GCS** - Google Cloud Storage (планируется)

### Будущие улучшения

- **Шифрование архивов** - дополнительная защита данных
- **Репликация архивов** - резервное копирование в несколько хранилищ
- **Автоматическое масштабирование** - динамическое изменение политик
- **ML-оптимизация** - использование машинного обучения для оптимизации политик
