package velkonost.aifuturesbot.db.repositories

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import velkonost.aifuturesbot.db.AuditLogsTable

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
