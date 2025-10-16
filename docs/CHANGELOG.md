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

### Changed
- Kotlin updated to 2.1.0, Coroutines to 1.10.2.
- CodeQL disabled (private repository constraint).

### Removed
- ZAP DAST workflow (no public URL available).

## [0.1.0] - 2025-10-15
### Added
- Initial repository structure: `app`, `core/*`, `features/*`.
- Basic `Main.kt` entrypoint.
