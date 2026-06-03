# Amar Savings

A simple, offline-first personal savings tracker designed to help users monitor physical cash savings using Bangladeshi currency notes.

---

## Core Philosophy

Amar Savings is designed around a single principle:

**"Track physical savings with the fewest possible taps."**

Every feature supports this goal. The app remains:
- Fast
- Offline-first
- Minimal
- Reliable
- Easy to use
- Visually modern

---

## Authentication & Backup Access

### Google Sign-In Flow

The app supports optional Google Sign-In exclusively for backup functionality.

**Splash Screen → Login Check → Dashboard**

| Scenario           | Flow                                                      |
|--------------------|-----------------------------------------------------------|
| User logged in     | Splash → Dashboard                                        |
| User not logged in | Splash → Login Screen → Dashboard (after login)           |
| User skips login   | App works fully offline. Backup button shows login option |

### Backup Authorization

- Backup features require Google Sign-In
- Users can use all tracking features without ever logging in
- When a user taps "Backup" while logged out, the app shows a login prompt
- After successful login, backup runs automatically

**No separate Settings page exists for any authentication configuration.**

---

## Savings Goal Management

### Savings Goal

Users can:
- Set a target savings amount
- Edit the target amount at any time

The goal has no deadline or target date.

The application tracks only:
- Target Amount
- Current Saved Amount
- Remaining Amount

**Example:**
- Target: ৳100,000
- Saved: ৳35,000
- Remaining: ৳65,000

### Goal Interaction

Users tap the Goal Card on the Dashboard to:
- Set the goal for the first time
- Edit an existing goal

No separate settings page is required.

---

## Adding & Withdrawing Savings

### Adding Savings (Cash In)

Users add physical cash via the Floating Action Button (FAB).

**Input Method: Counter-Input by Denomination**

The app uses note-based input optimized for Bangladeshi currency:

| Denomination | Input Field       |
|--------------|-------------------|
| ৳1000        | [Quantity] × 1000 |
| ৳500         | [Quantity] × 500  |
| ৳200         | [Quantity] × 200  |
| ৳100         | [Quantity] × 100  |
| ৳50          | [Quantity] × 50   |
| ৳20          | [Quantity] × 20   |
| ৳10          | [Quantity] × 10   |
| ৳5           | [Quantity] × 5    |
| ৳2           | [Quantity] × 2    |
| ৳1           | [Quantity] × 1    |

**User flow:**
1. Tap FAB
2. Enter quantity for each denomination they are adding
3. Total amount calculates automatically
4. Tap "Add" → entry saved

**Example:** 5 × ৳1000 + 2 × ৳500 = ৳6,000 added

### Withdrawing Savings (Cash Out)

Users can log money taken from their physical cash stash.

**Access:** Long-press FAB or select "Cash Out" from an option menu

**Input Method:** Same denomination-based counter

**User flow:**
1. Long-press FAB → select "Cash Out"
2. Enter quantities being removed
3. Total amount calculates automatically
4. Tap "Withdraw" → negative entry saved

The Dashboard updates instantly to reflect the new saved amount.

---

## Dashboard Screen

The Dashboard is the primary and central screen of the application.

### Components

| Component              | Description                                           |
|------------------------|-------------------------------------------------------|
| Savings Overview       | Current saved amount + goal amount + remaining amount |
| Goal Card              | Displays goal. Tap to set or edit                     |
| Progress Section       | Visual progress toward target                         |
| Cash Analytics         | Total notes count + distribution by denomination      |
| Backup Section         | Backup Now button + Last Backup timestamp             |
| Recent Transactions    | Last 5 entries (adds and withdrawals)                 |
| Floating Action Button | Tap = Add Cash. Long-press = Cash Out                 |
| View All Link          | Navigates to full History Screen                      |

### Navigation from Dashboard

- **View All** (next to Recent Transactions) → History Screen
- **Backup Now** → Triggers backup. Shows login prompt if not authenticated.
- **Goal Card** → Goal edit dialog

---

## History Screen

The History Screen replaces the previously planned "Transactions + Activity" tabs.

### Content

A single, scrollable list showing:

| Column       | Description                                     |
|--------------|-------------------------------------------------|
| Date & Time  | When the transaction occurred                   |
| Type         | Add (+) or Withdraw (-)                         |
| Breakdown    | Denomination quantities (e.g., "3×1000, 2×500") |
| Total Amount | Net change (+৳4,000 or -৳1,500)                 |

### What is NOT included

The following are intentionally excluded from History:
- Edit actions (no separate audit log)
- Delete actions
- Backup actions
- Restore actions

**Rationale:** An audit log for backup/restore is overkill for a simple offline tracker. Users only need to see their financial activity, not system operations.

**Last Backup timestamp** on Dashboard provides sufficient backup visibility.

### Actions Available

- Delete any entry (swipe or context menu)
- Edit any entry (tap → opens denomination counter with existing values)

---

## Backup & Restore

### Backup Strategy

- App remains fully functional offline
- All data stored locally in Room
- Cloud backup exists only for data safety

### Google Drive Backup

Backup uses the authenticated user's Google Drive.

**Authentication requirement:**
- Backup requires Google Sign-In
- If user is not logged in when tapping "Backup Now":
    - App shows a dialog: "Login to Google to enable backup"
    - User can login or cancel
    - After login, backup runs automatically

### Automatic Backup

When all conditions are met:
- User is logged in
- Internet connection is available

The app automatically creates background backups. No user action required.

### Manual Backup

Backup Now button on Dashboard:
- Logged in user → Backup runs immediately
- Logged out user → Shows login prompt first

### Restore

Users restore data from the latest Drive backup.

**Access:** Hidden/developer trigger (e.g., 5 taps on Backup button)

Upon restore:
- Existing local data is replaced
- App shows confirmation dialog before proceeding

---

## Screens

### Splash Screen

**Implementation:** Standard Android SplashScreen API

**Behavior:**
- Displays while Room and Koin initialize
- Dismisses instantly once data is ready
- **No hardcoded 1–2 second delay**

**Navigation after splash:**
- User logged in → Dashboard
- User not logged in → Login Screen

### Login Screen

**Content:**
- "Sign in with Google" button
- "Skip" link (visible and accessible)

**Behavior:**
- Tap "Sign in with Google" → Google Sign-In sheet
- Success → Navigate to Dashboard
- Skip → Navigate to Dashboard without authentication
- User remains offline-only until they explicitly log in via Backup button

### Dashboard Screen

(Described in detail above)

### History Screen

(Described in detail above)

---

## Removed Features (Explicitly Excluded)

- Activity/Audit Log tab
- Settings Screen
- Theme Selector
- About Screen
- Auto Backup Toggle
- Manual Dark/Light Mode Switch
- Goal Deadline or target date tracking
- Edit/Delete tracking in a separate log

---

## Theme System

The app automatically follows the device theme.

| Device Theme | App Theme  |
|--------------|------------|
| Dark Mode    | Dark Mode  |
| Light Mode   | Light Mode |

No theme configuration is required or provided.

---

## Technical Stack

| Component            | Technology                             |
|----------------------|----------------------------------------|
| Language             | Kotlin                                 |
| UI                   | Jetpack Compose                        |
| Architecture         | MVVM                                   |
| Dependency Injection | Koin                                   |
| Database             | Room                                   |
| Local Preferences    | DataStore                              |
| Backup               | Google Drive API + Google Sign-In      |
| Concurrency          | Coroutines + Flow                      |
| Navigation           | Navigation Compose                     |
| Authentication       | Google Sign-In (optional, backup only) |

---

## Summary of Changes from Original PRD

| Issue                    | Resolution                                                             |
|--------------------------|------------------------------------------------------------------------|
| How to add savings?      | Counter-input by denomination (10 note types)                          |
| How to withdraw?         | Long-press FAB → Cash Out with same counter input                      |
| Navigation to History    | "View All" link on Dashboard                                           |
| Activity tab scope creep | Removed entirely. Single History list with delete/edit                 |
| Google Drive auth flow   | Defined: unauthenticated Backup button shows login prompt              |
| Splash screen delay      | Removed hardcoded delay. Use Android SplashScreen API                  |
| Login requirement        | Login screen appears only if user not logged in. Skip option available |
| Backup without login     | Not possible. App prompts user to login when Backup tapped             |