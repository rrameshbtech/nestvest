# Implementation Plan: Phase 3 — The Kakeibo Engine (Planning & Reflection)

**Branch**: `004-kakeibo-planning-reflection` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification and visual/interaction reference [ui-mockups.html](./ui-mockups.html).

## Summary

Add offline monthly Kakeibo planning, optional weekly pacing, category progress and
explicit unassigned-expense assignment, monthly reflection, first-use learning, and a
current-month dashboard tile. Implement the feature in the existing Kotlin/Jetpack
Compose app, persist plans and reflections in the app's Room database, and derive all
spending summaries from current local expense records. Treat the HTML mockup as a
visual and interaction reference; do not reuse its synthetic data or in-memory storage.
Keep existing profile setup, expense capture, reminders, and dashboard customization
behavior intact.

## Technical Context

**Language/Version**: Kotlin 2.2.10, JVM target 17

**Primary Dependencies**: Existing Jetpack Compose Material 3, Lifecycle ViewModel,
Room 2.7.1, and Kotlin coroutines; SQLCipher for Android provides encrypted Room
storage. No other new dependency is required.

**Storage**: Existing app-private Room database (`nestvest.db`), currently schema
version 3. Encrypt the database using SQLCipher; wrap its random key with Android
Keystore. Convert the existing plaintext database without data loss before opening
Room, then apply an additive schema migration to version 4 for monthly plans, category
amounts, weekly allocations, reflections, and learning preference.

**Testing**: Existing JUnit 4/coroutine unit tests, Compose UI and accessibility
instrumentation tests, Room migration/persistence instrumentation tests, Android lint,
and debug assembly.

**Target Platform**: Android API 26 and later; compile/target SDK 36.

**Project Type**: Native Android mobile application using Jetpack Compose.

**Performance Goals**: No numeric latency target is specified. Keep monthly summaries
derived from local, month-bounded expense reads and load database state asynchronously
without blocking navigation.

**Constraints**: Operate offline; do not send household financial data to network
services or analytics. Use exact currency minor units, device-local calendar periods,
and current saved expenses; report read/write failures without stale or success-shaped
fallbacks. Preserve existing amount-first capture and reminder deep links. Support
localization, assistive technology, larger text, and small screens.

**Scale/Scope**: One local primary profile and calendar-month plans, with four fixed
Kakeibo categories. Family sharing, sync, foreign-currency conversion, income, and
automatic transfers are out of scope.

## Constitution Check

### Pre-Design

- **I. Local-First Privacy and Data Sovereignty**: PASS. Store plans, reflections, and
  learning preference in the app-private SQLCipher-encrypted Room database, protect
  its random key with Android Keystore, and derive progress from local expenses.
  Introduce no network or analytics path.
- **II. Zero-Friction, User-Controlled Capture**: PASS. Do not change expense capture.
  Learning, the overall available amount, weekly pacing, and reflection are optional;
  users can save a plan without a total available amount.
- **III. Conflict-Resilient Household Data**: PASS. No sync behavior is added. Use
  stable UUIDs or stable composite keys for records and avoid auto-increment IDs as
  cross-device identity.
- **IV. Kakeibo-Led Financial Wellbeing**: PASS. Use the four canonical categories;
  distinguish plans from actual expenses, expose overspending, and keep reflection
  non-judgmental.
- **V. Calm, Honest, and Legible Experience**: PASS. Follow the supplied mockup's
  established palette and interaction patterns, with localized text/amounts/dates,
  accessible labels, and status communicated without color alone.
- **Additional Constraints**: PASS. Keep data available offline; use local
  notifications only if needed (the plan nudge is in-app, not a notification).

**Gate result**: PASS. No constitution conflict or unresolved specification
clarification identified.

## Project Structure

### Documentation (this feature)

```text
specs/004-kakeibo-planning-reflection/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── kakeibo-ui.md
└── ui-mockups.html
```

### Source Code (repository root)

```text
app/src/main/java/com/nestvest/
├── MainActivity.kt                         # Connect dashboard destination to Kakeibo
├── kakeibo/
│   ├── data/                                # Room entities, DAO, repository
│   ├── domain/                              # Plans, periods, progress, reflection rules
│   └── ui/                                  # Route, ViewModel, Compose screens
├── dashboard/                               # Add fixed Kakeibo summary tile
├── expense/                                 # Reuse local expense reads; guarded assignment
└── profile/data/ProfileDatabase.kt          # Additive schema migration

app/src/test/java/com/nestvest/kakeibo/      # Domain and ViewModel tests
app/src/androidTest/java/com/nestvest/kakeibo/ # Compose, accessibility, persistence tests
```

**Structure Decision**: Extend the existing single-module Android app. Keep Kakeibo
domain, Room persistence, and Compose UI in a dedicated `kakeibo` package. Wire its
destination through the existing dashboard/MainActivity route; extend the existing
expense data path only for a guarded explicit category assignment. Add the Room schema
change through `ProfileDatabase`, preserving all existing profile, expense, reminder,
and dashboard rows.

## Design Decisions

- **Mockup authority**: Follow [ui-mockups.html](./ui-mockups.html) for the dashboard
  entry, learning slides, monthly plan/review, optional weekly pacing, category progress
  and chart, explicit assignment, and reflection screens. Its values and session-only
  persistence are illustrative; the spec and this plan govern production behavior.
- **Encrypted storage and migration**: Encrypt the entire Room database using
  SQLCipher for Android. Generate a random per-install database key, wrap it with a
  non-exportable Android Keystore AES key, and persist only the wrapped key in
  app-private preferences. Before Room opens an existing schema-v3 database, convert
  its contents into an encrypted temporary database, verify it, atomically replace the
  original, and retain recoverable source data until verification succeeds. Never
  silently fall back to plaintext on key or migration failure.
- **Local persistence**: Add Room-backed repositories following existing typed
  read/write results, Flow observation, `withTransaction`, and cancellation handling.
  Commit a monthly plan and its category/weekly rows atomically. A failed write leaves
  the prior plan intact and keeps the user's draft available for retry.
- **Money and periods**: Persist amounts in currency minor units, matching `Expense`.
  Use exact arithmetic (including overflow-safe aggregate totals); never use floating
  point to calculate balances. Compute calendar-month boundaries in the device zone
  and weekly segments using the existing locale-based `WeekFields` convention, clipped
  to the plan month.
- **Progress and assignment**: Derive category totals, daily chart values, dashboard
  totals, and health from the latest successful local expense read. Treat unassigned
  and other-currency expenses as separate groups. Assignment must atomically recheck
  that an expense is still unassigned, in the selected month, and in the plan currency
  before changing only its category.
- **Dashboard entry**: Add a dedicated Kakeibo tile, separate from the dashboard's
  user-customizable pin/unpin choices but visually in the same tile section, so the
  required no-plan prompt or current summary remains visible and accessible. Loading
  or failed summary reads keep the destination available and never reuse a stale amount.
- **Learning preference**: Persist whether the first-use prompt remains unseen or was
  viewed/dismissed. Once viewed or dismissed, do not repeat the prompt; keep a subtle
  learning action on Kakeibo home.
- **UI and charts**: Use existing Compose/Material 3 and localized string resources.
  Reuse the expense scaffold, brand header, primary/secondary buttons, and paper/ink/
  sage tokens. Show a derived read-only categories-plus-savings overall amount when
  omitted, without persisting a budget cap; slider maxima use the explicit or derived
  overall amount. Review uses currency-formatted cards.
  Render the compact daily chart with Compose drawing primitives and accessible
  textual totals/labels; no external chart or navigation library is needed.
- **No public API**: The feature is a native app flow with no server or third-party
  interface. Define a UI contract for the screens, navigation, accessibility, and
  failure behavior rather than introducing an external API contract.

## Constitution Check (Post-Design)

- **I. Local-first and privacy**: PASS. The schema and repositories use the existing
  app-private Room database encrypted by SQLCipher; its random key is protected by
  Android Keystore. Existing plaintext data is converted before Room use and is not
  silently reopened in plaintext after failure. Summaries are local derivations; no
  network, public cloud, or analytics dependency is introduced.
- **II. User-controlled capture**: PASS. Existing amount-only expense capture remains
  unchanged. Optional steps can be skipped; plan creation does not modify expenses.
- **III. Stable identity and data integrity**: PASS. Monthly plan/reflection identity
  uses UUIDs or unique natural keys, category/week rows have stable composite keys, and
  multi-row plan writes and guarded assignment are transactional.
- **IV. Honest Kakeibo presentation**: PASS. Four canonical categories, same-currency
  expense inclusion, explicit unassigned/foreign-currency states, negative overspend,
  and live expense totals are specified.
- **V. Accessibility and localization**: PASS. The UI contract requires semantic
  controls, non-color status labels, localized dates/money, screen-reader-readable
  chart information, and responsive layouts.
- **Additional constraints**: PASS. Offline persistence and platform-protected
  app-private storage are retained; no external service or push notification is
  required.

**Post-design gate result**: PASS. At-rest protection is implemented through SQLCipher
with Android Keystore key wrapping; migration must preserve current records. No
constitution violation or unresolved clarification remains.
