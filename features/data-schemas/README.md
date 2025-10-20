# Data Schemas Module

Модуль `data-schemas` обеспечивает качество данных в системе AiFuturesBot через схемы данных, валидацию на входе и контрактные тесты между модулями.

## Основные компоненты

### 1. Схемы данных (`DataSchemas.kt`)

Определяет типизированные схемы для всех основных сущностей системы:

- **`CandleData`** - данные свечей (OHLCV)
- **`SignalData`** - торговые сигналы
- **`AggregatedSignalData`** - агрегированные сигналы
- **`AnalysisResultData`** - результаты технического анализа
- **`AuditLogData`** - аудит логи

Каждая схема включает:
- Аннотации валидации (Bean Validation)
- Бизнес-правила в `init` блоках
- Сериализацию в JSON (Kotlinx Serialization)

### 2. Схемы событий (`EventSchemas.kt`)

Определяет события для межмодульного взаимодействия:

- **`MarketDataReceivedEvent`** - получение рыночных данных
- **`SignalReceivedEvent`** - получение сигнала
- **`SignalAggregatedEvent`** - агрегация сигналов
- **`AnalysisCompletedEvent`** - завершение анализа
- **`TradeDecisionEvent`** - торговое решение
- **`AuditEvent`** - аудит событие

### 3. Валидатор данных (`DataValidator.kt`)

Предоставляет комплексную валидацию:

- **Стандартная валидация** - Bean Validation
- **Детальная валидация** - с информацией об ошибках
- **Батчевая валидация** - для множественных объектов
- **Кастомные правила** - бизнес-логика валидации

### 4. Утилиты

- **`DataSchemaUtils`** - работа со схемами данных
- **`EventUtils`** - работа с событиями
- **`CustomValidationRules`** - кастомные правила валидации

## Использование

### Валидация данных

```kotlin
val candle = CandleData(
    symbol = "BTCUSDT",
    interval = "1m",
    openTime = 1000L,
    closeTime = 1600L,
    open = BigDecimal("100.00"),
    high = BigDecimal("110.00"),
    low = BigDecimal("90.00"),
    close = BigDecimal("105.00"),
    volume = BigDecimal("1000.00"),
    dataSource = "binance"
)

val validator = DataValidator()
val result = validator.validateCandleData(candle)

when (result) {
    is ValidationResult.Success -> println("Данные валидны")
    is ValidationResult.Failure -> println("Ошибки: ${result.errors}")
}
```

### Создание событий

```kotlin
val marketDataEvent = MarketDataReceivedEvent(
    eventId = "market_001",
    timestamp = System.currentTimeMillis(),
    version = "1.0",
    symbol = "BTCUSDT",
    interval = "1m",
    candleData = candle,
    source = "binance"
)

// Валидация события
val validationResult = EventUtils.validate(marketDataEvent)
```

### Сериализация/десериализация

```kotlin
// В JSON
val jsonString = DataSchemaUtils.toJson(candle)
val eventJson = EventUtils.toJson(marketDataEvent)

// Из JSON
val deserializedCandle = DataSchemaUtils.fromJson<CandleData>(jsonString)
val deserializedEvent = EventUtils.fromJson(eventJson)
```

## Контрактные тесты

Модуль включает комплексные тесты для проверки:

1. **Совместимости схем** - сериализация/десериализация
2. **Валидации** - корректность правил валидации
3. **Событий** - структура и валидация событий
4. **Интеграции** - совместимость между модулями

### Запуск тестов

```bash
./gradlew :features:data-schemas:test
```

## Интеграция с модулями

### Добавление в зависимости

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":features:data-schemas"))
}
```

### Использование в репозиториях

```kotlin
class CandlesRepository {
    fun saveCandle(candleData: CandleData) {
        // Валидация на входе
        val validationResult = DataSchemaUtils.validate(candleData)
        if (validationResult is ValidationResult.Failure) {
            throw IllegalArgumentException("Invalid candle data: ${validationResult.errors}")
        }
        
        // Сохранение в БД
        // ...
    }
}
```

### Использование в обработчиках событий

```kotlin
class MarketDataHandler {
    fun handleMarketData(event: MarketDataReceivedEvent) {
        // Валидация события
        val validationResult = EventUtils.validate(event)
        if (validationResult is ValidationResult.Failure) {
            logger.warn("Invalid market data event: ${validationResult.errors}")
            return
        }
        
        // Обработка данных
        // ...
    }
}
```

## Архитектурные принципы

1. **Типобезопасность** - все схемы типизированы
2. **Валидация на входе** - данные проверяются при получении
3. **Неизменяемость** - все схемы являются data классами
4. **Версионирование** - события содержат версию
5. **Расширяемость** - легко добавлять новые схемы и правила

## Зависимости

- **Kotlinx Serialization** - сериализация в JSON
- **Jakarta Validation** - валидация данных
- **JUnit 5** - тестирование

## Будущие улучшения

- [ ] Поддержка XML сериализации
- [ ] Автоматическая генерация схем из OpenAPI
- [ ] Интеграция с Apache Avro
- [ ] Метрики валидации
- [ ] Кэширование результатов валидации
