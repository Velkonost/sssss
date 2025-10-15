package velkonost.aifuturesbot.db

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.InstantColumnType
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.JsonbColumnType
import java.time.Instant

object CandlesTable : LongIdTable("candles") {
    val symbol = varchar("symbol", 32)
    val interval = varchar("interval", 16)
    val openTime = long("open_time")
    val closeTime = long("close_time")
    val open = decimal("open", 18, 8)
    val high = decimal("high", 18, 8)
    val low = decimal("low", 18, 8)
    val close = decimal("close", 18, 8)
    val volume = decimal("volume", 28, 12)
    val source = varchar("source", 16)
    val insertedAt = timestamp("inserted_at").clientDefault { java.time.Instant.now() }

    init {
        index(true, symbol, interval, openTime)
        index(false, symbol, interval, openTime)
    }
}

object SignalsTable : LongIdTable("signals") {
    val sourceType = varchar("source_type", 16)
    val sourceId = varchar("source_id", 64).nullable()
    val symbol = varchar("symbol", 32)
    val timeframe = varchar("timeframe", 16)
    val signalType = varchar("signal_type", 16)
    val weight = decimal("weight", 6, 3).default(java.math.BigDecimal.ONE)
    val confidence = decimal("confidence", 6, 3).default(java.math.BigDecimal.ONE)
    val payloadJson = jsonb("payload_json")
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }

    init {
        index(false, symbol, timeframe, createdAt)
    }
}

object AggregatedSignalsTable : LongIdTable("aggregated_signals") {
    val symbol = varchar("symbol", 32)
    val timeframe = varchar("timeframe", 16)
    val windowStart = long("window_start")
    val windowEnd = long("window_end")
    val score = decimal("score", 8, 4)
    val decision = varchar("decision", 16)
    val basisJson = jsonb("basis_json")
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }

    init {
        index(true, symbol, timeframe, windowEnd)
        index(false, symbol, timeframe, windowEnd)
    }
}

object AnalysisResultsTable : LongIdTable("analysis_results") {
    val symbol = varchar("symbol", 32)
    val timeframe = varchar("timeframe", 16)
    val analysisType = varchar("analysis_type", 32)
    val inputsJson = jsonb("inputs_json")
    val outputsJson = jsonb("outputs_json")
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }

    init {
        index(false, symbol, timeframe, analysisType, createdAt)
    }
}

object AuditLogsTable : LongIdTable("audit_logs") {
    val eventType = varchar("event_type", 32)
    val refType = varchar("ref_type", 32)
    val refId = long("ref_id").nullable()
    val level = varchar("level", 16)
    val message = varchar("message", 1024)
    val metaJson = jsonb("meta_json")
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }

    init {
        index(false, refType, refId, createdAt)
        index(false, eventType, createdAt)
    }
}

// Helpers
fun Table.jsonb(name: String) = registerColumn<String>(name, JsonbColumnType(String::class))


