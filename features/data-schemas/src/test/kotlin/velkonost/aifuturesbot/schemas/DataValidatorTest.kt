package velkonost.aifuturesbot.schemas

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

/**
 * Тесты для валидатора данных
 */
class DataValidatorTest {
    
    private val validator = DataValidator()
    
    @Test
    fun `validateCandleData - valid candle should pass`() {
        val validCandle = CandleData(
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
        
        val result = validator.validateCandleData(validCandle)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `validateCandleData - invalid candle should fail`() {
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
        
        val result = validator.validateCandleData(invalidCandle)
        assertTrue(result is ValidationResult.Failure)
        assertTrue((result as ValidationResult.Failure).errors.isNotEmpty())
    }
    
    @Test
    fun `validateSignalData - valid signal should pass`() {
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
        
        val result = validator.validateSignalData(validSignal)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `validateSignalData - invalid signal type should fail`() {
        val invalidSignal = SignalData(
            sourceType = "telegram",
            sourceId = "msg-123",
            symbol = "ETHUSDT",
            timeframe = "5m",
            signalType = "invalid_type", // Invalid signal type
            weight = BigDecimal("1.5"),
            confidence = BigDecimal("0.8"),
            payloadJson = """{"text":"buy signal"}"""
        )
        
        val result = validator.validateSignalData(invalidSignal)
        assertTrue(result is ValidationResult.Failure)
    }
    
    @Test
    fun `validateEvent - market data event should pass`() {
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
            eventId = "test_event",
            timestamp = System.currentTimeMillis(),
            version = "1.0",
            symbol = "BTCUSDT",
            interval = "1m",
            candleData = candleData,
            source = "binance"
        )
        
        val result = validator.validateEvent(event)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `validateWithDetails - should return detailed errors`() {
        val invalidCandle = CandleData(
            symbol = "", // Invalid: blank symbol
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
        
        val result = validator.validateWithDetails(invalidCandle)
        assertTrue(result is DetailedValidationResult.Failure)
        assertTrue((result as DetailedValidationResult.Failure).errors.isNotEmpty())
        
        val symbolError = (result as DetailedValidationResult.Failure).errors.find { it.field == "symbol" }
        assertNotNull(symbolError)
        assertTrue(symbolError!!.message.contains("cannot be blank"))
    }
    
    @Test
    fun `validateBatch - mixed valid and invalid data`() {
        val validCandle = CandleData(
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
        
        val invalidCandle = CandleData(
            symbol = "", // Invalid
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
        
        val result = validator.validateBatch(listOf(validCandle, invalidCandle))
        assertTrue(result is BatchValidationResult.Failure)
        assertEquals(1, (result as BatchValidationResult.Failure).errors.size)
        assertEquals(1, (result as BatchValidationResult.Failure).errors[0].index) // Second item (index 1) failed
    }
    
    @Test
    fun `validateWithCustomRules - should apply custom validation`() {
        val candle = CandleData(
            symbol = "BTCUSDT",
            interval = "1m",
            openTime = 1000L,
            closeTime = 1600L,
            open = BigDecimal("100.00"),
            high = BigDecimal("90.00"), // Invalid: high < low
            low = BigDecimal("95.00"),
            close = BigDecimal("105.00"),
            volume = BigDecimal("1000.00"),
            dataSource = "binance"
        )
        
        val customRules = listOf<(Any) -> ValidationResult> { data ->
            CustomValidationRules.validateCandlePrices(data as CandleData)
        }
        val result = validator.validateWithCustomRules(candle, customRules)
        
        assertTrue(result is ValidationResult.Failure)
        assertTrue((result as ValidationResult.Failure).errors.any { it.contains("High price") && it.contains("low price") })
    }
    
    @Test
    fun `CustomValidationRules - validateCandlePrices should catch price inconsistencies`() {
        val invalidCandle = CandleData(
            symbol = "BTCUSDT",
            interval = "1m",
            openTime = 1000L,
            closeTime = 500L, // Invalid: close time before open time
            open = BigDecimal("100.00"),
            high = BigDecimal("90.00"), // Invalid: high < low
            low = BigDecimal("95.00"),
            close = BigDecimal("105.00"),
            volume = BigDecimal("1000.00"),
            dataSource = "binance"
        )
        
        val result = CustomValidationRules.validateCandlePrices(invalidCandle)
        assertTrue(result is ValidationResult.Failure)
        assertTrue((result as ValidationResult.Failure).errors.size >= 2) // At least 2 errors expected
    }
    
    @Test
    fun `CustomValidationRules - validateSignalLogic should validate signal properties`() {
        val invalidSignal = SignalData(
            sourceType = "telegram",
            sourceId = "msg-123",
            symbol = "ETHUSDT",
            timeframe = "5m",
            signalType = "long",
            weight = BigDecimal("-1.0"), // Invalid: negative weight
            confidence = BigDecimal("1.5"), // Invalid: confidence > 1
            payloadJson = "invalid json" // Invalid JSON
        )
        
        val result = CustomValidationRules.validateSignalLogic(invalidSignal)
        assertTrue(result is ValidationResult.Failure)
        assertTrue((result as ValidationResult.Failure).errors.size >= 2) // At least 2 errors expected
    }
    
    @Test
    fun `CustomValidationRules - validateTimeWindow should validate time windows`() {
        val invalidAggregated = AggregatedSignalData(
            symbol = "BTCUSDT",
            timeframe = "1h",
            windowStart = System.currentTimeMillis(),
            windowEnd = System.currentTimeMillis() - 1000L, // Invalid: end before start
            score = BigDecimal("0.5"),
            decision = "long",
            basisJson = """{"signals":3}"""
        )
        
        val result = CustomValidationRules.validateTimeWindow(invalidAggregated)
        assertTrue(result is ValidationResult.Failure)
        assertTrue((result as ValidationResult.Failure).errors.any { it.contains("Window end") && it.contains("Window start") })
    }
}
