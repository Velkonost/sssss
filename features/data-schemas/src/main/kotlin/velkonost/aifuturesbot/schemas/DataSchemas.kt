package velkonost.aifuturesbot.schemas

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.Contextual
import kotlinx.serialization.serializer
import java.math.BigDecimal
import jakarta.validation.constraints.*

/**
 * Схемы данных для валидации и сериализации
 * Основаны на структуре таблиц БД
 */

@Serializable
data class CandleData(
    @field:NotBlank(message = "Symbol cannot be blank")
    @field:Size(max = 32, message = "Symbol must be at most 32 characters")
    val symbol: String,
    
    @field:NotBlank(message = "Interval cannot be blank")
    @field:Size(max = 16, message = "Interval must be at most 16 characters")
    val interval: String,
    
    @field:NotNull(message = "Open time cannot be null")
    @field:Min(value = 0, message = "Open time must be positive")
    val openTime: Long,
    
    @field:NotNull(message = "Close time cannot be null")
    @field:Min(value = 0, message = "Close time must be positive")
    val closeTime: Long,
    
    @field:NotNull(message = "Open price cannot be null")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Open price must be positive")
    @Contextual
    val open: BigDecimal,
    
    @field:NotNull(message = "High price cannot be null")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "High price must be positive")
    @Contextual
    val high: BigDecimal,
    
    @field:NotNull(message = "Low price cannot be null")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Low price must be positive")
    @Contextual
    val low: BigDecimal,
    
    @field:NotNull(message = "Close price cannot be null")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Close price must be positive")
    @Contextual
    val close: BigDecimal,
    
    @field:NotNull(message = "Volume cannot be null")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Volume must be non-negative")
    @Contextual
    val volume: BigDecimal,
    
    @field:NotBlank(message = "Data source cannot be blank")
    @field:Size(max = 16, message = "Data source must be at most 16 characters")
    val dataSource: String
) {
    init {
        require(closeTime > openTime) { "Close time must be after open time" }
        require(high >= low) { "High price must be >= low price" }
        require(high >= open && high >= close) { "High price must be >= open and close prices" }
        require(low <= open && low <= close) { "Low price must be <= open and close prices" }
    }
}

@Serializable
data class SignalData(
    @field:NotBlank(message = "Source type cannot be blank")
    @field:Size(max = 16, message = "Source type must be at most 16 characters")
    val sourceType: String,
    
    @field:Size(max = 64, message = "Source ID must be at most 64 characters")
    val sourceId: String? = null,
    
    @field:NotBlank(message = "Symbol cannot be blank")
    @field:Size(max = 32, message = "Symbol must be at most 32 characters")
    val symbol: String,
    
    @field:NotBlank(message = "Timeframe cannot be blank")
    @field:Size(max = 16, message = "Timeframe must be at most 16 characters")
    val timeframe: String,
    
    @field:NotBlank(message = "Signal type cannot be blank")
    @field:Size(max = 16, message = "Signal type must be at most 16 characters")
    val signalType: String,
    
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Weight must be non-negative")
    @field:DecimalMax(value = "10.0", inclusive = true, message = "Weight must be at most 10.0")
    @Contextual
    val weight: BigDecimal = BigDecimal.ONE,
    
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Confidence must be non-negative")
    @field:DecimalMax(value = "1.0", inclusive = true, message = "Confidence must be at most 1.0")
    @Contextual
    val confidence: BigDecimal = BigDecimal.ONE,
    
    @field:NotBlank(message = "Payload JSON cannot be blank")
    val payloadJson: String
) {
    init {
        require(signalType in listOf("long", "short", "hold", "buy", "sell")) { 
            "Signal type must be one of: long, short, hold, buy, sell" 
        }
        require(sourceType in listOf("telegram", "binance", "manual", "api")) { 
            "Source type must be one of: telegram, binance, manual, api" 
        }
    }
}

@Serializable
data class AggregatedSignalData(
    @field:NotBlank(message = "Symbol cannot be blank")
    @field:Size(max = 32, message = "Symbol must be at most 32 characters")
    val symbol: String,
    
    @field:NotBlank(message = "Timeframe cannot be blank")
    @field:Size(max = 16, message = "Timeframe must be at most 16 characters")
    val timeframe: String,
    
    @field:NotNull(message = "Window start cannot be null")
    @field:Min(value = 0, message = "Window start must be positive")
    val windowStart: Long,
    
    @field:NotNull(message = "Window end cannot be null")
    @field:Min(value = 0, message = "Window end must be positive")
    val windowEnd: Long,
    
    @field:NotNull(message = "Score cannot be null")
    @field:DecimalMin(value = "-1.0", inclusive = true, message = "Score must be >= -1.0")
    @field:DecimalMax(value = "1.0", inclusive = true, message = "Score must be <= 1.0")
    @Contextual
    val score: BigDecimal,
    
    @field:NotBlank(message = "Decision cannot be blank")
    @field:Size(max = 16, message = "Decision must be at most 16 characters")
    val decision: String,
    
    @field:NotBlank(message = "Basis JSON cannot be blank")
    val basisJson: String
) {
    init {
        require(windowEnd > windowStart) { "Window end must be after window start" }
        require(decision in listOf("long", "short", "hold", "buy", "sell")) { 
            "Decision must be one of: long, short, hold, buy, sell" 
        }
    }
}

@Serializable
data class AnalysisResultData(
    @field:NotBlank(message = "Symbol cannot be blank")
    @field:Size(max = 32, message = "Symbol must be at most 32 characters")
    val symbol: String,
    
    @field:NotBlank(message = "Timeframe cannot be blank")
    @field:Size(max = 16, message = "Timeframe must be at most 16 characters")
    val timeframe: String,
    
    @field:NotBlank(message = "Analysis type cannot be blank")
    @field:Size(max = 32, message = "Analysis type must be at most 32 characters")
    val analysisType: String,
    
    @field:NotBlank(message = "Inputs JSON cannot be blank")
    val inputsJson: String,
    
    @field:NotBlank(message = "Outputs JSON cannot be blank")
    val outputsJson: String
) {
    init {
        require(analysisType in listOf("indicators", "patterns", "volume", "momentum", "trend")) { 
            "Analysis type must be one of: indicators, patterns, volume, momentum, trend" 
        }
    }
}

@Serializable
data class AuditLogData(
    @field:NotBlank(message = "Event type cannot be blank")
    @field:Size(max = 32, message = "Event type must be at most 32 characters")
    val eventType: String,
    
    @field:NotBlank(message = "Reference type cannot be blank")
    @field:Size(max = 32, message = "Reference type must be at most 32 characters")
    val refType: String,
    
    val refId: Long? = null,
    
    @field:NotBlank(message = "Level cannot be blank")
    @field:Size(max = 16, message = "Level must be at most 16 characters")
    val level: String,
    
    @field:NotBlank(message = "Message cannot be blank")
    @field:Size(max = 1024, message = "Message must be at most 1024 characters")
    val message: String,
    
    @field:NotBlank(message = "Meta JSON cannot be blank")
    val metaJson: String
) {
    init {
        require(level in listOf("DEBUG", "INFO", "WARN", "ERROR", "FATAL")) { 
            "Level must be one of: DEBUG, INFO, WARN, ERROR, FATAL" 
        }
        require(eventType in listOf("decision", "signal", "trade", "error", "system")) { 
            "Event type must be one of: decision, signal, trade, error, system" 
        }
    }
}

/**
 * Утилиты для работы со схемами данных
 */
object DataSchemaUtils {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    /**
     * Валидация данных с помощью Bean Validation
     */
    fun <T> validate(data: T): ValidationResult {
        val validator = jakarta.validation.Validation.buildDefaultValidatorFactory().validator
        val violations = validator.validate(data)
        
        return if (violations.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(violations.map { "${it.propertyPath}: ${it.message}" })
        }
    }
    
    /**
     * Сериализация в JSON
     */
    inline fun <reified T> toJson(data: T): String = json.encodeToString(serializer<T>(), data)
    
    /**
     * Десериализация из JSON
     */
    inline fun <reified T> fromJson(jsonString: String): T = json.decodeFromString(serializer<T>(), jsonString)
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<String>) : ValidationResult()
}
