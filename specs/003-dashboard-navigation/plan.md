# Implementation Plan: Phase 3 — Dashboard Navigation

**Branch**: `003-dashboard-navigation` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification and the interaction/visual reference [ui-mockups.html](./ui-mockups.html).

## Summary

Add a calm, local-first dashboard after saved profile setup. The dashboard provides
direct destinations for Add expense, Expenses, and Reminders; lets the user pin or unpin
those options, retaining the choice locally; and displays the optional current-day
expense summary only when it can be read accurately. Follow the Phase 3 mockup for
structure, pinned defaults, Customize/More flow, feedback, and failure states. Preserve
the existing amount-first capture, expense history, reminder settings, and reminder
notification entry behavior.

## Technical Context

**Language/Version**: Kotlin 2.2.10, JVM target 17

**Primary Dependencies**: Existing AndroidX Compose Material 3, Lifecycle ViewModel and
Compose integrations, Room 2.7.1, and Kotlin coroutines; no new dependency is required.

**Storage**: Existing app-private Room database (`nestvest.db`), currently schema
version 2. Add one local dashboard-preference record through an additive migration.
Expense summaries are derived from existing local expense records and are not persisted
as duplicate financial data.

**Testing**: Existing JUnit 4 and coroutine unit tests; Compose UI and accessibility
instrumentation tests; Room migration/persistence instrumentation tests; Android lint
and debug assembly.

**Target Platform**: Android API 26 and later; compile/target SDK 36.

**Project Type**: Native Android mobile application using Jetpack Compose.

**Performance Goals**: Show the dashboard without waiting for preference or summary
reads; load saved choices and local summary asynchronously. No numeric dashboard
latency threshold is specified.

**Constraints**: Offline operation; no network, public cloud, or analytics access for
dashboard data. Preserve existing profile gating and expense/reminder behavior. Support
locales, assistive technology, larger text, and small screens. Do not present missing
expense data as a zero total or stale data as current.

**Scale/Scope**: One local primary profile and three currently available destinations
(Add expense, Expenses, Reminders). Later-phase planning, wealth, and family features
are not actionable destinations in this phase. No arbitrary pin-count limit is
introduced.

## Constitution Check

### Pre-Design

- **I. Local-First Privacy and Data Sovereignty**: PASS. Read and save dashboard
  preferences and expense summaries locally; no network access is introduced.
- **II. Zero-Friction, User-Controlled Capture**: PASS. Add expense opens the existing
  amount-first capture flow, and dashboard summaries/customization do not add required
  capture fields or steps.
- **III. Conflict-Resilient Household Data**: PASS. This feature adds only a local
  preference, not a syncable financial record or sync behavior.
- **IV. Kakeibo-Led Financial Wellbeing**: PASS. No budgeting or category behavior is
  changed. Any expense summary presents recorded values without forecasts.
- **V. Calm, Honest, and Legible Experience**: PASS. Follow the restrained mockup and
  keep status understandable without color, with accessible controls and localized
  strings/formatting.
- **Additional Constraints**: PASS. No cloud, public-network, or analytics integration;
  app remains usable offline.

**Gate result**: PASS. No constitution conflicts identified.

## Project Structure

### Documentation (this feature)

```text
specs/003-dashboard-navigation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── dashboard-ui.md
└── ui-mockups.html
```

### Source Code (repository root)

```text
app/src/main/java/com/nestvest/
├── MainActivity.kt
├── dashboard/
│   ├── data/       # Room preference entity, DAO, and repository
│   ├── domain/     # Dashboard options, preference and summary state
│   └── ui/         # Dashboard route, state holder, and Compose screens
├── expense/        # Existing capture, history, reminders, and local data
└── profile/        # Existing profile setup, persistence, and profile gate

app/src/test/java/com/nestvest/dashboard/
app/src/androidTest/java/com/nestvest/dashboard/
```

**Structure Decision**: Extend the existing single-module native Android app. Keep
dashboard preference/domain/UI concerns in a `dashboard` feature package, compose it
with the saved-profile route in `MainActivity`, and route to existing expense screens
instead of duplicating expense or reminder behavior. Add the Room schema migration and
tests alongside the existing profile database and Android test patterns.

## Design Decisions

- Treat the Phase 3 HTML as an interaction and appearance reference, not a data source
  or implementation. Its sample expenses and in-memory preference behavior are not
  production behavior.
- The initial pinned set is Add expense and Expenses; Reminders remains available under
  More. All three destinations are configurable and remain available regardless of
  pinning.
- Customization is a draft until the user chooses Save choices. On save failure, keep
  the previous persisted preference and tell the user the change was not saved.
- Compute an optional Today count and amount from current local expenses in the
  profile's currency. Differentiate no recorded entries from a meaningful zero total.
  If the read fails, omit the amount and clearly state that the summary is unavailable;
  keep the Expenses destination usable.
- Keep profile setup as the initial route for a missing profile. Once a profile is
  saved, show the dashboard on ordinary launch. Honor the existing reminder
  notification capture request by opening expense capture rather than losing or
  misrouting that action.
- Store stable option identifiers rather than display strings. Unknown or unavailable
  options must not create broken navigation; the catalog determines destinations
  available in the installed app.

## Constitution Check (Post-Design)

- **I. Local-first**: PASS. Preference writes and summary reads use local Room data;
  no external service or analytics path is proposed.
- **II. User-controlled capture**: PASS. The capture destination retains the existing
  amount-only save behavior and the mockup's route to that flow.
- **III. Conflict-resilient data**: PASS. No household-shared entity or sync semantics
  are added. The preference is installation-local.
- **IV. Honest financial information**: PASS. Dashboard summary is a current local
  aggregation only, is omitted on read failure, and distinguishes no data from zero.
- **V. Calm, accessible, localized experience**: PASS. Contract covers accessible
  names/state, non-color status, responsive scrolling, locale-aware currency/date
  presentation, and localization-ready strings.
- **Additional constraints**: PASS. Local-only and offline behavior remain intact.

**Gate result**: PASS. The detailed design introduces no new constitution violations.

## Complexity Tracking

No constitution exceptions or additional architectural projects are required.
