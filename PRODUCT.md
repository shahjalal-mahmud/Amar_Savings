# Amar Savings

A simple, offline-first personal savings tracker designed to help users monitor physical cash savings using Bangladeshi currency notes.

---

## Savings Goal Management

### Savings Goal

Users can:

* Set a target savings amount
* Edit the target amount at any time

The goal does not have a deadline or target date.

The application only tracks:

* Target Amount
* Current Saved Amount
* Remaining Amount

Example:

Target: ৳100,000

Saved: ৳35,000

Remaining: ৳65,000

Users can tap the Goal Card on the Dashboard to:

* Set the goal for the first time
* Edit an existing goal

No separate settings page is required.

---

## Backup & Restore

### Backup Strategy

The application remains fully functional offline.

All data is stored locally using Room.

Cloud backup is provided only for data safety.

### Automatic Backup

When an internet connection is available:

* The application automatically creates a backup in the background.
* No user action is required.

### Manual Backup

A Backup button is available directly on the Dashboard.

Users can manually trigger a backup at any time.

### Restore

Users can restore their data from the latest available backup.

---

## Dashboard Screen

The Dashboard is the primary and central screen of the application.

It contains:

### Savings Overview

* Current Saved Amount
* Goal Amount
* Remaining Amount

### Goal Card

Displays the current savings goal.

Tap actions:

* Set Goal
* Edit Goal

### Progress Section

Shows progress toward the target amount.

### Cash Analytics

Displays:

* Total notes saved
* Distribution of each denomination

### Backup Section

* Backup Now Button
* Last Backup Information

### Recent Transactions

Shows the latest savings entries.

### Floating Action Button

Used to add a new savings entry.

---

## Screens

### Splash Screen

Purpose:

* Load local data
* Initialize backup services
* Navigate to Dashboard

Duration:

1–2 seconds

---

### Dashboard Screen

Main application screen.

Contains:

* Savings Summary
* Goal Management
* Progress Tracking
* Analytics
* Recent Transactions
* Backup Actions

---

### History Screen

Contains:

#### Transactions Tab

Displays all savings entries.

#### Activity Tab

Displays:

* Add actions
* Edit actions
* Delete actions
* Backup actions
* Restore actions

---

## Removed Features

The following features are intentionally excluded:

* Settings Screen
* Theme Selector
* About Screen
* Auto Backup Toggle
* Manual Dark/Light Mode Switch
* Goal Deadline
* Goal Date Tracking

---

## Theme System

The application automatically follows the device theme.

Examples:

* Device Dark Mode → App Dark Mode
* Device Light Mode → App Light Mode

No theme configuration is required.

---

## Technical Stack

### Language

Kotlin

### UI

Jetpack Compose

### Architecture

MVVM

### Dependency Injection

Koin

### Database

Room

### Local Preferences

DataStore

### Backup

Google Drive Backup

### Concurrency

Coroutines + Flow

### Navigation

Navigation Compose

---

## Product Philosophy

Amar Savings is designed around a single principle:

"Track physical savings with the fewest possible taps."

Every feature should support this goal.

The app should remain:

* Fast
* Offline-first
* Minimal
* Reliable
* Easy to use
* Visually modern
