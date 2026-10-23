# Implementation Plan: Phase 2 — Ultra-Fast Daily Expense Capture

**Branch**: `002-daily-expense-capture` | **Date**: 2026-10-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-daily-expense-capture/spec.md`

## Summary

Add fast, amount-only local expense capture to the existing Android profile
application. Use Kotlin, Jetpack Compose Material 3, the existing ViewModel/StateFlow
pattern, and the existing Room database. Expenses use stable UUID identities and
validated currency minor units, retain the creating profile's stable identity, and
support optional confirmed vendor/category/receipt details, period-filtered history,
confirmed deletion, and opt-in local reminders. Preserve the four-screen flow and key
interactions in [ui-mockups.html](./ui-mockups.html): amount capture (including a subtle
immediate-enrichment action), day/week/month history, a persistent expense summary
during enrichment, and reminder settings reachable from history.

## Technical Context

**Language/Version**: Kotlin 2.2.10; Java bytecode target 17.

**Primary Dependencies**: Existing Android Gradle Plugin 9.2.1, Jetpack Compose
Material 3, AndroidX Lifecycle/ViewModel 2.9.0, Kotlin coroutines 1.10.2, Room 2.7.1,
AndroidX WorkManager (add the runtime dependency for persisted inexact reminders), and
Android platform date/time, photo picker, and notification APIs.

**Storage**: Existing app-private Room SQLite database `nestvest.db`, schema version 1.
Add expense and reminder-preference storage with a non-destructive version 2
auto-migration and export the resulting schema. Do not add internet, analytics, public
services, or a separate storage stack.

**Testing**: JUnit unit tests for amount and period rules, repository/ViewModel state
tests with coroutine test dispatchers, file-backed Room persistence/migration tests,
Compose UI and accessibility tests, and manual emulator checks for notification
permission and device locale. Run existing debug build, lint, JVM, and connected Android
tests.

**Target Platform**: Android API 26 and later; compile/target API 36.

**Project Type**: Native Android mobile app; one `:app` module.

**Performance Goals**: Preserve the spec's usability targets: at least 90% first-attempt
amount-only saves and a median capture-to-save time no greater than 10 seconds. Local
save and history refresh remain in the interactive flow with no network wait.

**Constraints**: Offline-first and app-private; expense data and receipts do not leave
the device. Amount-only save remains the fastest primary action; immediate enrichment
is a secondary option. Dates, amounts, week boundaries, and strings are locale-ready.
Accessibility, focused controls, truthful save/delete outcomes, and confirmed-detail
retention are required. Reminders are off by default and never required for capture.

**Scale/Scope**: One local primary profile and its manually captured expenses. History
supports the current day, week, and month. This feature does not add income, budgets,
reports, imports, recurring transactions, family membership, synchronization, or
app-managed backup. Each expense records the creating primary profile's stable ID as
provenance so a later family-sync feature can preserve attribution.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Local-first privacy and secure storage**: Pass. Expenses, optional details, and
  receipt references remain in app-private storage; no network permission or external
  service is needed. Database migration preserves existing profile data.
- **Zero-friction, user-controlled capture**: Pass. A valid amount and explicit save
  suffice. Vendor, category, and receipt remain optional. Each detail commits on user
  confirmation; leaving an unfinished vendor edit does not commit it or erase other
  confirmed details.
- **Conflict-resilient identity**: Pass. Expense records use stable UUID primary keys,
  not auto-incrementing identities, and retain the creating primary profile UUID.
  Synchronization and conflict resolution are not implemented in this phase. Phase 6
  must preserve the creator ID when importing records and resolve its household member.
- **Kakeibo-led financial wellbeing**: Pass. Enrichment uses Needs, Wants, Culture, and
  Unexpected. Totals and labels are derived only from recorded expenses.
- **Calm, honest, accessible, localized experience**: Pass. The four screens follow the
  provided visual study and theme; keyboard, date/currency rendering, semantics,
  contrast, empty/error states, and non-color cues are explicit validation targets.
- **Local non-intrusive reminders**: Pass. Reminders are user-enabled, local, and
  cancelable; notification denial leaves expense capture available.
- **Quality gates**: Pass, pending tests specified in the design artifacts below.
- **Scope conflict**: None identified. The HTML mockup is a behavior and visual
  reference, not a persistence or platform-security implementation.

## Cross-Feature Dependencies

The [product requirements](../../prd.md) establish the Phase 1 Primary User, later
Family Circle identity, and Phase 6 authenticated peer-to-peer sync. Phase 2 captures
`createdByProfileId` from the existing primary profile UUID; it does not add a member
directory, family UI, or sync protocol. Phase 6 must preserve that ID in transfers,
resolve it against trusted household membership, and show the originating member
without substituting the receiving device's primary user.

Phase 3's Kakeibo planning and reflection flows depend on Phase 2's durable expense
amounts, currencies, dates, and category values. Phase 3 may present its preferred
category vocabulary but must map it consistently to `Needs`, `Wants`, `Culture`, and
`Unexpected`.

Phase 4's on-device SMS/statement ingestion may add expenses or suggest parsed details.
It must extend this model with explicit ingestion source and parsed/confirmed provenance;
`createdByProfileId` identifies the profile that recorded or accepted the expense and
must not be used to claim that an automatically parsed transaction was manually entered
or user-confirmed.

The PRD's P2P-optimized database rule calls for functional unique keys (with examples
such as device-plus-time keys or deterministic transaction hashes), while the
constitution requires stable device-independent record identities and Phase 2 uses
random UUIDs. Phase 6 must explicitly reconcile that rule with intentional repeated
transactions: content equality must not collapse two distinct expenses that the user
explicitly saved. Creator attribution does not itself define duplicate detection or
conflict resolution.

## UI Design Reference

[ui-mockups.html](./ui-mockups.html) is the interaction reference, not production code.
Preserve its four screens, order of emphasis, restrained palette, local-data cue, and
navigation semantics. The specification takes precedence if illustrative copy or
in-memory demo behavior differs.

| Mockup screen / interaction | Native implementation intent | Acceptance focus |
|---|---|---|
| 01 / Capture in a moment | Compose screen with large currency-formatted amount, accessible numeric keypad, primary **Save expense**, and subtle **Add details now** action | Amount-only save; invalid/failed save keeps truthful state; immediate-details action saves before opening enrichment |
| 02 / Find one to fill in later | History screen; Today/This week/This month selectors; currency totals; expense cards; reminder settings icon in app bar | Today is default; week/month rows include local calendar day and time; empty state preserves capture access; settings icon opens reminders |
| 03 / A clear card, as you add details | Expense detail screen with persistent amount/time/detail summary, optional vendor, Kakeibo categories, receipt selection, amount correction, confirmed deletion | Detail confirmation is independent; vendor commits when focus leaves after normalization; category/receipt confirm immediately; user can stop without losing confirmed details |
| 04 / A nudge, only if you ask | Reminder preferences screen opened from history settings icon, off by default, daily time and accessible switch | Permission denial and disable stop reminders; capture remains usable; no notification is required to test capture |

The HTML prototype stores sample data in memory and simulates receipt attachment and
reminder activation. It does not demonstrate actual Android persistence, secure storage,
photo selection, notification scheduling, or accessibility compliance.

## Project Structure

### Documentation (this feature)

```text
specs/002-daily-expense-capture/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── expense-ui.md
├── spec.md
├── tasks.md
└── ui-mockups.html
```

### Source Code (repository root)

```text
app/src/main/java/com/nestvest/
├── MainActivity.kt
├── NestVestApplication.kt
└── expense/
    ├── data/
    │   ├── ExpenseDao.kt
    │   ├── ExpenseEntity.kt
    │   ├── ExpenseRepository.kt
    │   ├── ReminderPreferenceDao.kt
    │   ├── ReminderPreferenceEntity.kt
    │   └── ...
    ├── domain/
    │   ├── Expense.kt
    │   ├── ExpenseAmount.kt
    │   ├── ExpensePeriod.kt
    │   └── ...
    └── ui/
        ├── ExpenseRoute.kt
        ├── ExpenseViewModel.kt
        ├── ExpenseUiState.kt
        └── ...

app/src/main/java/com/nestvest/profile/data/
├── ProfileDatabase.kt
└── additive auto-migration to schema version 2

app/src/main/res/values/
└── strings.xml

app/src/test/java/com/nestvest/expense/
└── amount, date-period, and ViewModel tests

app/src/androidTest/java/com/nestvest/expense/
└── Compose flow, Room migration/persistence, accessibility, and notification tests
```

**Structure Decision**: Extend the current single Android app module. Keep expense
domain, local persistence, and Compose UI together in a new `expense` feature package.
Reuse `ProfileDatabase` as the shared database and migrate its version from 1 to 2;
preserve the existing profile DAO/repository contract. Add no second app, backend, or
external interface.

**Implementation refinements**: Shared domain/state declarations may live together
when they are small (`Expense.kt` includes category; detail state lives with its
ViewModel). Localized feature strings use dedicated `expense_strings.xml` and
`reminder_strings.xml` resource files rather than expanding the profile strings file.
The temporary read-only MVP review is replaced directly by the detail screen in the
complete feature. Supported OS local device transfer includes receipt files alongside
the database; cloud backup stays excluded.

**UI feedback revision (2026-10-23)**: Match the HTML mockup's explicit palette, flat
bordered cards, typography hierarchy, icon-led compact history rows, and rectangular
controls through shared Compose styling. Primary actions sit in fixed bottom footers;
capture has no embedded history and opens Today history via its app-bar Expenses action.
The detail note card remains pinned, with saved/absent detail pills and a scrolling
optional form. Short viewports and large text use a compact header and scrollable pills.
No persistence, permission, identity, or save-confirmation semantics change.

Reminder settings also uses the shared scaffold/tokens: mockup screen 04's bordered
card, off-state guidance, full-width local-time control, and fixed bottom return action.
The native time dialog remains accessible and follows the device's 12/24-hour setting;
large-text content can scroll without moving the return footer.

**Confirmed edge-case policies (2026-10-23)**: Reminder dates at or before the last
claimed date are suppressed after calendar rollback. When the database commits but
receipt cleanup fails, keep the committed change, report cleanup pending, and reconcile
unreferenced private files on a later operation or restart.

## Complexity Tracking

No constitution violations or additional architectural projects require justification.

## Post-Design Constitution Review

- Expense identity and amount precision are represented explicitly in
  [data-model.md](./data-model.md); the migration retains the existing profile and each
  expense records its immutable creating-profile identity.
- UI transitions and copy align with the four screens in
  [ui-mockups.html](./ui-mockups.html) and are specified as native behaviors in
  [contracts/expense-ui.md](./contracts/expense-ui.md).
- [quickstart.md](./quickstart.md) requires offline, accessibility, locale, migration,
  creator-attribution, confirmed-detail, and optional-reminder checks.
- No public-network, cloud backup, analytics, or required-permission dependency is added.
- Phase 3 category consumption, Phase 4 parsed-data provenance, and Phase 6 trusted
  creator-identity resolution are documented as future dependencies, not Phase 2 scope.
