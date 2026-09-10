# Taski

**Plan smart. Do more.**

Taski is an offline-first Android productivity app for university students and young professionals. It helps you capture work, rank it with a transparent rule-based priority engine, build a daily focus plan, run a task-linked timer, and track progress on the device — without an account or internet connection.

## Problem

Coursework, deadlines, and personal work compete for the same hours. Generic to-do lists do not explain *why* one task should come first, and cloud apps require accounts and connectivity. Taski keeps planning, focus, and progress local and readable.

## Main features

- One-time onboarding, then a Home dashboard with live priorities, plan snapshot, and progress
- Task list: add, edit, complete, and delete tasks stored on the device
- Priority result screen that shows the score, level, weighted factors, and reasons
- Today’s Focus Plan: packs pending tasks into the available time
- Task-specific focus timer (25 min, 50 min, or custom) with saved sessions
- Progress: completions, focus time, daily streak, and recent activity
- Local deadline reminders for upcoming and overdue incomplete tasks
- Profile copy that explains offline storage and reminders

## Technology stack

- Language: Kotlin
- UI: XML layouts and Material 3
- Architecture: MVVM
- Persistence: Room (SQLite on device)
- Navigation: Android Navigation Component
- Async UI: ViewModel and LiveData

## Architecture

Fragments observe ViewModels. ViewModels talk to repositories. Repositories use Room DAOs. Priority scoring, focus-plan packing, reminder planning, and progress stats are local Kotlin classes — not remote services.

## Database

Room stores two main tables on the device:

- `tasks` — title, description, deadline, importance, estimated effort, category, priority score, completion timestamps
- `focus_sessions` — duration and outcome, linked to a task with `ON DELETE CASCADE`

There is no MySQL, backend server, or cloud database.

## AI prioritization

Taski’s “AI priority” is a **transparent rule-based heuristic**, not machine learning and not an external AI model or API.

Weighted score (0–100):

- **Urgency 40%** — how close the deadline is
- **Importance 40%** — Low / Medium / High
- **Effort 20%** — estimated duration (shorter work ranks a little higher)

Levels: High 70–100, Medium 40–69, Low 0–39. The same deadline, importance, effort, and current time always produce the same score. The priority screen lists the factors and reasons so the ranking is inspectable.

## Testing

- JVM unit tests for the priority calculator, focus-plan builder, timer engine, progress stats, and reminder planner
- In-memory Room integration tests (Robolectric) for DAO, repository, completion/progress, focus sessions, plan data flow, and reminder scheduling
- Instrumented smoke test for the application package name

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest   # optional; needs a device or emulator
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Offline-first behavior

Taski does not require Firebase, a backend, MySQL, an external API, an internet connection, or an external AI service. The app has no `INTERNET` permission. Tasks, sessions, progress, and reminders stay on the phone.

## How to build and run

1. Install [Android Studio](https://developer.android.com/studio) with an Android SDK.
2. Open this repository as an Android project.
3. Sync Gradle, then run the `app` configuration on an emulator or device (API 24+).

Configuration:

- Application ID: `com.example.taski`
- minSdk 24, targetSdk 36, compileSdk 37.1
- versionName `1.0` (versionCode 1)

## Project scope

Taski is a local productivity client. It does **not** include accounts, authentication, cloud sync, networking, Firebase, machine learning models, or third-party AI APIs.
