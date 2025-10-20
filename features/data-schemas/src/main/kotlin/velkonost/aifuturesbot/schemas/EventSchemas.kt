package velkonost.aifuturesbot.schemas

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Схемы событий для межмодульного взаимодействия
 * Определяют контракты между продюсерами и консюмерами
 */

@Serializable
sealed class DomainEvent {
    abstract val eventId: String
    abstract val timestamp: Long
    abstract val version: String
}

@Serializable
data class MarketDataReceivedEvent(
    override val eventId: String,
    override val timestamp: Long,
    override val version: String = "1.0",
    val symbol: String,
    val interval: String,
    val candleData: CandleData,
    val source: String
) : DomainEvent()

@Serializable
data class SignalReceivedEvent(
    override val eventId: String,
    override val timestamp: Long,
    override val version: String = "1.0",
    val signalData: SignalData,
    val source: String
) : DomainEvent()

@Serializable
data class SignalAggregatedEvent(
    override val eventId: String,
    override val timestamp: Long,
    override val version: String = "1.0",
    val aggregatedData: AggregatedSignalData,
    val sourceSignals: List<String> // IDs исходных сигналов
) : DomainEvent()

@Serializable
data class AnalysisCompletedEvent(
    override val eventId: String,
    override val timestamp: Long,
    override val version: String = "1.0",
    val analysisData: AnalysisResultData,
    val analysisDurationMs: Long
) : DomainEvent()

@Serializable
data class TradeDecisionEvent(
    override val eventId: String,
    override val timestamp: Long,
    override val version: String = "1.0",
    val symbol: String,
    val action: String, // "buy", "sell", "hold"
    val quantity: String? = null,
    val price: String? = null,
    val reason: String,
    val confidence: String,
    val metadata: Map<String, String> = emptyMap()
) : DomainEvent()

@Serializable
data class AuditEvent(
    override val eventId: String,
    override val timestamp: Long,
    override val version: String = "1.0",
    val auditData: AuditLogData
) : DomainEvent()

/**
 * Утилиты для работы с событиями
 */
object EventUtils {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    /**
     * Создание события с автоматической генерацией ID и timestamp
     */
    fun createEvent(
        eventType: String,
        data: Any,
        version: String = "1.0"
    ): DomainEvent {
        val eventId = "${eventType}_${System.currentTimeMillis()}_${(0..9999).random()}"
        val timestamp = System.currentTimeMillis()
        
        return when (eventType) {
            "market_data_received" -> {
                val marketData = data as MarketDataParams
                MarketDataReceivedEvent(
                    eventId, timestamp, version, 
                    marketData.symbol, marketData.interval, 
                    marketData.candleData, marketData.source
                )
            }
            "signal_received" -> {
                val signalData = data as SignalParams
                SignalReceivedEvent(eventId, timestamp, version, signalData.signalData, signalData.source)
            }
            "signal_aggregated" -> {
                val aggregatedData = data as AggregatedParams
                SignalAggregatedEvent(eventId, timestamp, version, aggregatedData.aggregatedData, aggregatedData.sourceSignals)
            }
            "analysis_completed" -> {
                val analysisData = data as AnalysisParams
                AnalysisCompletedEvent(eventId, timestamp, version, analysisData.analysisData, analysisData.duration)
            }
            "trade_decision" -> {
                val tradeData = data as TradeParams
                TradeDecisionEvent(
                    eventId, timestamp, version, 
                    tradeData.symbol, tradeData.action, 
                    tradeData.quantity, tradeData.price, 
                    tradeData.reason, tradeData.confidence, 
                    tradeData.metadata
                )
            }
            "audit" -> {
                val auditData = data as AuditLogData
                AuditEvent(eventId, timestamp, version, auditData)
            }
            else -> throw IllegalArgumentException("Unknown event type: $eventType")
        }
    }
    
    /**
     * Сериализация события в JSON
     */
    fun toJson(event: DomainEvent): String = json.encodeToString(serializer<DomainEvent>(), event)
    
    /**
     * Десериализация события из JSON
     */
    fun fromJson(jsonString: String): DomainEvent = json.decodeFromString(serializer<DomainEvent>(), jsonString)
    
    /**
     * Валидация события
     */
    fun validate(event: DomainEvent): ValidationResult {
        return when (event) {
            is MarketDataReceivedEvent -> DataSchemaUtils.validate(event.candleData)
            is SignalReceivedEvent -> DataSchemaUtils.validate(event.signalData)
            is SignalAggregatedEvent -> DataSchemaUtils.validate(event.aggregatedData)
            is AnalysisCompletedEvent -> DataSchemaUtils.validate(event.analysisData)
            is TradeDecisionEvent -> ValidationResult.Success // Простая валидация для TradeDecisionEvent
            is AuditEvent -> DataSchemaUtils.validate(event.auditData)
        }
    }
}

// Вспомогательные типы для создания событий
data class MarketDataParams(val symbol: String, val interval: String, val candleData: CandleData, val source: String)
data class SignalParams(val signalData: SignalData, val source: String)
data class AggregatedParams(val aggregatedData: AggregatedSignalData, val sourceSignals: List<String>)
data class AnalysisParams(val analysisData: AnalysisResultData, val duration: Long)
data class TradeParams(
    val symbol: String, 
    val action: String, 
    val quantity: String?, 
    val price: String?, 
    val reason: String, 
    val confidence: String, 
    val metadata: Map<String, String>
)
