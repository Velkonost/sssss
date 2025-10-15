package velkonost.aifuturesbot.app

import kotlinx.coroutines.coroutineScope
import org.koin.java.KoinJavaComponent.inject

suspend fun main(): Unit = coroutineScope {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        println("Ошибка в потоке ${thread.name}: ${throwable.stackTraceToString()}")
    }

    // TODO: SDK.init(), load configs, setup Binance/Telegram SDKs, start listeners
}
