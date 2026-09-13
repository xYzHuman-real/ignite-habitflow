# Ignite HabitFlow

Ignite HabitFlow is a minimal, local-first Android productivity app combining tasks, habits, and focused work sessions.

## Current feature set

### Home
- Today overview for tasks, habits, and focus
- Quick actions for Tasks and Focus
- Productivity Insights shortcut

### Tasks
- Create, edit, complete, and delete
- Today / Upcoming / Completed filters
- Categories
- Low / Medium / High priority
- Due-date picker
- Recurring tasks: daily, weekdays, weekly, monthly
- Subtasks with completion progress

### Habits
- Create and delete habits
- Date-based completion history
- Monthly calendar
- Current streaks
- Monthly and range completion percentages
- Backward migration from the original V1 completion format

### Focus
- Configurable focus duration: 15 / 25 / 50 minutes
- Short break: 3 / 5 / 10 minutes
- Long break: 10 / 15 / 20 minutes
- Long-break cadence: 2 / 4 / 6 focus sessions
- Focus → break → focus phase progression
- Pause / resume / reset
- Persisted focus totals and session history
- Date-aware focus analytics with legacy-history migration

### Insights
- 7-day, month, and year views
- Habit consistency for the selected period
- Today habit completion
- Overall task completion
- Period focus minutes and session count
- Average focus session length
- Best active habit streak
- Year focus activity summary

### Settings
- System / Light / Dark appearance
- Ink / Blue / Green accent
- Focus and break preferences
- Premium entry point
- Local-storage explanation

### Premium foundation
- Premium feature catalogue and entitlement state
- Premium status screen
- Planned pricing: ₹50/month, ₹200/year, ₹350 lifetime
- Billing is intentionally not enabled until Play Console product IDs and release configuration are available.

## Design

Ignite HabitFlow uses a restrained, premium productivity aesthetic: generous whitespace, rounded cards, subtle surfaces, limited accents, and a glass-style bottom navigation bar.

## Tech

- Kotlin
- Jetpack Compose + Material 3
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- compileSdk / targetSdk 35
- minSdk 26
- SharedPreferences + JSON for local persistence

## Build

GitHub Actions builds the debug APK on pushes and pull requests to `main`, and supports manual debug/release builds.

## Release work still requiring external setup

- Final Play Store listing assets and screenshots
- Final application icon artwork
- Play Console subscription/lifetime product configuration
- Google Play Billing integration and purchase verification
- Release signing keystore / secrets
- Final privacy-policy URL and Play Console declarations

## License

No open-source license has been selected yet.
