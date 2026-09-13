# Ignite HabitFlow

A minimal Android productivity app combining tasks, daily habits, and focused work sessions.

## MVP

- **Home** — today overview for tasks, habits, and focus minutes
- **Tasks** — create, edit, complete, and delete tasks
- **Habits** — daily completion history, streaks, monthly calendar, and completion percentage
- **Focus** — 15/25/50 minute focus sessions with pause/reset and persisted focus minutes
- **Local-first storage** — data is stored on-device with SharedPreferences
- **V1 migration** — legacy habit completion state is migrated to date-based history

## Design

Ignite HabitFlow uses a restrained, premium productivity aesthetic: generous whitespace, rounded cards, subtle surfaces, and a glass-style bottom navigation bar.

## Tech

- Kotlin
- Jetpack Compose + Material 3
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- compileSdk / targetSdk 35
- minSdk 26

## Open in Android Studio

1. Clone the repository.
2. Open the project in Android Studio.
3. Allow Gradle sync to finish.
4. Select the `app` configuration.
5. Run on an Android 8.0+ device or emulator.

The repository includes a GitHub Actions workflow that builds the debug APK on pushes and pull requests to `main`.

## Product direction

Premium features planned after the MVP include recurring tasks, subtasks, advanced statistics, custom timer presets, focus history, themes, and home customization.

## License

No open-source license has been selected yet.
