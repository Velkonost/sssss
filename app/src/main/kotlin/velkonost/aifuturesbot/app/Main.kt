package velkonost.aifuturesbot.app

import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.SQLException
import velkonost.aifuturesbot.db.DatabaseFactory

suspend fun main(): Unit = coroutineScope {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        println("Ошибка в потоке ${thread.name}: ${throwable.stackTraceToString()}")
    }

    // 1) Инициализация БД (создание недостающих таблиц)
    val db = initializeDatabase()

    // 2) Проверка доступности соединения и печать списка таблиц в текущей схеме
    printExistingTables(db)

    // Далее: SDK.init(), загрузка конфигов, настройка интеграций и запуск обработчиков
}

private fun initializeDatabase() = try {
    DatabaseFactory.init(createSchema = true)
} catch (e: SQLException) {
    println("[DB] Ошибка инициализации: ${e.message}")
    throw e
}

private fun printExistingTables(db: org.jetbrains.exposed.sql.Database) {
    try {
        val existing = mutableListOf<String>()
        transaction(db) {
            exec(
                """
                SELECT tablename FROM pg_catalog.pg_tables 
                WHERE schemaname = current_schema()
                """.trimIndent()
            ) { rs ->
                while (rs.next()) existing += rs.getString(1)
            }
        }
        println("[DB] Подключение успешно. Таблицы в схеме: ${existing.sorted()}")
    } catch (e: SQLException) {
        println("[DB] Ошибка проверки схемы: ${e.message}")
        throw e
    }
}
