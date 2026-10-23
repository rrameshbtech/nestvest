# NestVest

A local-first app for a more mindful financial life. Create one primary profile, then
record an expense using only an amount and **Save expense**. Add Expense stays focused
on amount entry; **Expenses** in its app bar opens today's history, with current
day/week/month filters and a fixed bottom add action. Optional vendor,
Kakeibo category, and receipt details can be added independently later or through
**Add details now**. Amount correction preserves the expense identity; deletion requires
confirmation. Each record retains the creating profile's stable UUID.

Daily reminders are local, optional, off by default, and reachable from history settings.
Android may delay their approximate delivery time. Notification denial never blocks
capture. Dashboards, family membership, synchronization, ingestion, and app-managed
backup/export/import remain outside this phase.

## Android development

- JDK 21, Android SDK platform 36 and build tools 36.0.0.
- Android 8.0 / API 26 or newer for installation.
- Set `ANDROID_HOME` to your SDK or configure an untracked `local.properties`.
- Use the checked-in Gradle 9.4.1 wrapper. AGP 9.2.1 provides built-in Kotlin 2.2.10;
  the app uses Jetpack Compose Material 3, Room/SQLite, and MVI-style state flow.

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
# With a running emulator or connected device:
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
```

### Git hooks

Enable the checked-in hooks once per clone:

```sh
git config --local core.hooksPath .githooks
```

The pre-commit and pre-push hooks both compile the debug app, run Android lint, and
run unit tests. Connected instrumentation tests remain a separate step because they
require a running emulator or connected device.

Launch NestVest from the device launcher or run the app configuration in Android
Studio. `:app:assembleRelease` builds a minified, unsigned release APK; configure
release signing outside version control before distributing.

## Data and recovery boundaries

Profile and expense data stay in an app-private, credential-protected Room database.
Receipt images are copied into private internal files, not retained as provider URIs.
There is no
internet permission, analytics, public service, or cloud account. This phase relies on
Android sandboxing and device storage protection, not an additional app-managed
database encryption key. It is not a guarantee against a compromised device.

Cloud backup is excluded. API 28+ permits database and receipt-file OS local device transfer
where supported; API 26-27 excludes backup entirely because it cannot require the
local-transfer transport. Actual migration depends on the OS/OEM; the app does not
provide a transfer feature.

The version-1 profile database migrates additively to version 2 without deleting the
profile. Expense amounts use exact currency minor units. Creator identity is provenance,
not a household-sync or duplicate-resolution algorithm. Future planning must retain the
canonical Kakeibo mapping; ingestion must add separate parsed/confirmed provenance;
trusted household sync must define member resolution, conflicts, duplicate detection,
and deletion tombstones before transfer is implemented.

If receipt cleanup fails after a record change commits, the change stays saved and the
app reports cleanup pending. Unreferenced files are retried on the next detail operation
or app restart. Reminder date claims suppress dates at or before the most recent claim,
so moving the calendar backward cannot repeat an already claimed day.

Unfinished setup uses Android saved task state. Returning through a system-restored
task after background process termination resumes entered values and applied
appearance. Room is checked first: a committed profile wins, absence restores the
draft for explicit retry, and a read error exposes read retry rather than new setup.
Draft recovery is not guaranteed after force-stop, task dismissal, reboot, or before
Android captures task state. Successfully saved profiles remain durable independently
of this draft limitation.

English is the sole shipped UI language, with English fallback for untranslated
languages and device-locale currency labels. RTL and expanded-text pseudolocales
test readiness; they are not additional translations.

## Feature references

### Daily expense capture

- [Specification](specs/002-daily-expense-capture/spec.md)
- [Implementation plan](specs/002-daily-expense-capture/plan.md)
- [Validation and outstanding checks](specs/002-daily-expense-capture/quickstart.md)
- [Implementation task status](specs/002-daily-expense-capture/tasks.md)
- [Interactive mobile mockups](specs/002-daily-expense-capture/ui-mockups.html)

### Local profile foundation

- [Specification](specs/001-local-profile-foundation/spec.md)
- [Implementation plan](specs/001-local-profile-foundation/plan.md)
- [Build, tests, acceptance evidence, and outstanding checks](specs/001-local-profile-foundation/quickstart.md)
- [Implementation task status](specs/001-local-profile-foundation/tasks.md)
- [Interactive mobile mockups](specs/001-local-profile-foundation/ui-mockups.html)

The HTML mockups are visual studies with in-memory sample data, not an Android app
or evidence of durable persistence. API 26 runtime coverage, manual TalkBack, OEM
device migration, and first-time-user acceptance measurements remain unverified.
