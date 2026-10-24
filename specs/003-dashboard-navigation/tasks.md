---

description: "Implementation tasks for Phase 3 dashboard navigation"

---

# Tasks: Phase 3 — Dashboard Navigation

**Input**: Design documents from `/specs/003-dashboard-navigation/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`,
`contracts/dashboard-ui.md`, and `quickstart.md`

**Tests**: Test tasks are included to satisfy the feature acceptance scenarios and the
constitution's quality gates for local-only behavior, persistence, accessibility, and
localization.

**Organization**: Tasks are grouped by user story. Shared Room migration work is
foundational because the dashboard and customization need the saved preference.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other tasks in the same phase because it changes
  different files and does not depend on their incomplete changes.
- **[Story]**: User story from `spec.md` (`US1`, `US2`, or `US3`).
- Every task includes the exact source, test, schema, or validation-guide path it changes.

## Path Conventions

This is a single-module Android application. Production Kotlin sources are under
`app/src/main/java/com/nestvest/`; unit tests are under
`app/src/test/java/com/nestvest/`; instrumentation tests are under
`app/src/androidTest/java/com/nestvest/`.

## Phase 1: Setup

**Purpose**: Project initialization.

The Android application, Compose, Room, test runners, and feature directory already
exist. No project initialization or dependency changes are required for this feature.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Add and verify the local preference persistence required by dashboard
entry and customization. Complete this phase before implementing user stories.

- [X] T001 Add the singleton `DashboardPreferenceEntity` and DAO in `app/src/main/java/com/nestvest/dashboard/data/DashboardPreferenceEntity.kt` and `app/src/main/java/com/nestvest/dashboard/data/DashboardPreferenceDao.kt`; `pinnedOptionIds` MUST contain only catalog IDs, default to `expense_capture` and `expense_history`, and MUST have no arbitrary pin-count limit.
- [X] T002 Register the dashboard preference entity and DAO in `app/src/main/java/com/nestvest/profile/data/ProfileDatabase.kt` and add the additive version 2-to-3 migration; existing profile, expense, and reminder data MUST remain unchanged and destructive migration fallback MUST NOT be added.
- [ ] T003 Verify the version 2-to-3 migration preserves existing profile, expense, and reminder rows in `app/src/androidTest/java/com/nestvest/dashboard/DashboardPreferenceMigrationTest.kt`; update the exported schema at `app/schemas/com.nestvest.profile.data.ProfileDatabase/3.json`.

**Checkpoint**: Dashboard preference storage is local, migrated, and verified.

---

## Phase 3: User Story 1 — Navigate to Available Features (Priority: P1) 🎯 MVP

**Goal**: Show the dashboard after profile setup and provide routes to all three
currently available destinations without interrupting existing expense/reminder flows.

**Independent Test**: Complete profile setup, verify the dashboard displays Add expense
and Expenses as pinned and Reminders under More, then open each destination. Verify
offline navigation and the existing reminder-triggered capture entry.

### Tests for User Story 1

- [X] T004 [P] [US1] Add Compose navigation tests for profile gating, pinned and More destinations, direct amount-first capture/history/reminder routes, and reminder-intent capture routing in `app/src/androidTest/java/com/nestvest/dashboard/DashboardNavigationTest.kt`.
- [X] T005 [P] [US1] Add dashboard accessibility and offline navigation checks for meaningful option labels, focus/reading order, and no internet-dependent route in `app/src/androidTest/java/com/nestvest/dashboard/DashboardAccessibilityTest.kt`.

### Implementation for User Story 1

- [X] T006 [P] [US1] Define the stable available-option catalog and default IDs (`expense_capture`, `expense_history`, `reminders`) in `app/src/main/java/com/nestvest/dashboard/domain/DashboardOption.kt`; do not include future-phase destinations that are not available in this app version.
- [X] T007 [US1] Implement the dashboard screen with the NestVest/local-data app bar, Pinned for you group, More group, empty secondary state, unavailable-option state, and Customize entry in `app/src/main/java/com/nestvest/dashboard/ui/DashboardScreen.kt`, following `specs/003-dashboard-navigation/contracts/dashboard-ui.md`.
- [X] T008 [US1] Implement dashboard state and navigation to the existing expense and reminder destinations in `app/src/main/java/com/nestvest/dashboard/ui/DashboardViewModel.kt`; preserve the existing amount-only capture and history behavior.
- [X] T009 [US1] Wire the saved-profile route to the dashboard and preserve the existing `ExpenseReminderNotifier.OPEN_CAPTURE` request in `app/src/main/java/com/nestvest/MainActivity.kt`.
- [X] T010 [US1] Add localized dashboard labels, descriptions, empty/unavailable states, and navigation feedback to `app/src/main/res/values/strings.xml`; use resource-backed strings rather than mockup text in production.

**Checkpoint**: A saved-profile user can navigate to all currently available destinations;
profile setup and reminder-triggered capture still work.

---

## Phase 4: User Story 2 — Pin Frequently Used Options (Priority: P2)

**Goal**: Let users pin/unpin available options, save their choices locally, and retain
all destinations under either the primary area or More.

**Independent Test**: Pin Reminders, unpin Expenses, save, restart the app, and verify
the saved grouping persists and all destinations remain accessible. Simulate a write
failure and verify the former saved grouping remains in effect.

### Tests for User Story 2

- [X] T011 [P] [US2] Add unit tests for default preferences, valid catalog IDs, pin/unpin draft changes, save success, and save failure in `app/src/test/java/com/nestvest/dashboard/DashboardViewModelTest.kt`.
- [X] T012 [P] [US2] Add Compose and Room-backed tests for Pin/Unpin accessible states, Save choices behavior, persistence across database reopen, retryable write-failure feedback, More grouping, and retaining every available option in `app/src/androidTest/java/com/nestvest/dashboard/DashboardCustomizationTest.kt`.

### Implementation for User Story 2

- [X] T013 [US2] Define the preference repository contract and preference value type in `app/src/main/java/com/nestvest/dashboard/domain/DashboardPreference.kt`; normalize unknown stored IDs out of the pinned set while leaving their currently available destinations under More.
- [X] T014 [US2] Implement local preference reads and writes in `app/src/main/java/com/nestvest/dashboard/data/RoomDashboardPreferenceRepository.kt` using the foundational DAO; surface read/write failures explicitly rather than treating them as successful saves.
- [X] T015 [US2] Add editable pin drafts, explicit save/cancel navigation, persistence loading, and failure state handling in `app/src/main/java/com/nestvest/dashboard/ui/DashboardViewModel.kt`.
- [X] T016 [US2] Add the Customize list with Pin/Unpin actions, “Pinned on dashboard”/“Available under More” state, Save choices action, and accessible feedback in `app/src/main/java/com/nestvest/dashboard/ui/DashboardScreen.kt`.
- [X] T017 [US2] Add localized customization labels, pin states, save success, save failure, and retry messages to `app/src/main/res/values/strings.xml`.

**Checkpoint**: Customization survives app restarts; a failed save never replaces the
last successfully saved preference.

---

## Phase 5: User Story 3 — Understand Useful Tile Information (Priority: P3)

**Goal**: Show an accurate, optional current-day expense summary without blocking
navigation or confusing no history with zero activity today.

**Independent Test**: Verify the summary against local expenses for a day with records,
a day with no records but prior history, a user with no expense history, and an expense
read failure. Confirm Expenses remains usable in every state.

### Tests for User Story 3

- [X] T018 [P] [US3] Add unit tests for local-day boundaries, exact minor-unit aggregation, no-history versus zero-today states, and unavailable reads in `app/src/test/java/com/nestvest/dashboard/DashboardSummaryTest.kt`.
- [X] T019 [P] [US3] Add Compose tests for summary labels/values, stale or unavailable summary handling, and opening Expenses from every summary state in `app/src/androidTest/java/com/nestvest/dashboard/DashboardSummaryUiTest.kt`.

### Implementation for User Story 3

- [X] T020 [P] [US3] Implement the summary states and current-local-day count/total calculation in `app/src/main/java/com/nestvest/dashboard/domain/DashboardSummary.kt`; use exact amount minor units and do not combine different currencies.
- [X] T021 [US3] Observe existing local expense data and refresh the summary when the dashboard is shown/resumed and when the local day changes in `app/src/main/java/com/nestvest/dashboard/ui/DashboardViewModel.kt`; on read failure expose Unavailable and never reuse a stale amount.
- [X] T022 [US3] Display the optional current-day count and profile-currency total, no-history state, zero-today state, loading state, and unavailable state on the Expenses tile in `app/src/main/java/com/nestvest/dashboard/ui/DashboardScreen.kt`; keep the tile actionable and status understandable without color alone.
- [X] T023 [US3] Add localized summary, no-history, zero-today, loading, and unavailable labels in `app/src/main/res/values/strings.xml`, using locale-aware date and currency formatting.

**Checkpoint**: Summary states are accurate and optional; summary failures do not block
expense history.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validate the integrated feature against the design contract and
constitution.

- [ ] T024 Verify all Phase 3 manual acceptance scenarios, migration, accessibility, locale, and privacy checks in `specs/003-dashboard-navigation/quickstart.md`; record executed Gradle commands and observed results without claiming unperformed usability testing.
- [X] T025 Align the native dashboard with `specs/003-dashboard-navigation/ui-mockups.html`, greet the saved profile by name, provide a More screen, and make the NestVest brand action return to the dashboard from all post-profile screens; update `app/src/main/java/com/nestvest/dashboard/ui/DashboardScreen.kt`, `app/src/main/java/com/nestvest/expense/ui/ExpenseDesign.kt`, `app/src/main/java/com/nestvest/expense/ui/ExpenseRoute.kt`, `app/src/main/res/values/strings.xml`, `app/src/androidTest/java/com/nestvest/dashboard/DashboardNavigationTest.kt`, `app/src/androidTest/java/com/nestvest/expense/ExpenseNavigationTest.kt`, and the feature contract/quickstart.
- [X] T026 Align dashboard tile details with the mockup: put Customize in the Pinned for you heading row, use relevant leading tile icons, and render a concise localized Expenses summary pill; update `app/src/main/java/com/nestvest/dashboard/ui/DashboardScreen.kt`, `app/src/main/java/com/nestvest/dashboard/domain/DashboardSummary.kt`, `app/src/main/java/com/nestvest/expense/ui/ExpenseDesign.kt`, `app/src/main/res/values/strings.xml`, `app/src/androidTest/java/com/nestvest/dashboard/DashboardNavigationTest.kt`, `app/src/androidTest/java/com/nestvest/dashboard/DashboardSummaryUiTest.kt`, `specs/003-dashboard-navigation/contracts/dashboard-ui.md`, and `specs/003-dashboard-navigation/quickstart.md`.
- [X] T027 Hide the dashboard More section when every available option is pinned and replace persistent save-success text with a short, mild snackbar confirmation; add UI coverage and update the dashboard contract/acceptance guide in `app/src/main/java/com/nestvest/dashboard/ui/DashboardScreen.kt`, `app/src/main/res/values/strings.xml`, `app/src/androidTest/java/com/nestvest/dashboard/DashboardNavigationTest.kt`, `app/src/androidTest/java/com/nestvest/dashboard/DashboardCustomizationTest.kt`, `specs/003-dashboard-navigation/contracts/dashboard-ui.md`, and `specs/003-dashboard-navigation/quickstart.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No work; the existing Android project and dependencies are
  already initialized.
- **Foundational (Phase 2)**: Blocks all stories because the dashboard reads saved
  preferences and customization must persist.
- **US1 (Phase 3)**: Depends on the Room preference schema and provides the dashboard
  shell and destinations.
- **US2 (Phase 4)**: Depends on the foundational preference persistence and the US1
  dashboard route/screen.
- **US3 (Phase 5)**: Depends on the US1 dashboard tile and navigation shell; it may be
  implemented independently of US2 once the shared shell exists.
- **Polish (Phase 6)**: Depends on the completed stories.

### User Story Completion Order

```text
Foundational → US1 (MVP) → US2
                         └→ US3
US2 and US3 are independent of each other after US1.
```

### Parallel Opportunities

- **Foundational example**: Complete T001, then T002; run T003 after both.
- **US1 example**: After the foundation, T004, T005, and T006 can be developed in
  parallel because each changes a separate test or domain file. Integrate T007–T010
  sequentially where the same UI, state holder, and activity become connected.
- **US2 example**: After US1, author T011 and T012 in parallel; implement T013 and T014
  before connecting save/draft behavior through T015 and T016, then add T017 strings.
- **US3 example**: After US1, T018, T019, and T020 can be developed in parallel because
  unit tests, UI tests, and summary-domain logic use separate files. Integrate T021–T023
  sequentially into the shared state holder, screen, and resource file.
- Changes to `DashboardScreen.kt`, `DashboardViewModel.kt`, and `strings.xml` occur in
  multiple story phases and should be integrated sequentially to avoid overlapping
  edits.

## Implementation Strategy

1. **MVP**: Complete the foundational migration and US1. Verify setup gating, dashboard
   launch, all available routes, offline navigation, and reminder-triggered capture.
2. **Customization**: Complete US2 and verify saved pin choices survive restart and
   errors do not produce false success.
3. **Informative tiles**: Complete US3 and verify each summary state against local data.
4. **Release readiness**: Complete the quickstart checks, including accessibility,
   localization, privacy, and schema migration.

Each user story has an independent verification path; US1 is the smallest useful
dashboard MVP, while later stories extend it without changing existing capture
requirements.
