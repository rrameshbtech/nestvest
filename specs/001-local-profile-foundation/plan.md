# Implementation Plan: Phase 1 — Local Profile Foundation

**Branch**: `001-local-profile-foundation` | **Date**: 2026-10-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-local-profile-foundation/spec.md`

## Summary

Create one primary user profile entirely on the Android device, with a short setup flow,
required name and currency, optional appearance, durable local persistence, and a stable
identity suitable for future family association. Use Kotlin, Jetpack Compose, Room over
SQLite, and an MVI-style unidirectional state flow. Recommend Material 3 for Jetpack
Compose as the native UI component system, using the
[mobile UI mockups](./ui-mockups.html) as the visual and interaction reference.
Keep profile data in app-private
storage, disable cloud backup, and allow supported OS-level local device migration without
adding app-managed export/import or synchronization.
Resume unfinished setup through Android saved state after system-initiated background
process termination, reconciling with Room before displaying a draft or saved profile.
Ship English interface text with English fallback while preserving localization readiness.

## Technical Context

**Language/Version**: Kotlin; version selected with the Android project setup.

**Primary Dependencies**: Jetpack Compose with the recommended
`androidx.compose.material3:material3` component library, AndroidX ViewModel and lifecycle-aware
collection, Room, Kotlin coroutines/Flow, and Android platform localization/accessibility
support. Use ViewModel `SavedStateHandle` for restorable draft values, not a separate draft
database. Exact dependency versions are selected when the app module is established.

**Storage**: Room backed by SQLite in app-private internal storage. Use Android's
platform-provided app isolation and device storage encryption. Configure OS data
extraction to exclude cloud backup while permitting supported local device-to-device
migration. Do not add app-managed backup/export/import in this phase.

**Testing**: JUnit-based unit and Room DAO tests, coroutine/Flow state tests, Compose UI
tests, saved-state/process-recreation tests, and Android accessibility checks plus manual
TalkBack verification. Rotation alone does not prove process-death recovery.

**Target Platform**: Native Android mobile application; minimum supported Android version
is set when the app module is initialized, with Phase 1 behavior limited to APIs available
from Android 8.0 (API 26) onward.

**Project Type**: Android mobile app. The repository currently contains no application
source or Android build files; the plan assumes a new single Android app module.

**Performance Goals**: At least 90% of first-time users complete required setup in under
two minutes (SC-001). Saving and reopening a local profile must complete as part of the
normal interactive flow without waiting for network access.

**Constraints**: One primary profile per app installation; no cloud registration,
internet-facing data services, analytics, or app-managed transfer. Profile data remains
local. Name and preferred currency are required; appearance is optional. Preserve the
profile UUID when supported external OS-level migration restores the profile.
Draft recovery is limited to system-restored tasks, not force-stop, task dismissal, or
reboot. Persisted profiles retain their existing durability guarantees. English is the only
shipped interface language; use English fallback and device-locale currency formatting.

**Scale/Scope**: One profile per installation, one profile-creation flow, locally
available currency choices, no family membership or sync, and no more than the profile
fields defined in the spec.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Local-first privacy**: Pass. Profile creation, persistence, and reads are on-device;
  cloud backup is excluded and no internet service is required.
- **Secure local storage**: Pass. Profile records stay in app-private storage protected by
  Android platform isolation and storage encryption. No export of the raw database is
  introduced.
- **Stable, merge-ready identity**: Pass. Generate a random UUID once at profile creation
  and persist it as the profile's stable identity; do not use auto-incrementing identity.
- **Zero-friction setup**: Pass. Only name and currency are required; appearance is
  optional and setup does not require network connectivity. FR-010 draft recovery avoids
  re-entry after system-restored interruptions without treating a draft as a profile.
- **Localization and accessibility**: Pass. User-facing text is localizable and the
  Compose flow must expose accessible semantics, readable contrast, and operable controls.
  English-only delivery does not hard-code a single-locale design; fallback, currency
  presentation, expanded-text, and RTL behavior remain explicit validation targets.
- **Quality gates**: Pass. Tests cover validation, save failure, persistence, identity,
  offline behavior, localization readiness, accessibility, and system-restored drafts.
- **Scope conflict**: None. The selected Android and local database technologies support
  the requirements without adding a public service or expanding into family sync.

### Post-design review

The Material 3 recommendation and mockup mapping preserve all gates above. Native Compose
components replace HTML controls; there is no WebView, React runtime, or external asset
dependency. The mockup's simulated save is not evidence of persistence or data protection;
the Android implementation must satisfy the original storage and error-handling gates.
Recovery stores only lightweight draft values in local Android saved state, with no new
cloud path or persistent draft table. Database reconciliation prevents duplicate profiles
and false success; the clarified language and acceptance populations introduce no gate
violations.

## Recovery and Acceptance Design

Save the name, currency code, and applied nullable avatar/color identifiers through
`SavedStateHandle` as draft values change. Do not serialize repositories, database objects,
or transient in-flight operations. On ViewModel recreation, enter Loading and read Room
before acting on restored UI state:

1. A committed profile takes precedence over any draft; show Saved and clear draft state.
2. Confirmed absence restores the draft into editable Setup without automatically
   resubmitting an interrupted save.
3. A read failure shows Load error and retains the draft; retry reads again before
   allowing creation. Never interpret an error as absence.

Use the existing atomic single-profile creation path on explicit retry, preserving a
committed UUID and preventing duplicates. Unapplied personalization-sheet edits need not
resume; the applied selection is part of the recoverable draft. Saved-state restoration
depends on Android saving the background task; there is no promise of durable drafts
after force-stop, task removal, reboot, or interruption before state capture.

Keep acceptance populations separate: SC-003 measures otherwise valid offline creation
flows; SC-005 measures failed profile writes with retained input and retry. Invalid-field
guidance and read-error recovery are separate acceptance tests. SC-001/SC-004 remain
first-time-user measurements, not substitutes for functional test results.

## UI Design Reference

[ui-mockups.html](./ui-mockups.html) contains four interactive mobile screen studies at a
390 x 844 reference aspect ratio. Use it for hierarchy, whitespace, restrained color, and
interaction intent, not as a fixed-size layout or production implementation. The feature
specification takes precedence over illustrative copy, sample data, and demo behavior.
The [UI contract](./contracts/profile-ui.md) defines the native flow and state behavior.

| Mock screen | Native Material 3 mapping | Behavior |
|---|---|---|
| 01 / The essentials | `Scaffold`, `TopAppBar`, `OutlinedTextField`, currency picker, `Button` | Only name and currency are required; show field errors without discarding entered values |
| 02 / A personal touch | `ModalBottomSheet`, selectable avatar/color controls, `Button`, `TextButton` | Open from Choose; apply or skip returns to setup; dismiss keeps the previous selection |
| 03 / An honest save failure | Error-colored `Surface`, text summary, retry `Button` | Preserve entered values; show a truthful failure with retry, never a success confirmation |
| 04 / Saved profile | `OutlinedCard`, profile summary, status text | Show only after a confirmed local save, or load the existing profile on return |

Personalization is optional rather than a mandatory onboarding step; its full-screen
mock study maps to a bottom sheet in the Android app. While saving, show an accessible
progress indicator and prevent duplicate submissions. Render loading and load-error states
before deciding between setup and an existing profile; a failed read is not an empty profile.

Theme Material 3 with the mockup's monochrome base, muted sage accents, sans-serif hierarchy,
flat outlined surfaces, and minimal elevation. Use `MaterialTheme` color, typography, and
shape roles instead of duplicating component styling. Do not enable dynamic color by default,
so the reference palette remains predictable. Avoid gradients and decorative shadows.
Keep controls at least 48 dp in touch size, expose selection/error state to TalkBack, and
allow content to scroll under large text, keyboard, and small-window constraints.
Localize all visible copy and use start/end alignment for right-to-left layouts.

## Project Structure

### Documentation (this feature)

```text
specs/001-local-profile-foundation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── ui-mockups.html
├── contracts/
│   └── profile-ui.md
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
app/
├── src/
│   ├── main/
│   │   ├── java/com/nestvest/
│   │   │   ├── profile/
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   └── ui/
│   │   │   │       └── theme/
│   │   │   └── NestVestApplication.kt
│   │   ├── res/
│   │   │   ├── values/strings.xml
│   │   │   └── xml/
│   │   └── AndroidManifest.xml
│   ├── test/java/com/nestvest/profile/
│   └── androidTest/java/com/nestvest/profile/
├── build.gradle.kts
└── proguard-rules.pro
build.gradle.kts
settings.gradle.kts
gradle/libs.versions.toml
```

**Structure Decision**: Establish a single native Android application module. Keep profile
storage and repository concerns in `profile/data`, profile rules in `profile/domain`, and
Compose screens plus ViewModel state handling in `profile/ui`. Separate JVM/unit tests
from Android/Compose instrumentation tests. These are planned paths; no app module exists
in the repository yet.

## Complexity Tracking

No constitution violations or additional architectural projects are proposed.
