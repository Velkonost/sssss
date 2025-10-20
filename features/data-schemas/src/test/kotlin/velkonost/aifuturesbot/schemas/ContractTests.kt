package velkonost.aifuturesbot.schemas

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

/**
 * Контрактные тесты для проверки совместимости схем данных
 * между продюсерами и консюмерами
 */
class ContractTests {
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    @Test
    fun `CandleData schema contract - serialization and deserialization`() {
        // Arrange
        val originalCandle = CandleData(
            symbol = "BTCUSDT",
            interval = "1m",
            openTime = 1000L,
            closeTime = 1600L,
            open = BigDecimal("100.50"),
            high = BigDecimal("110.75"),
            low = BigDecimal("95.25"),
            close = BigDecimal("105.00"),
            volume = BigDecimal("1234.5678"),
            dataSource = "binance"
        )
        
        // Act
        val jsonString = json.encodeToString(originalCandle)
        val deserializedCandle = json.decodeFromString<CandleData>(jsonString)
        
        // Assert
        assertEquals(originalCandle.symbol, deserializedCandle.symbol)
        assertEquals(originalCandle.interval, deserializedCandle.interval)
        assertEquals(originalCandle.openTime, deserializedCandle.openTime)
        assertEquals(originalCandle.closeTime, deserializedCandle.closeTime)
        assertEquals(originalCandle.open, deserializedCandle.open)
        assertEquals(originalCandle.high, deserializedCandle.high)
        assertEquals(originalCandle.low, deserializedCandle.low)
        assertEquals(originalCandle.close, deserializedCandle.close)
        assertEquals(originalCandle.volume, deserializedCandle.volume)
        assertEquals(originalCandle.dataSource, deserializedCandle.dataSource)
    }
    
    @Test
    fun `SignalData schema contract - validation rules`() {
        // Test valid signal
        val validSignal = SignalData(
            sourceType = "telegram",
            sourceId = "msg-123",
            symbol = "ETHUSDT",
            timeframe = "5m",
            signalType = "long",
            weight = BigDecimal("1.5"),
            confidence = BigDecimal("0.8"),
            payloadJson = """{"text":"buy signal","user":"trader123"}"""
        )
        
        val validationResult = DataSchemaUtils.validate(validSignal)
        assertTrue(validationResult is ValidationResult.Success)
        
        // Test invalid signal type
        val invalidSignal = validSignal.copy(signalType = "invalid_type")
        val invalidResult = DataSchemaUtils.validate(invalidSignal)
        assertTrue(invalidResult is ValidationResult.Failure)
    }
    
    @Test
    fun `MarketDataReceivedEvent contract - event structure`() {
        // Arrange
        val candleData = CandleData(
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
        
        val event = MarketDataReceivedEvent(
            eventId = "market_data_123",
            timestamp = System.currentTimeMillis(),
            version = "1.0",
            symbol = "BTCUSDT",
            interval = "1m",
            candleData = candleData,
            source = "binance"
        )
        
        // Act & Assert
        assertNotNull(event.eventId)
        assertTrue(event.timestamp > 0)
        assertEquals("1.0", event.version)
        assertEquals("BTCUSDT", event.symbol)
        assertEquals("1m", event.interval)
        assertEquals("binance", event.source)
        
        // Test serialization
        val jsonString = EventUtils.toJson(event)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("BTCUSDT"))
        
        // Test deserialization
        val deserializedEvent = EventUtils.fromJson(jsonString) as MarketDataReceivedEvent
        assertEquals(event.eventId, deserializedEvent.eventId)
        assertEquals(event.symbol, deserializedEvent.symbol)
        assertEquals(event.candleData.symbol, deserializedEvent.candleData.symbol)
    }
    
    @Test
    fun `SignalReceivedEvent contract - event structure`() {
        // Arrange
        val signalData = SignalData(
            sourceType = "telegram",
            sourceId = "msg-456",
            symbol = "ETHUSDT",
            timeframe = "5m",
            signalType = "short",
            weight = BigDecimal("2.0"),
            confidence = BigDecimal("0.9"),
            payloadJson = """{"action":"sell","reason":"technical_analysis"}"""
        )
        
        val event = SignalReceivedEvent(
            eventId = "signal_456",
            timestamp = System.currentTimeMillis(),
            version = "1.0",
            signalData = signalData,
            source = "telegram"
        )
        
        // Act & Assert
        assertNotNull(event.eventId)
        assertTrue(event.timestamp > 0)
        assertEquals("1.0", event.version)
        assertEquals("telegram", event.source)
        assertEquals("ETHUSDT", event.signalData.symbol)
        assertEquals("short", event.signalData.signalType)
        
        // Test validation
        val validationResult = EventUtils.validate(event)
        assertTrue(validationResult is ValidationResult.Success)
    }
    
    @Test
    fun `TradeDecisionEvent contract - event structure`() {
        // Arrange
        val event = TradeDecisionEvent(
            eventId = "trade_decision_789",
            timestamp = System.currentTimeMillis(),
            version = "1.0",
            symbol = "BTCUSDT",
            action = "buy",
            quantity = "0.1",
            price = "50000.00",
            reason = "Strong bullish signal",
            confidence = "0.85",
            metadata = mapOf(
                "strategy" to "momentum",
                "risk_level" to "medium"
            )
        )
        
        // Act & Assert
        assertNotNull(event.eventId)
        assertTrue(event.timestamp > 0)
        assertEquals("1.0", event.version)
        assertEquals("BTCUSDT", event.symbol)
        assertEquals("buy", event.action)
        assertEquals("0.1", event.quantity)
        assertEquals("50000.00", event.price)
        assertEquals("Strong bullish signal", event.reason)
        assertEquals("0.85", event.confidence)
        assertEquals(2, event.metadata.size)
        assertEquals("momentum", event.metadata["strategy"])
        assertEquals("medium", event.metadata["risk_level"])
        
        // Test serialization
        val jsonString = EventUtils.toJson(event)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("BTCUSDT"))
        assertTrue(jsonString.contains("buy"))
    }
    
    @Test
    fun `Event versioning contract - backward compatibility`() {
        // Test that events with version 1.0 can be deserialized
        val eventV1 = MarketDataReceivedEvent(
            eventId = "test_event",
            timestamp = System.currentTimeMillis(),
            version = "1.0",
            symbol = "BTCUSDT",
            interval = "1m",
            candleData = CandleData(
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
            ),
            source = "binance"
        )
        
        val jsonString = EventUtils.toJson(eventV1)
        val deserializedEvent = EventUtils.fromJson(jsonString)
        
        assertTrue(deserializedEvent is MarketDataReceivedEvent)
        assertEquals("1.0", deserializedEvent.version)
        assertEquals("BTCUSDT", (deserializedEvent as MarketDataReceivedEvent).symbol)
    }
    
    @Test
    fun `Schema validation contract - error handling`() {
        // Test validation with invalid data
        val invalidCandle = CandleData(
            symbol = "", // Invalid: blank symbol
            interval = "1m",
            openTime = 1000L,
            closeTime = 500L, // Invalid: close time before open time
            open = BigDecimal("-10.00"), // Invalid: negative price
            high = BigDecimal("110.00"),
            low = BigDecimal("90.00"),
            close = BigDecimal("105.00"),
            volume = BigDecimal("1000.00"),
            dataSource = "binance"
        )
        
        val validationResult = DataSchemaUtils.validate(invalidCandle)
        assertTrue(validationResult is ValidationResult.Failure)
        assertTrue((validationResult as ValidationResult.Failure).errors.isNotEmpty())
        
        // Check that specific validation errors are present
        val errorMessages = (validationResult as ValidationResult.Failure).errors.joinToString("; ")
        assertTrue(errorMessages.contains("Symbol cannot be blank") || 
                  errorMessages.contains("Close time must be after open time") ||
                  errorMessages.contains("Open price must be positive"))
    }
}
