# Changelog

All notable changes to **AmarSavings** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial open-source release of the project structure, documentation, and CI

## [0.1.0] - 2026-07-04

### Added
- Offline-first personal savings tracker for Android, focused on physical cash
  savings denominated in Bangladeshi Taka
- Dashboard with current balance, recent transactions, and savings goal progress
- Add / withdraw transactions with Bangladeshi denomination quick-input
  (৳1, ৳2, ৳5, ৳10, ৳20, ৳50, ৳100, ৳200, ৳500, ৳1000)
- Savings goals — set a target amount and track progress over time
- Transaction history with filtering
- Local persistence using Room with a `SavingsRepository` as the single
  source of truth
- User preferences (DataStore) — currency, locale, theme
- Opt-in Google Sign-In + Google Drive backup & restore
- MVVM architecture with Kotlin Coroutines + Flow
- Koin dependency injection
- Jetpack Compose UI with Material 3 theme
- Navigation Compose graph
- Build configuration via Gradle 8.x + version catalog
  (`gradle/libs.versions.toml`)
- Open-source community files: `LICENSE`, `CONTRIBUTING.md`,
  `CODE_OF_CONDUCT.md`, `SECURITY.md`
- GitHub Actions CI: `assembleDebug` on every push and pull request to `main`,
  with Gradle caching and debug-APK artifact upload

### Changed
- N/A (initial release)

### Deprecated
- N/A

### Removed
- N/A

### Fixed
- N/A

### Security
- N/A

---

[Unreleased]: https://github.com/shahjalal-mahmud/Amar_Savings/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/shahjalal-mahmud/Amar_Savings/releases/tag/v0.1.0
