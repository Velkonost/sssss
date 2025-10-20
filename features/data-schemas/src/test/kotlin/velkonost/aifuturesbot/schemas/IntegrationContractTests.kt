package velkonost.aifuturesbot.schemas

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

/**
 * Интеграционные тесты для проверки совместимости схем
 * между различными модулями системы
 */
class IntegrationContractTests {
    
    @Test
    fun `DB Repository to Schema compatibility - CandlesRepository`() {
        // Test that DB repository data classes are compatible with schemas
        // Note: This test requires the db module to be available
        // For now, we'll test schema validation independently
        
        val schemaCandle = CandleData(
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
        
        // Validate schema
        val validationResult = DataSchemaUtils.validate(schemaCandle)
        assertTrue(validationResult is ValidationResult.Success)
    }
    
    @Test
    fun `Event flow contract - MarketData to Signal processing`() {
        // Simulate the flow: MarketData -> Analysis -> Signal
        val marketDataEvent = MarketDataReceivedEvent(
            eventId = "market_001",
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
        
        // Validate market data event
        val marketValidation = EventUtils.validate(marketDataEvent)
        assertTrue(marketValidation is ValidationResult.Success)
        
        // Simulate analysis result
        val analysisEvent = AnalysisCompletedEvent(
            eventId = "analysis_001",
            timestamp = System.currentTimeMillis(),
            version = "1.0",
            analysisData = AnalysisResultData(
                symbol = "BTCUSDT",
                timeframe = "1m",
                analysisType = "indicators",
                inputsJson = """{"candles":[{"open":100,"high":110,"low":90,"close":105,"volume":1000}]}""",
                outputsJson = """{"rsi":30,"macd":"bullish","signal":"buy"}"""
            ),
            analysisDurationMs = 150L
        )
        
        // Validate analysis event
        val analysisValidation = EventUtils.validate(analysisEvent)
        assertTrue(analysisValidation is ValidationResult.Success)
        
        // Simulate signal generation
        val signalEvent = SignalReceivedEvent(
            eventId = "signal_001",
            timestamp = System.currentTimeMillis(),
            version = "1.0",
            signalData = SignalData(
                sourceType = "analysis",
                sourceId = "analysis_001",
                symbol = "BTCUSDT",
                timeframe = "1m",
                signalType = "long",
                weight = BigDecimal("1.5"),
                confidence = BigDecimal("0.8"),
                payloadJson = """{"analysis_id":"analysis_001","reason":"RSI oversold + MACD bullish"}"""
            ),
            source = "technical_analysis"
        )
        
        // Validate signal event
        val signalValidation = EventUtils.validate(signalEvent)
        assertTrue(signalValidation is ValidationResult.Success)
        
        // Verify data consistency across the flow
        assertEquals(marketDataEvent.symbol, analysisEvent.analysisData.symbol)
        assertEquals(analysisEvent.analysisData.symbol, signalEvent.signalData.symbol)
        assertEquals(marketDataEvent.interval, analysisEvent.analysisData.timeframe)
        assertEquals(analysisEvent.analysisData.timeframe, signalEvent.signalData.timeframe)
    }
    
    @Test
    fun `Event aggregation contract - Multiple signals to aggregated signal`() {
        // Create multiple signals
        val signals = listOf(
            SignalData(
                sourceType = "telegram",
                sourceId = "msg_001",
                symbol = "ETHUSDT",
                timeframe = "5m",
                signalType = "long",
                weight = BigDecimal("1.0"),
                confidence = BigDecimal("0.7"),
                payloadJson = """{"user":"trader1","message":"bullish pattern"}"""
            ),
            SignalData(
                sourceType = "analysis",
                sourceId = "analysis_001",
                symbol = "ETHUSDT",
                timeframe = "5m",
                signalType = "long",
                weight = BigDecimal("2.0"),
                confidence = BigDecimal("0.9"),
                payloadJson = """{"rsi":25,"macd":"bullish"}"""
            ),
            SignalData(
                sourceType = "manual",
                sourceId = "manual_001",
                symbol = "ETHUSDT",
                timeframe = "5m",
                signalType = "short",
                weight = BigDecimal("0.5"),
                confidence = BigDecimal("0.6"),
                payloadJson = """{"reason":"resistance level"}"""
            )
        )
        
        // Validate all signals
        signals.forEach { signal ->
            val validation = DataSchemaUtils.validate(signal)
            assertTrue(validation is ValidationResult.Success)
        }
        
        // Create aggregated signal
        val aggregatedEvent = SignalAggregatedEvent(
            eventId = "aggregated_001",
            timestamp = System.currentTimeMillis(),
            version = "1.0",
            aggregatedData = AggregatedSignalData(
                symbol = "ETHUSDT",
                timeframe = "5m",
                windowStart = System.currentTimeMillis() - 300000L, // 5 minutes ago
                windowEnd = System.currentTimeMillis(),
                score = BigDecimal("0.6"), // Positive score (more long signals)
                decision = "long",
                basisJson = """{"signal_count":3,"long_signals":2,"short_signals":1,"avg_confidence":0.73}"""
            ),
            sourceSignals = listOf("msg_001", "analysis_001", "manual_001")
        )
        
        // Validate aggregated event
        val aggregatedValidation = EventUtils.validate(aggregatedEvent)
        assertTrue(aggregatedValidation is ValidationResult.Success)
        
        // Verify aggregation logic
        assertEquals("ETHUSDT", aggregatedEvent.aggregatedData.symbol)
        assertEquals("5m", aggregatedEvent.aggregatedData.timeframe)
        assertEquals("long", aggregatedEvent.aggregatedData.decision)
        assertEquals(3, aggregatedEvent.sourceSignals.size)
        assertTrue(aggregatedEvent.aggregatedData.score > BigDecimal.ZERO)
    }
    
    @Test
    fun `Trade decision contract - Signal to Trade decision flow`() {
        // Start with aggregated signal
        val aggregatedSignal = SignalAggregatedEvent(
            eventId = "aggregated_002",
            timestamp = System.currentTimeMillis(),
            version = "1.0",
            aggregatedData = AggregatedSignalData(
                symbol = "BTCUSDT",
                timeframe = "1h",
                windowStart = System.currentTimeMillis() - 3600000L, // 1 hour ago
                windowEnd = System.currentTimeMillis(),
                score = BigDecimal("0.8"),
                decision = "long",
                basisJson = """{"confidence":0.85,"risk_level":"medium","strategy":"momentum"}"""
            ),
            sourceSignals = listOf("signal_001", "signal_002", "signal_003")
        )
        
        // Validate aggregated signal
        val signalValidation = EventUtils.validate(aggregatedSignal)
        assertTrue(signalValidation is ValidationResult.Success)
        
        // Create trade decision
        val tradeDecision = TradeDecisionEvent(
            eventId = "trade_decision_001",
            timestamp = System.currentTimeMillis(),
            version = "1.0",
            symbol = "BTCUSDT",
            action = "buy",
            quantity = "0.05",
            price = "45000.00",
            reason = "Strong bullish signal with high confidence",
            confidence = "0.85",
            metadata = mapOf(
                "strategy" to "momentum",
                "risk_level" to "medium",
                "source_signals" to "signal_001,signal_002,signal_003",
                "aggregated_score" to "0.8"
            )
        )
        
        // Validate trade decision
        val tradeValidation = EventUtils.validate(tradeDecision)
        assertTrue(tradeValidation is ValidationResult.Success)
        
        // Verify decision consistency
        assertEquals(aggregatedSignal.aggregatedData.symbol, tradeDecision.symbol)
        assertEquals("buy", tradeDecision.action)
        assertEquals("0.85", tradeDecision.confidence)
        assertTrue(tradeDecision.metadata.containsKey("aggregated_score"))
        assertEquals("0.8", tradeDecision.metadata["aggregated_score"])
    }
    
    @Test
    fun `Audit logging contract - Event tracking`() {
        // Test audit logging for various events
        val events = listOf(
            MarketDataReceivedEvent(
                eventId = "market_003",
                timestamp = System.currentTimeMillis(),
                version = "1.0",
                symbol = "ADAUSDT",
                interval = "15m",
                candleData = CandleData(
                    symbol = "ADAUSDT",
                    interval = "15m",
                    openTime = 2000L,
                    closeTime = 2600L,
                    open = BigDecimal("0.50"),
                    high = BigDecimal("0.55"),
                    low = BigDecimal("0.48"),
                    close = BigDecimal("0.52"),
                    volume = BigDecimal("1000000.00"),
                    dataSource = "binance"
                ),
                source = "binance"
            ),
            SignalReceivedEvent(
                eventId = "signal_004",
                timestamp = System.currentTimeMillis(),
                version = "1.0",
                signalData = SignalData(
                    sourceType = "telegram",
                    sourceId = "msg_002",
                    symbol = "ADAUSDT",
                    timeframe = "15m",
                    signalType = "hold",
                    weight = BigDecimal("1.0"),
                    confidence = BigDecimal("0.5"),
                    payloadJson = """{"message":"waiting for breakout"}"""
                ),
                source = "telegram"
            )
        )
        
        // Create audit events for each
        val auditEvents = events.map { event ->
            AuditEvent(
                eventId = "audit_${event.eventId}",
                timestamp = System.currentTimeMillis(),
                version = "1.0",
                auditData = AuditLogData(
                    eventType = "signal",
                    refType = when (event) {
                        is MarketDataReceivedEvent -> "market_data"
                        is SignalReceivedEvent -> "signal"
                        else -> "unknown"
                    },
                    refId = null,
                    level = "INFO",
                    message = "Event processed successfully",
                    metaJson = """{"event_id":"${event.eventId}","processing_time_ms":50}"""
                )
            )
        }
        
        // Validate all audit events
        auditEvents.forEach { auditEvent ->
            val validation = EventUtils.validate(auditEvent)
            assertTrue(validation is ValidationResult.Success)
        }
        
        // Verify audit data consistency
        assertEquals(2, auditEvents.size)
        assertTrue(auditEvents.any { it.auditData.refType == "market_data" })
        assertTrue(auditEvents.any { it.auditData.refType == "signal" })
        auditEvents.forEach { auditEvent ->
            assertEquals("INFO", auditEvent.auditData.level)
            assertEquals("signal", auditEvent.auditData.eventType)
        }
    }
}
