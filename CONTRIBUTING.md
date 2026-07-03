# Contributing to AmarSavings

Thank you for your interest in contributing to **AmarSavings** — a simple,
offline-first personal savings tracker for Android, built specifically for
Bangladeshi Taka. Every contribution, from typo fixes to new features, helps
make physical-cash savings tracking easier for everyone.

This document explains how to set up the project locally, propose changes,
and submit pull requests.

---

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [How to Contribute](#how-to-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Features](#suggesting-features)
  - [Your First Code Contribution](#your-first-code-contribution)
  - [Pull Requests](#pull-requests)
- [Style Guidelines](#style-guidelines)
- [Testing](#testing)
- [Reporting Security Issues](#reporting-security-issues)

---

## Code of Conduct

By participating in this project, you agree to abide by our
[Code of Conduct](CODE_OF_CONDUCT.md). Please read it before contributing.

## Getting Started

### Prerequisites

- **JDK 17** (required by Android Gradle Plugin 8.x)
- **Android Studio Hedgehog (2023.1.1)** or newer
- **Android SDK Platform 36** (compileSdk) — install via SDK Manager
- **Git**

### Clone & Build

```bash
# Clone
git clone https://github.com/shahjalal-mahmud/Amar_Savings.git
cd Amar_Savings

# Build a debug APK
./gradlew assembleDebug

# Install on a connected device or running emulator
./gradlew installDebug

# Open in Android Studio
# File → Open → select the Amar_Savings directory
```

> On Windows, use `gradlew.bat` instead of `./gradlew`.

The first build will download Gradle and all dependencies, which may take a
few minutes. Subsequent builds are fast thanks to Gradle's incremental
compilation.

## Project Structure

```
app/src/main/java/com/appriyo/amarsavings/
├── AmarSavingsApp.kt           # Application class, Koin initialisation
├── MainActivity.kt             # Single-activity host
├── navigation/                 # Navigation Compose graph
├── data/
│   ├── db/                     # Room entities, DAOs, AppDatabase, AppPreferences
│   └── repository/             # SavingsRepository — single source of truth
├── di/                         # Koin modules
├── ui/
│   ├── components/             # Reusable composables (TransactionItem, dialogs, sheets)
│   ├── dashboard/              # DashboardScreen
│   ├── history/                # HistoryScreen
│   └── theme/                  # Color, Theme, Type
├── util/                       # Formatters, helpers
└── viewmodel/                  # DashboardViewModel, TransactionViewModel, HistoryViewModel
```

For a full overview see the [Project Structure section in the README](README.md#-project-structure).

## How to Contribute

### Reporting Bugs

Open a [Bug Report](https://github.com/shahjalal-mahmud/Amar_Savings/issues/new?template=bug_report.yml). Please include:

- A clear, descriptive title
- Steps to reproduce the issue
- Expected vs. actual behaviour
- Device, Android version, and app version
- Relevant logcat output or stack traces (if any)

### Suggesting Features

Open a [Feature Request](https://github.com/shahjalal-mahmud/Amar_Savings/issues/new?template=feature_request.yml). Please include:

- The problem you are trying to solve
- Your proposed solution
- Alternatives you have considered
- Any mockups or screenshots (if applicable)

### Your First Code Contribution

Look for issues labelled
[`good first issue`](https://github.com/shahjalal-mahmud/Amar_Savings/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)
or [`help wanted`](https://github.com/shahjalal-mahmud/Amar_Savings/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22).
These are scoped tasks that don't require deep familiarity with the codebase.

### Pull Requests

1. **Fork** the repository and create a feature branch from `main`:
   ```bash
   git checkout -b feature/my-improvement
   ```
2. **Make your changes.** Keep commits focused and use
   [Conventional Commits](#commit-messages) prefixes.
3. **Run the build locally** to make sure everything still works:
   ```bash
   ./gradlew assembleDebug
   ```
4. **Push** your branch and open a Pull Request targeting `main`.
5. **Fill out** the [pull request template](.github/PULL_REQUEST_TEMPLATE.md).
   Link the related issue using `Fixes #<issue-number>` or
   `Related to #<issue-number>`.
6. **Wait for review.** A maintainer will review your PR and may request
   changes. Please be patient — reviews are an opportunity to learn, not a
   judgement.

> **Tip:** Keep PRs small and focused. A PR that fixes one bug or adds one
> feature is much easier to review than a sweeping change touching many
> systems.

## Style Guidelines

### Kotlin

- Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use **4-space indentation** (no tabs).
- Prefer `val` over `var` where mutation is not needed.
- Keep functions small and focused; favour expression bodies for one-liners.
- Avoid wildcard imports (`import kotlinx.android.synthetic.main.*`) — prefer
  explicit imports.

### Compose

- Keep composables pure; hoist state to ViewModels.
- Use Material 3 components from `androidx.compose.material3`.
- Name previews `<FunctionName>Preview` and tag them with `@Preview`.

### Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/) for commit
messages. Examples:

```
feat: add biometric lock on app launch
fix: history screen crashes on empty state
docs: clarify clone command in README
refactor: extract denomination parser into util
test: add unit tests for Formatters
chore: bump Koin to 4.0.4
```

## Testing

The project currently has minimal automated tests. New code should be
covered by unit tests where it makes sense:

- **Unit tests** (JVM): `app/src/test/java/` — use JUnit 4
- **Instrumented tests** (device/emulator): `app/src/androidTest/java/` —
  use AndroidX Test + Espresso or Compose UI test

Run the full test suite with:

```bash
./gradlew test connectedAndroidTest
```

If your change adds a new feature, please add a test that exercises the
happy path. Bug fixes should ideally include a regression test.

## Reporting Security Issues

**Please do not open a public issue for security vulnerabilities.** See our
[Security Policy](SECURITY.md) for instructions on how to report
vulnerabilities privately via GitHub's private vulnerability reporting
feature.

---

Thank you for taking the time to contribute. সঞ্চয় করুন, স্বাধীন হন — _Save, Become Independent._ 💰
