package velkonost.aifuturesbot.db.repositories

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import velkonost.aifuturesbot.db.AggregatedSignalsTable
import java.math.BigDecimal

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
            .selectAll()
            .where { (AggregatedSignalsTable.symbol eq symbol) and (AggregatedSignalsTable.timeframe eq timeframe) }
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
