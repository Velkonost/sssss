# AIFuturesBot

[![CI](https://github.com/velkonost/ai-bot/actions/workflows/ci.yml/badge.svg)](https://github.com/velkonost/ai-bot/actions/workflows/ci.yml)
[![Release](https://github.com/velkonost/ai-bot/actions/workflows/release.yml/badge.svg)](https://github.com/velkonost/ai-bot/actions/workflows/release.yml)

Новый улучшенный фьючерсный бот на основе опыта ml-futures-bot.

## Цели
- Модульная архитектура (app/core/features)
- DI через Koin
- Асинхронность на Coroutines
- Exposed + PostgreSQL (возможна SQLite для локальных задач)
- DataFrame для расчёта индикаторов
- Интеграции через собственные SDK: Binance, Telegram, Technical Analysis

## Входная точка
`app` модуль. `Main.kt` инициализирует DI, загружает конфиги, запускает SDK и обработчики.

## Запуск (черновик)
```bash
./gradlew :app:run
```

## Планы
- Реализовать SDK.init() и загрузку конфигов
- Добавить базовые контроллеры и сервисы (индикаторы, статистика, решения)
- Настроить интеграцию с Telegram и Binance
- Обновлять README по мере развития

## Безопасная настройка (Linux/JVM)

- Никогда не коммитьте секреты. Используйте переменные окружения или секреты CI/CD.
- Провайдер читает ключи из переменных окружения с префиксом `AIFB_`.
- Пример переменных (см. `.env.example`):

```bash
export AIFB_BINANCE_API_KEY="..."
export AIFB_BINANCE_API_SECRET="..."
export AIFB_TELEGRAM_BOT_TOKEN="..."
export AIFB_POSTGRES_URL="jdbc:postgresql://localhost:5432/aifb"
export AIFB_POSTGRES_USER="..."
export AIFB_POSTGRES_PASSWORD="..."
```

- В коде используйте `EnvSecureConfigProvider` из `core/configs`.
- Проверка утечек: GitHub Actions запускает TruffleHog (workflow `Secrets Scan`).
