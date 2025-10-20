package velkonost.aifuturesbot.db.repositories

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import velkonost.aifuturesbot.db.CandlesTable
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
                this[CandlesTable.dataSource] = c.source
            }
        }
    }

    fun findRange(symbol: String, interval: String, startOpenTime: Long, endOpenTime: Long): List<Candle> =
        transaction(db) {
            CandlesTable
                .selectAll()
                .where {
                    (CandlesTable.symbol eq symbol) and
                    (CandlesTable.interval eq interval) and
                    (CandlesTable.openTime greaterEq startOpenTime) and
                    (CandlesTable.openTime lessEq endOpenTime)
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
                        it[CandlesTable.dataSource]
                    )
                }
        }
}
