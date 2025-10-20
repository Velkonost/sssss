package velkonost.aifuturesbot.schemas

import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import jakarta.validation.Validation
import java.math.BigDecimal

/**
 * Валидатор данных для интеграции с модулями системы
 * Обеспечивает валидацию на входе для всех данных
 */
class DataValidator {
    
    private val validatorFactory: ValidatorFactory = Validation.buildDefaultValidatorFactory()
    private val validator: Validator = validatorFactory.validator
    
    /**
     * Валидация данных свечей
     */
    fun validateCandleData(candle: CandleData): ValidationResult {
        val violations = validator.validate(candle)
        return if (violations.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(violations.map { "${it.propertyPath}: ${it.message}" })
        }
    }
    
    /**
     * Валидация сигналов
     */
    fun validateSignalData(signal: SignalData): ValidationResult {
        val violations = validator.validate(signal)
        return if (violations.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(violations.map { "${it.propertyPath}: ${it.message}" })
        }
    }
    
    /**
     * Валидация агрегированных сигналов
     */
    fun validateAggregatedSignalData(aggregated: AggregatedSignalData): ValidationResult {
        val violations = validator.validate(aggregated)
        return if (violations.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(violations.map { "${it.propertyPath}: ${it.message}" })
        }
    }
    
    /**
     * Валидация результатов анализа
     */
    fun validateAnalysisResultData(analysis: AnalysisResultData): ValidationResult {
        val violations = validator.validate(analysis)
        return if (violations.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(violations.map { "${it.propertyPath}: ${it.message}" })
        }
    }
    
    /**
     * Валидация аудит логов
     */
    fun validateAuditLogData(audit: AuditLogData): ValidationResult {
        val violations = validator.validate(audit)
        return if (violations.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(violations.map { "${it.propertyPath}: ${it.message}" })
        }
    }
    
    /**
     * Валидация событий
     */
    fun validateEvent(event: DomainEvent): ValidationResult {
        return when (event) {
            is MarketDataReceivedEvent -> validateCandleData(event.candleData)
            is SignalReceivedEvent -> validateSignalData(event.signalData)
            is SignalAggregatedEvent -> validateAggregatedSignalData(event.aggregatedData)
            is AnalysisCompletedEvent -> validateAnalysisResultData(event.analysisData)
            is TradeDecisionEvent -> ValidationResult.Success // Простая валидация
            is AuditEvent -> validateAuditLogData(event.auditData)
        }
    }
    
    /**
     * Валидация с детальными ошибками
     */
    fun validateWithDetails(data: Any): DetailedValidationResult {
        val violations = validator.validate(data)
        return if (violations.isEmpty()) {
            DetailedValidationResult.Success
        } else {
            DetailedValidationResult.Failure(
                violations.map { violation ->
                    ValidationError(
                        field = violation.propertyPath.toString(),
                        message = violation.message,
                        invalidValue = violation.invalidValue?.toString(),
                        constraintType = violation.constraintDescriptor?.annotation?.annotationClass?.simpleName
                    )
                }
            )
        }
    }
    
    /**
     * Батчевая валидация
     */
    fun validateBatch(dataList: List<Any>): BatchValidationResult {
        val results = dataList.mapIndexed { index, data ->
            val violations = validator.validate(data)
            if (violations.isEmpty()) {
                null
            } else {
                BatchValidationError(
                    index = index,
                    errors = violations.map { "${it.propertyPath}: ${it.message}" }
                )
            }
        }.filterNotNull()
        
        return if (results.isEmpty()) {
            BatchValidationResult.Success
        } else {
            BatchValidationResult.Failure(results)
        }
    }
    
    /**
     * Валидация с кастомными правилами
     */
    fun validateWithCustomRules(data: Any, customRules: List<(Any) -> ValidationResult>): ValidationResult {
        // Сначала стандартная валидация
        val standardResult = validateWithDetails(data)
        if (standardResult is DetailedValidationResult.Failure) {
            return ValidationResult.Failure(standardResult.errors.map { "${it.field}: ${it.message}" })
        }
        
        // Затем кастомные правила
        return customRules.firstOrNull { rule ->
            val ruleResult = rule(data)
            ruleResult is ValidationResult.Failure
        }?.let { rule ->
            rule(data)
        } ?: ValidationResult.Success
    }
}

/**
 * Детальный результат валидации
 */
sealed class DetailedValidationResult {
    object Success : DetailedValidationResult()
    data class Failure(val errors: List<ValidationError>) : DetailedValidationResult()
}

/**
 * Ошибка валидации с деталями
 */
data class ValidationError(
    val field: String,
    val message: String,
    val invalidValue: String?,
    val constraintType: String?
)

/**
 * Результат батчевой валидации
 */
sealed class BatchValidationResult {
    object Success : BatchValidationResult()
    data class Failure(val errors: List<BatchValidationError>) : BatchValidationResult()
}

/**
 * Ошибка батчевой валидации
 */
data class BatchValidationError(
    val index: Int,
    val errors: List<String>
)

/**
 * Кастомные правила валидации для бизнес-логики
 */
object CustomValidationRules {
    
    /**
     * Проверка корректности цен свечей
     */
    fun validateCandlePrices(candle: CandleData): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (candle.high < candle.low) {
            errors.add("High price (${candle.high}) must be >= low price (${candle.low})")
        }
        
        if (candle.high < candle.open || candle.high < candle.close) {
            errors.add("High price (${candle.high}) must be >= open (${candle.open}) and close (${candle.close}) prices")
        }
        
        if (candle.low > candle.open || candle.low > candle.close) {
            errors.add("Low price (${candle.low}) must be <= open (${candle.open}) and close (${candle.close}) prices")
        }
        
        if (candle.closeTime <= candle.openTime) {
            errors.add("Close time (${candle.closeTime}) must be after open time (${candle.openTime})")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Проверка логики сигналов
     */
    fun validateSignalLogic(signal: SignalData): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (signal.weight < BigDecimal.ZERO) {
            errors.add("Weight cannot be negative")
        }
        
        if (signal.confidence < BigDecimal.ZERO || signal.confidence > BigDecimal.ONE) {
            errors.add("Confidence must be between 0 and 1")
        }
        
        // Проверка JSON payload
        try {
            kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(signal.payloadJson)
        } catch (e: kotlinx.serialization.SerializationException) {
            errors.add("Payload JSON is invalid: ${e.message}")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Проверка временных окон
     */
    fun validateTimeWindow(aggregated: AggregatedSignalData): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (aggregated.windowEnd <= aggregated.windowStart) {
            errors.add("Window end (${aggregated.windowEnd}) must be after window start (${aggregated.windowStart})")
        }
        
        val windowDuration = aggregated.windowEnd - aggregated.windowStart
        val maxWindowDuration = 24 * 60 * 60 * 1000L // 24 hours
        if (windowDuration > maxWindowDuration) {
            errors.add("Window duration (${windowDuration}ms) exceeds maximum allowed (${maxWindowDuration}ms)")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
}
