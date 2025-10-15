package velkonost.aifuturesbot.db.repositories

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import velkonost.aifuturesbot.db.SignalsTable
import java.math.BigDecimal
import java.time.Instant

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
            .selectAll()
            .where {
                (SignalsTable.symbol eq symbol) and
                (SignalsTable.timeframe eq timeframe) and
                (SignalsTable.createdAt.between(Instant.ofEpochMilli(fromTs), Instant.ofEpochMilli(toTs)))
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
