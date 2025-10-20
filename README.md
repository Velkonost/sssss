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
export AIFB_BINANCE_API_KEY="your_binance_api_key"
export AIFB_BINANCE_API_SECRET="your_binance_api_secret"
export AIFB_TELEGRAM_BOT_TOKEN="your_telegram_bot_token"
export AIFB_POSTGRES_URL="jdbc:postgresql://your-host:5432/your-database"
export AIFB_POSTGRES_USER="your_db_user"
export AIFB_POSTGRES_PASSWORD="your_db_password"
export AIFB_POSTGRES_SSL_CERT_PATH="configs/ssl/ca-cert.pem"
export AIFB_POSTGRES_SSL_MODE="require"
```

- В коде используйте `EnvSecureConfigProvider` из `core/configs`.
- Проверка утечек: GitHub Actions запускает TruffleHog (workflow `Secrets Scan`).

### SSL соединение с PostgreSQL

Для безопасного подключения к удаленной PostgreSQL базе данных:

1. **Сертификат**: Замените содержимое `configs/ssl/ca-cert.pem` на реальный сертификат от провайдера БД
2. **SSL режим**: Используйте `verify-full` для максимальной безопасности или `require` для базовой защиты
3. **Переменные окружения**: Настройте `AIFB_POSTGRES_SSL_CERT_PATH` и `AIFB_POSTGRES_SSL_MODE`

Пример для облачной PostgreSQL:
```bash
export AIFB_POSTGRES_URL="jdbc:postgresql://your-cloud-host:5432/your-database?ssl=true&sslmode=require"
export AIFB_POSTGRES_SSL_CERT_PATH="configs/ssl/ca-cert.pem"
export AIFB_POSTGRES_SSL_MODE="require"
```

## Security checks

- Semgrep SAST: `.github/workflows/semgrep.yml` (push/PR). Configure `SEMGREP_APP_TOKEN` for uploads
- Dependency Review: `.github/workflows/dependency-review.yml` (PR gate)
- Secrets Scan: `.github/workflows/secrets-scan.yml` (push/PR)

## Linting / Static analysis

- Detekt (Kotlin): локально `./gradlew detekt`, отчёты: `build/reports/detekt/`

## Changelog

- См. `docs/CHANGELOG.md` для истории изменений.

## PRs via GitHub CLI (gh)

- Установка: `brew install gh`
- Аутентификация (без интерктива):
  - Создайте PAT с правами `repo` и экспортируйте: `export GH_TOKEN=...`
  - Запустите: `./scripts/gh-auth.sh`
- Создание PR:
  ```bash
  gh pr create --base main --head feature/your-branch \
    --title "feat: short title" \
    --body "Описание изменений"
  ```
