package velkonost.aifuturesbot.app

import kotlinx.coroutines.coroutineScope

suspend fun main(): Unit = coroutineScope {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        println("Ошибка в потоке ${thread.name}: ${throwable.stackTraceToString()}")
    }

    // Инициализация будет добавлена в следующих задачах: SDK.init(), загрузка конфигов,
    // настройка Binance/Telegram SDKs и запуск обработчиков
}
