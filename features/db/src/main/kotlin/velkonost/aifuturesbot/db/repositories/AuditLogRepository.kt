package velkonost.aifuturesbot.db.repositories

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import velkonost.aifuturesbot.db.AuditLogsTable

data class AuditLogEntry(
    val eventType: String,
    val refType: String,
    val refId: Long?,
    val level: String,
    val message: String,
    val metaJson: String
)

class AuditLogRepository(private val db: Database) {
    fun log(entry: AuditLogEntry) {
        transaction(db) {
            AuditLogsTable.insert {
                it[AuditLogsTable.eventType] = entry.eventType
                it[AuditLogsTable.refType] = entry.refType
                it[AuditLogsTable.refId] = entry.refId
                it[AuditLogsTable.level] = entry.level
                it[AuditLogsTable.message] = entry.message
                it[AuditLogsTable.metaJson] = entry.metaJson
            }
        }
    }
}
