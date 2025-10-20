package velkonost.aifuturesbot.db.repositories

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import velkonost.aifuturesbot.db.AnalysisResultsTable

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
