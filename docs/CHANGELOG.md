# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project adheres to Semantic Versioning.

## [Unreleased]
### Added
- CI/CD with GitHub Actions: build, tests, release artifacts.
- Security checks: Semgrep SAST, Dependency Review, Secrets scan.
- Detekt static analysis with CI integration.
- Secure env provider (`EnvSecureConfigProvider`) and `.env` ignore.
- Dependabot for `gradle` and `github-actions`.
- Changelog (`docs/CHANGELOG.md`) and changelog enforcer workflow for PRs.
- PostgreSQL storage layer with Exposed ORM and HikariCP connection pooling.
- Database repositories for candles, signals, aggregated signals, analysis results, and audit logs.
- Integration tests with Testcontainers PostgreSQL support.
- SSL support for PostgreSQL connections with certificate-based authentication.
- SSL configuration via environment variables (`AIFB_POSTGRES_SSL_CERT_PATH`, `AIFB_POSTGRES_SSL_MODE`).
- Data schemas module (`features:data-schemas`) with comprehensive data validation.
- Typed data schemas for candles, signals, aggregated signals, analysis results, and audit logs.
- Domain event schemas for inter-module communication (MarketDataReceivedEvent, SignalReceivedEvent, etc.).
- DataValidator with standard validation, detailed validation, batch validation, and custom rules.
- Contract tests for schema compatibility and inter-module integration.
- JSON serialization/deserialization support with Kotlinx Serialization.
- Bean Validation integration with Jakarta Validation annotations.
- Data retention module (`features:data-retention`) with TTL policies and archiving.
- Retention policies for raw and aggregated data with configurable hot/warm/cold tiers.
- FileSystemArchiveService with GZIP compression support.
- DatabaseCleanupService for automated data cleanup and archiving.
- RetentionScheduler for scheduled cleanup tasks with cron support.
- RetentionMonitoringService for metrics and storage usage tracking.
- RetentionService as a unified facade for retention operations.
- Comprehensive retention configuration with validation.

### Changed
- Kotlin updated to 2.1.0, Coroutines to 1.10.2.
- CodeQL disabled (private repository constraint).

### Removed
- ZAP DAST workflow (no public URL available).

## [0.1.0] - 2025-10-15
### Added
- Initial repository structure: `app`, `core/*`, `features/*`.
- Basic `Main.kt` entrypoint.
