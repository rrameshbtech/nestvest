# NestVest

A local-first app for a more mindful financial life. Phase 1 provides one primary
profile on an Android device: required name and currency, optional built-in avatar
and color, and a stable UUID retained across reopen and supported OS restoration.
Expense entry, dashboards, family membership, synchronization, and app-managed
backup/export/import are not implemented.

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

Profile data stays in an app-private, credential-protected Room database. There is no
internet permission, analytics, public service, or cloud account. This phase relies on
Android sandboxing and device storage protection, not an additional app-managed
database encryption key. It is not a guarantee against a compromised device.

Cloud backup is excluded. API 28+ permits database-only OS local device transfer
where supported; API 26-27 excludes backup entirely because it cannot require the
local-transfer transport. Actual migration depends on the OS/OEM; the app does not
provide a transfer feature.

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

- [Specification](specs/001-local-profile-foundation/spec.md)
- [Implementation plan](specs/001-local-profile-foundation/plan.md)
- [Build, tests, acceptance evidence, and outstanding checks](specs/001-local-profile-foundation/quickstart.md)
- [Implementation task status](specs/001-local-profile-foundation/tasks.md)
- [Interactive mobile mockups](specs/001-local-profile-foundation/ui-mockups.html)

The HTML mockups are visual studies with in-memory sample data, not an Android app
or evidence of durable persistence. API 26 runtime coverage, manual TalkBack, OEM
device migration, and first-time-user acceptance measurements remain unverified.
