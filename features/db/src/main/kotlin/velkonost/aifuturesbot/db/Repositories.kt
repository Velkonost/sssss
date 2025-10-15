package velkonost.aifuturesbot.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

class CandlesRepository(private val db: Database) {
    data class Candle(
        val symbol: String,
        val interval: String,
        val openTime: Long,
        val closeTime: Long,
        val open: BigDecimal,
        val high: BigDecimal,
        val low: BigDecimal,
        val close: BigDecimal,
        val volume: BigDecimal,
        val source: String
    )

    fun batchInsertIgnore(candles: List<Candle>) {
        if (candles.isEmpty()) return
        transaction(db) {
            CandlesTable.batchInsert(candles, ignore = true) { c ->
                this[CandlesTable.symbol] = c.symbol
                this[CandlesTable.interval] = c.interval
                this[CandlesTable.openTime] = c.openTime
                this[CandlesTable.closeTime] = c.closeTime
                this[CandlesTable.open] = c.open
                this[CandlesTable.high] = c.high
                this[CandlesTable.low] = c.low
                this[CandlesTable.close] = c.close
                this[CandlesTable.volume] = c.volume
                this[CandlesTable.source] = c.source
            }
        }
    }

    fun findRange(symbol: String, interval: String, startOpenTime: Long, endOpenTime: Long): List<Candle> =
        transaction(db) {
            CandlesTable
                .select {
                    (CandlesTable.symbol eq symbol) and
                    (CandlesTable.interval eq interval) and
                    (CandlesTable.openTime between startOpenTime..endOpenTime)
                }
                .orderBy(CandlesTable.openTime to SortOrder.ASC)
                .map {
                    Candle(
                        it[CandlesTable.symbol],
                        it[CandlesTable.interval],
                        it[CandlesTable.openTime],
                        it[CandlesTable.closeTime],
                        it[CandlesTable.open],
                        it[CandlesTable.high],
                        it[CandlesTable.low],
                        it[CandlesTable.close],
                        it[CandlesTable.volume],
                        it[CandlesTable.source]
                    )
                }
        }
}

class SignalsRepository(private val db: Database) {
    data class Signal(
        val sourceType: String,
        val sourceId: String?,
        val symbol: String,
        val timeframe: String,
        val signalType: String,
        val weight: BigDecimal,
        val confidence: BigDecimal,
        val payloadJson: String
    )

    fun insert(signal: Signal): Long = transaction(db) {
        SignalsTable.insertAndGetId {
            it[sourceType] = signal.sourceType
            it[sourceId] = signal.sourceId
            it[symbol] = signal.symbol
            it[timeframe] = signal.timeframe
            it[signalType] = signal.signalType
            it[weight] = signal.weight
            it[confidence] = signal.confidence
            it[payloadJson] = signal.payloadJson
        }.value
    }

    fun find(symbol: String, timeframe: String, fromTs: Long, toTs: Long): List<Signal> = transaction(db) {
        SignalsTable
            .select {
                (SignalsTable.symbol eq symbol) and
                (SignalsTable.timeframe eq timeframe) and
                (SignalsTable.createdAt.between(org.jetbrains.exposed.sql.javatime.timestampLiteral(java.time.Instant.ofEpochMilli(fromTs)), org.jetbrains.exposed.sql.javatime.timestampLiteral(java.time.Instant.ofEpochMilli(toTs))))
            }
            .orderBy(SignalsTable.createdAt to SortOrder.ASC)
            .map {
                Signal(
                    it[SignalsTable.sourceType],
                    it[SignalsTable.sourceId],
                    it[SignalsTable.symbol],
                    it[SignalsTable.timeframe],
                    it[SignalsTable.signalType],
                    it[SignalsTable.weight],
                    it[SignalsTable.confidence],
                    it[SignalsTable.payloadJson]
                )
            }
    }
}

class AggregatedSignalsRepository(private val db: Database) {
    data class Aggregated(
        val symbol: String,
        val timeframe: String,
        val windowStart: Long,
        val windowEnd: Long,
        val score: BigDecimal,
        val decision: String,
        val basisJson: String
    )

    fun upsertIgnore(items: List<Aggregated>) {
        if (items.isEmpty()) return
        transaction(db) {
            AggregatedSignalsTable.batchInsert(items, ignore = true) { a ->
                this[AggregatedSignalsTable.symbol] = a.symbol
                this[AggregatedSignalsTable.timeframe] = a.timeframe
                this[AggregatedSignalsTable.windowStart] = a.windowStart
                this[AggregatedSignalsTable.windowEnd] = a.windowEnd
                this[AggregatedSignalsTable.score] = a.score
                this[AggregatedSignalsTable.decision] = a.decision
                this[AggregatedSignalsTable.basisJson] = a.basisJson
            }
        }
    }

    fun latest(symbol: String, timeframe: String): Aggregated? = transaction(db) {
        AggregatedSignalsTable
            .select { (AggregatedSignalsTable.symbol eq symbol) and (AggregatedSignalsTable.timeframe eq timeframe) }
            .orderBy(AggregatedSignalsTable.windowEnd to SortOrder.DESC)
            .limit(1)
            .map {
                Aggregated(
                    it[AggregatedSignalsTable.symbol],
                    it[AggregatedSignalsTable.timeframe],
                    it[AggregatedSignalsTable.windowStart],
                    it[AggregatedSignalsTable.windowEnd],
                    it[AggregatedSignalsTable.score],
                    it[AggregatedSignalsTable.decision],
                    it[AggregatedSignalsTable.basisJson]
                )
            }
            .firstOrNull()
    }
}

class AnalysisResultsRepository(private val db: Database) {
    data class Result(
        val symbol: String,
        val timeframe: String,
        val analysisType: String,
        val inputsJson: String,
        val outputsJson: String
    )

    fun insert(result: Result): Long = transaction(db) {
        AnalysisResultsTable.insertAndGetId {
            it[symbol] = result.symbol
            it[timeframe] = result.timeframe
            it[analysisType] = result.analysisType
            it[inputsJson] = result.inputsJson
            it[outputsJson] = result.outputsJson
        }.value
    }
}

class AuditLogRepository(private val db: Database) {
    fun log(eventType: String, refType: String, refId: Long?, level: String, message: String, metaJson: String) {
        transaction(db) {
            AuditLogsTable.insert {
                it[AuditLogsTable.eventType] = eventType
                it[AuditLogsTable.refType] = refType
                it[AuditLogsTable.refId] = refId
                it[AuditLogsTable.level] = level
                it[AuditLogsTable.message] = message
                it[AuditLogsTable.metaJson] = metaJson
            }
        }
    }
}


