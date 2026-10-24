---

description: "Implementation tasks for Phase 3 Kakeibo planning and reflection"
---

# Tasks: Phase 3 — The Kakeibo Engine (Planning & Reflection)

**Input**: Design documents from `/specs/004-kakeibo-planning-reflection/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/kakeibo-ui.md](./contracts/kakeibo-ui.md), [quickstart.md](./quickstart.md)

**UI reference**: [ui-mockups.html](./ui-mockups.html) is the visual and interaction reference. Its sample data and in-memory state are not production behavior.

**Tests**: Test tasks are included because the spec defines independent acceptance tests and offline/persistence success criteria, and the constitution requires local-data, accessibility, and data-integrity verification.

**Organization**: Tasks are grouped by user story. Story phases are ordered by spec priority: P1 stories first (US1, US2, US4), then P2 stories (US3, US5).

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel because the task changes distinct files and has no dependency on incomplete tasks.
- **[Story]**: User story label (`US1` through `US5`) matching the feature specification.
- Every task description names the file(s) to change.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish shared feature foundations in the existing Android app.

- [x] T001 Add Kakeibo-internal screen/back-stack state in `app/src/main/java/com/nestvest/kakeibo/ui/KakeiboNavigation.kt` for Kakeibo home, learning, monthly plan, weekly pacing, review, progress, category detail, and reflection; leave Dashboard destination and `MainActivity` routing to T042.
- [x] T002 Add canonical category identifiers and local-month/week-period helpers in `app/src/main/java/com/nestvest/kakeibo/domain/KakeiboCategory.kt` and `app/src/main/java/com/nestvest/kakeibo/domain/KakeiboPeriod.kt`; use `Needs`, `Wants`, `Culture`, and `Unexpected`, `YearMonth`, device-local month bounds, and locale-based `WeekFields` week boundaries clipped to the month.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish local persistence, typed operations, migration, and stable feature entry points required by all user stories.

- [x] T003 Create Room entities for monthly plans, category plans, weekly allocations, period reflections, and the singleton learning preference in `app/src/main/java/com/nestvest/kakeibo/data/KakeiboEntities.kt`; preserve the data-model constraints verbatim: `yearMonth` is unique and canonical `YYYY-MM`, `availableMinorUnits` is nullable and non-negative, `savingsIntentionMinorUnits` and `plannedMinorUnits` are non-negative, the four canonical category codes are used, `weekStartDate`/`weekEndDateInclusive` are the inclusive actual displayed dates, reflection text fields may be empty if the user chooses, `learningPromptState` is `UNSEEN`, `VIEWED`, or `DISMISSED`, and creation/update epoch fields are persisted. Use UUID plan/reflection identifiers, `(planId, categoryCode)` category keys, `(planId, categoryCode, weekStartDate)` weekly keys, and cascade child rows with their plan.
- [x] T004 Define Room queries and typed DAO operations in `app/src/main/java/com/nestvest/kakeibo/data/KakeiboDao.kt`, including month lookup/observation, atomic plan/category/weekly replacement, reflection read/write, learning preference read/write, and eligibility-guarded expense category assignment that only changes an existing uncategorized same-month/same-currency expense.
- [x] T005 Add a typed Kakeibo repository contract and Room implementation in `app/src/main/java/com/nestvest/kakeibo/domain/KakeiboRepository.kt` and `app/src/main/java/com/nestvest/kakeibo/data/RoomKakeiboRepository.kt`; expose explicit read/write outcomes, perform multi-row plan writes transactionally, preserve previous saved data on failure, and rethrow coroutine cancellation.
- [x] T006 Add SQLCipher for Android 4.19.1 in `gradle/libs.versions.toml` and `app/build.gradle.kts`; implement random database key generation/wrapping with Android Keystore in `app/src/main/java/com/nestvest/profile/data/DatabaseKeyStore.kt`, data-preserving plaintext-to-encrypted conversion in `app/src/main/java/com/nestvest/profile/data/EncryptedDatabaseMigration.kt`, configure `ProfileDatabase.kt` to use SQLCipher's Room 2 `SupportOpenHelperFactory`, register the Kakeibo DAO, provide it from `NestVestApplication.kt`, and export `app/schemas/com.nestvest.profile.data.ProfileDatabase/4.json`. Do not silently fall back to plaintext or discard the source database if key access or conversion fails.
- [x] T007 Add database encryption/conversion and migration instrumentation coverage in `app/src/androidTest/java/com/nestvest/kakeibo/KakeiboMigrationTest.kt`; migrate a plaintext schema-v3 database, verify existing profile/expense/reminder/dashboard rows survive, verify the result cannot be opened as plaintext SQLite, verify a failed conversion leaves the source recoverable without plaintext fallback, and verify Kakeibo rows persist after encrypted database reopen.

**Checkpoint**: Feature navigation and local persistence foundations are ready; story work can proceed subject to the dependencies below.

---

## Phase 3: User Story 1 — Learn Kakeibo in a Visual Walk-Through (Priority: P1)

**Goal**: Offer an optional, visual Kakeibo introduction once, while keeping planning reachable and learning subtly available afterward.

**Independent Test**: Open the first-use prompt, navigate backward/forward through the slides, skip or finish to planning, dismiss the prompt, leave and return, and verify it does not recur while the Learn action remains available.

### Tests for User Story 1

- [x] T008 [P] [US1] Add learning preference state-transition and persistence tests in `app/src/test/java/com/nestvest/kakeibo/KakeiboViewModelTest.kt`, covering `UNSEEN` to `VIEWED`/`DISMISSED` and no prompt repetition.
- [x] T009 [P] [US1] Add learning prompt, three-slide navigation, skip, and Plan this month Compose tests in `app/src/androidTest/java/com/nestvest/kakeibo/KakeiboLearningUiTest.kt`, including one understandable canonical-category example per category, using semantic labels and test tags.

### Implementation for User Story 1

- [x] T010 [US1] Implement learning preference transitions and repository integration in `app/src/main/java/com/nestvest/kakeibo/ui/KakeiboViewModel.kt`; opening the introduction records `VIEWED`, choosing Not now records `DISMISSED`, and either state suppresses the first-use prompt without disabling Learn.
- [x] T011 [US1] Implement exactly the three optional visual slides and learning prompt in `app/src/main/java/com/nestvest/kakeibo/ui/KakeiboRoute.kt`, following the `intro-screen` mockup for Kakeibo benefits, how to use NestVest, previous/next, skip, and direct plan navigation; include at least one understandable example for each canonical category: Needs, Wants, Culture, and Unexpected.
- [x] T012 [US1] Add the Kakeibo home prompt/Learn action and learning-screen integration in `app/src/main/java/com/nestvest/kakeibo/ui/KakeiboRoute.kt`; keep plan creation reachable when learning is skipped or dismissed.
- [x] T013 [US1] Add localized learning strings in `app/src/main/res/values/strings.xml`; no translated `values-*` directories exist. Expose slide position, navigation, prompt state, and completion actions to assistive technology.

**Checkpoint**: First-use learning can be viewed, dismissed, skipped, revisited, and tested without preventing access to monthly planning.

---

## Phase 4: User Story 2 — Plan for a Month (Priority: P1) 🎯 First usable MVP with US1

**Goal**: Create or revise a calendar-month plan with savings intention and four category amounts, with optional total available, exact arithmetic, review, and durable local save.

**Independent Test**: Create and retrieve monthly plans both with and without an overall amount; verify synchronized slider/text values, category and savings totals, optional remainder, over-allocation rejection, review-before-replacement, offline persistence, and failure recovery.

### Tests for User Story 2

- [x] T014 [P] [US2] Add exact amount parsing, allocation, available-balance, and validation tests in `app/src/test/java/com/nestvest/kakeibo/KakeiboPlanTest.kt` and `KakeiboMoneyAmountParserTest.kt`; cover non-negative minor units, currency precision, overflow, optional available amount, no invented remainder, and the data-model rule “Reject malformed/excess precision rather than rounding silently.”
- [x] T015 [P] [US2] Add plan save/update/retry and atomic rollback instrumentation tests in `app/src/androidTest/java/com/nestvest/kakeibo/KakeiboPlanRepositoryTest.kt`, verifying a failed replacement preserves previous plan/category rows; ViewModel unit coverage verifies draft retention.
- [x] T016 [P] [US2] Add planning/review Compose tests in `app/src/androidTest/java/com/nestvest/kakeibo/KakeiboPlanUiTest.kt`, covering slider/text synchronization, optional overall amount, review and cancel; offline retrieval is covered by the migration/reopen test.

### Implementation for User Story 2

- [x] T017 [US2] Implement monthly plan domain values and exact allocation calculations in `app/src/main/java/com/nestvest/kakeibo/domain/KakeiboPlan.kt` and `app/src/main/java/com/nestvest/kakeibo/domain/KakeiboPlanCalculator.kt`; enforce that amounts are non-negative minor units, an omitted available amount produces no remainder, and a provided amount is not exceeded by savings plus all four categories.
- [x] T018 [US2] Implement monthly plan create/read/replace operations in `app/src/main/java/com/nestvest/kakeibo/data/RoomKakeiboRepository.kt`; save the plan and its four category rows atomically and retain the prior plan on failure.
- [x] T019 [US2] Implement monthly plan draft, review, confirmation, and retry state in `app/src/main/java/com/nestvest/kakeibo/ui/KakeiboViewModel.kt`, using the primary profile currency and selected calendar month without changing saved expenses.
- [x] T020 [US2] Implement monthly planning and review screens in `app/src/main/java/com/nestvest/kakeibo/ui/KakeiboRoute.kt`, matching the `planner-screen` and `review-screen` mockup flows with synchronized slider/exact fields and optional available amount.
- [x] T021 [US2] Add localized plan, currency, validation, review, and save-failure strings in `app/src/main/res/values/strings.xml`; no translated `values-*` directories exist. Make amounts, slider labels, validation errors, and save feedback accessible.

**Checkpoint**: A valid monthly plan can be created, retrieved offline, and confirmed before replacing an existing month; invalid and failed saves never report success.

---

## Phase 5: User Story 4 — See Category Progress and Reflect (Priority: P1)

**Goal**: Show truthful category progress and daily spending, explicitly assign eligible untagged expenses, and save a monthly reflection without changing expense records.

**Independent Test**: With a saved plan and expenses, inspect each category's plan/spend/remaining/health/chart, verify unassigned and other-currency separation, confirm or cancel assignment, save/reopen reflection, and verify current totals refresh independently of saved reflection text.

### Tests for User Story 4

- [x] T022 [P] [US4] Add progress, remaining-balance, health, and full-month daily aggregation tests in `app/src/test/java/com/nestvest/kakeibo/KakeiboPeriodAndProgressTest.kt`; cover `No allocation`, `Within plan`, `At limit`, `Over plan`, negative remaining, zero-spend days, and exact overflow-safe totals.
- [x] T023 [P] [US4] Add guarded expense-assignment repository tests in `app/src/androidTest/java/com/nestvest/kakeibo/KakeiboExpenseAssignmentTest.kt`; verify only eligible uncategorized same-month/same-currency expenses change category and stale, canceled, failed, missing, or ineligible assignments do not alter records.
- [x] T024 [P] [US4] Add reflection save/reopen/failure tests in `app/src/test/java/com/nestvest/kakeibo/KakeiboViewModelTest.kt`, verifying draft retention on failure and saved answers remain unchanged when expenses later change.
- [x] T025 [P] [US4] Add progress/category/reflection Compose tests in `app/src/androidTest/java/com/nestvest/kakeibo/KakeiboProgressReflectionUiTest.kt`, including no-plan state, reflection input/save, and accessible chart data through the dedicated chart semantics test.

### Implementation for User Story 4

- [x] T026 [US4] Implement exact category progress, health status, and day-by-day monthly aggregation in `app/src/main/java/com/nestvest/kakeibo/domain/KakeiboProgress.kt`; include only same-month/same-currency matching-category expenses, produce all calendar dates, show negative remaining, and keep unassigned/other-currency expenses separate.
- [x] T027 [US4] Implement transactional guarded expense-category assignment in `app/src/main/java/com/nestvest/kakeibo/data/KakeiboDao.kt` and `app/src/main/java/com/nestvest/kakeibo/data/RoomKakeiboRepository.kt`; at write time recheck that the saved expense remains uncategorized, within the selected local month, and in plan currency, changing only its category.
- [x] T028 [US4] Implement progress, category detail, assignment, and reflection state/actions in the shared `app/src/main/java/com/nestvest/kakeibo/ui/KakeiboViewModel.kt`; refresh from saved expense data and retain unsaved responses on write failure.
- [x] T029 [US4] Implement monthly progress and category detail screens in `app/src/main/java/com/nestvest/kakeibo/ui/KakeiboRoute.kt`, following `progress-screen`/`category-detail-screen` with category totals, negative remaining, textual health, full-month daily chart, and separate exception/assignment states.
- [x] T030 [US4] Implement monthly reflection screen and saved-reflection reopen behavior in `app/src/main/java/com/nestvest/kakeibo/ui/KakeiboRoute.kt` and `KakeiboViewModel.kt`, following `reflection-screen`/`saved-reflection-screen` with optional postpone, plan/spend comparison, save/reopen, and retry behavior; each user response may be empty if the user chooses, and later expense edits refresh displayed totals without rewriting the saved answers.
- [x] T031 [US4] Add localized progress, category, chart, assignment, and reflection strings in `app/src/main/res/values/strings.xml`; no translated `values-*` directories exist. Expose chart dates/amounts and health through text/semantics, not color or icon alone.

**Checkpoint**: Progress reflects current local expenses, eligible assignment changes only category, and reflection can be postponed, saved, reopened, and retried without editing expenses.

---

## Phase 6: User Story 3 — Pace the Plan Week by Week (Priority: P2)

**Goal**: Optionally break category limits into locale-calendar weeks without adding funds to the monthly plan.

**Independent Test**: Add weekly values for one or more categories, verify partial-week dates and blank-as-zero, reject category sums above monthly limits, and skip pacing without changing monthly amounts.

### Tests for User Story 3

- [x] T032 [P] [US3] Add locale-week slicing and weekly total validation tests in `KakeiboPeriodAndProgressTest.kt`, `KakeiboMoneyAmountParserTest.kt`, and `KakeiboViewModelTest.kt`, covering partial first/last weeks, alternate first-day locales, blank-as-zero, and over-limit rejection.
- [x] T033 [P] [US3] Add weekly pacing Compose tests in `app/src/androidTest/java/com/nestvest/kakeibo/KakeiboWeeklyPlanUiTest.kt`, covering week-date labels and retained accessible amount input.

### Implementation for User Story 3

- [x] T034 [US3] Implement weekly allocation generation and per-category total validation in `KakeiboPeriod.kt` and `KakeiboViewModel.kt`; use locale-based `WeekFields`, clip actual week dates to the selected month, treat blank values as zero, and reject any weekly sum above its monthly category limit.
- [x] T035 [US3] Implement atomic weekly allocation save/read/clear operations in `app/src/main/java/com/nestvest/kakeibo/data/RoomKakeiboRepository.kt`; weekly amounts remain a breakdown and do not modify monthly allocations.
- [x] T036 [US3] Implement optional weekly pacing state and screen in the shared `KakeiboViewModel.kt` and `KakeiboRoute.kt`, following `weekly-screen` with actual calendar date labels, zero for blank fields, skip, error retention, and accessible feedback.
- [x] T037 [US3] Add localized weekly pacing, period-date, and validation strings in `app/src/main/res/values/strings.xml`; no translated `values-*` resource directories exist.

**Checkpoint**: Weekly pacing is optional, correctly labeled by actual locale-calendar dates, and cannot exceed or add to monthly category limits.

---

## Phase 7: User Story 5 — Find the Monthly Plan from the Dashboard (Priority: P2)

**Goal**: Show an always-accessible dashboard Kakeibo tile with a no-plan nudge or current-month categorized spend/plan summary and honest health.

**Independent Test**: View the dashboard with no plan, with current same-currency saved expenses, and with plan/expense reads unavailable; verify the prompt, summary and most-constrained health, stale/zero distinction, and working destination.

### Tests for User Story 5

- [x] T038 [P] [US5] Add Kakeibo dashboard summary calculation and ViewModel tests in `app/src/test/java/com/nestvest/dashboard/KakeiboDashboardSummaryTest.kt` and `KakeiboDashboardViewModelTest.kt`, covering no plan, current total, unavailable reads, meaningful zero, and precedence `Over plan` before `At limit` before `Within plan`, with `No allocation` only when nothing is allocated or spent.
- [x] T039 [P] [US5] Add dashboard tile and navigation Compose coverage in `app/src/androidTest/java/com/nestvest/dashboard/KakeiboDashboardTileTest.kt`, verifying no-plan prompt and tile availability/navigation regardless of pinned shortcut preferences; unavailable summary without stale values is covered by the ViewModel test.

### Implementation for User Story 5

- [x] T040 [US5] Implement transient current-month dashboard summary states and calculation in `app/src/main/java/com/nestvest/kakeibo/domain/KakeiboProgress.kt` and `app/src/main/java/com/nestvest/dashboard/ui/DashboardViewModel.kt`; use current local plan/expense reads, same-currency categorized totals, most-constrained category health, and explicit loading/no-plan/unavailable states without stale fallback.
- [x] T041 [US5] Add a dedicated, non-customizable Kakeibo tile to `app/src/main/java/com/nestvest/dashboard/ui/DashboardScreen.kt`, with the no-plan nudge or current summary, textual health, accessible unavailable/loading state, and navigation remaining enabled for every summary state.
- [x] T042 [US5] Add Kakeibo destination routing in `app/src/main/java/com/nestvest/dashboard/domain/DashboardOption.kt` and `app/src/main/java/com/nestvest/MainActivity.kt`; route the fixed Kakeibo tile into `KakeiboRoute` without adding it to the user's pin/unpin catalog or changing amount-first capture/reminder deep links.
- [x] T043 [US5] Add localized Kakeibo dashboard tile, prompt, summary, and unavailable strings in `app/src/main/res/values/strings.xml`; no translated `values-*` resource directories exist.

**Checkpoint**: The dashboard always provides a working Kakeibo destination and accurately switches from a current-month planning prompt to a fresh summary after the plan is saved.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validate whole-feature integration and constitution requirements across all story screens.

- [x] T044 [P] Add accessibility and localization instrumentation coverage in `KakeiboAccessibilityTest.kt` and `KakeiboLocalizationTest.kt`; includes day-by-day chart semantics, localized date/currency output, large text, compact-screen scrolling, and a minimum touch-target assertion.
- [ ] T045 Run the end-to-end scenarios from `specs/004-kakeibo-planning-reflection/quickstart.md`, fix integration failures in the referenced Kakeibo/dashboard files, and record verified commands/results in `specs/004-kakeibo-planning-reflection/quickstart.md`.
- [ ] T046 Run and verify schema-4 export, SQLCipher encryption, Android Keystore key protection, plaintext migration recovery, and privacy boundaries in `app/schemas/com.nestvest.profile.data.ProfileDatabase/4.json`, `KakeiboMigrationTest.kt`, and `KakeiboPrivacyTest.kt`; instrumentation sources cover existing-record preservation, plaintext rejection, failed-conversion preservation, and wrapped-key storage but still require device execution.

---

## Dependencies & Execution Order

### UI feedback follow-up

Execute T047, then T048, then T049; these follow the completed story implementations.

- [x] T047 Add regression coverage for calculated overall amounts, explicit slider caps, currency-formatted review cards, dashboard tile placement, and Kakeibo home navigation in the existing Kakeibo ViewModel/Compose and dashboard tile tests.
- [x] T048 Align shared Kakeibo frames, buttons, cards, sliders, planning summaries, review, and dashboard tile presentation with the mockup in `KakeiboRoute.kt`, `KakeiboViewModel.kt`, `ExpenseDesign.kt`, `DashboardScreen.kt`, and localized strings. Keep omitted overall amounts nullable in storage.
- [x] T049 Update the UI contract and validation guide for the confirmed calculated-total behavior; run unit, connected UI, lint, and build validation.

### Complete feature mockup alignment

Execute T050, then T051, then T052. Preserve the calculated-total decisions above.

- [x] T050 Add regression coverage in Kakeibo Compose/ViewModel tests for inline learning, home entry tiles, progress/exception summaries, optional weekly save/skip, category selection/confirmation, and reflection save/view/edit/postpone.
- [x] T051 Align every Kakeibo screen in `KakeiboRoute.kt`, shared navigation/ViewModel state, and localized strings with the mockup; preserve live local financial data, explicit error states, and existing dashboard routing.
- [x] T052 Validate focused and full UI/unit/lint/build suites, inspect rendered screens, and record final behavior/evidence in the UI contract and quickstart. Final combined run: 89 instrumentation tests, zero failures/skips; unit tests, lint, and debug assembly passed. See quickstart for the bounded manual checks and remaining broader acceptance work.

### Multi-select unassigned expenses

Execute T053, then T054, then T055.

- [x] T053 Add multi-selection/deselection/cancel/retry UI coverage in `KakeiboFeatureUiTest.kt` and atomic batch eligibility/write-failure coverage in `KakeiboExpenseAssignmentTest.kt`; cover stale selection and in-flight duplicate confirmation in UI/ViewModel tests.
- [x] T054 Implement checkbox selection and one confirmed atomic batch assignment in `KakeiboRoute.kt`, `KakeiboViewModel.kt`, `KakeiboRepository.kt`, and `RoomKakeiboRepository.kt`; preserve existing eligibility rules and explicit errors.
- [x] T055 Update the feature specification/UI contract/quickstart, then validate affected instrumentation, unit tests, lint, and debug build. Passed 11 focused emulator tests with zero failures/skips, the unit suite, lint, and debug assembly.

### Phase dependencies

- **Setup (Phase 1)**: No prerequisites.
- **Foundational (Phase 2)**: Depends on Setup. Complete the Kakeibo domain primitives, Room data access, schema migration, and migration tests before story implementations.
- **US1 (Phase 3)**: Depends on the shared navigation and encrypted-persistence foundation; its Plan this month action routes into US2. US1 can be tested as a learning flow independently, but the first usable MVP release combines US1 and US2.
- **US2 (Phase 4)**: Depends on the shared persistence foundation; provides saved monthly plans required by US3, US4, and US5.
- **US4 (Phase 5)**: Depends on US2 plans and the existing Phase 2 expense store; its progress/assignment supports the dashboard summary in US5.
- **US3 (Phase 6)**: Depends on US2 monthly category limits; otherwise independent of US1 and US4.
- **US5 (Phase 7)**: Depends on US2 plan persistence and US4 progress/summary rules, plus the existing dashboard navigation feature. It is independent of optional US3 weekly pacing.
- **Polish (Phase 8)**: Depends on all selected story phases.

### User story completion dependency graph

```text
Setup → Foundation ─┬─→ US1
                    ├─→ US2 ──┬─→ US4 ──┬─→ US5
                    │         │         └────┘
                    │         └─→ US3
                    └─────────────────────────
```

US1 and US2 can proceed independently after Foundation. US3 and US4 can proceed in parallel after US2. US5 follows US4 and US2; US3 is not a prerequisite.

### Parallel execution examples

**After Foundation**:

- US1 can implement the learning preference/view flow while US2 implements monthly plan logic/UI; they touch separate ViewModels/screens and story tests.
- Within US2, plan-domain tests (`T014`), repository tests (`T015`), and Compose tests (`T016`) target separate files and can be prepared in parallel before their corresponding implementation tasks.
- Within US4, progress calculations (`T026`), guarded assignment (`T027`), reflection state (`T028`), and screen work (`T029`/`T030`) can be split by their named files once US2's plan/repository contracts are stable. Test tasks (`T022`–`T025`) use separate test files.
- US3 weekly logic/UI can proceed alongside US4 after US2 is complete; they use separate weekly files and test files.
- US5 dashboard summary tests and tile/navigation work use dashboard/MainActivity files, separate from Kakeibo screen files. Start US5 only after US2 and US4 behavior contracts are stable.
- Polish accessibility/localization checks (`T044`) can be prepared while story implementation is underway, but final execution (`T045`–`T046`) follows completed stories.

## Implementation Strategy

1. Complete shared navigation and encrypted local persistence foundations; prove plaintext-to-SQLCipher conversion and the schema-3-to-4 migration preserve existing data.
2. Deliver **US1 + US2 together** as the first usable MVP: a persisted, optional, exactly three-slide Kakeibo introduction with examples for all four categories, plus monthly plan creation and retrieval. US1 remains independently testable as an optional learning flow.
3. Deliver **US4** for accurate current progress and reflection; this adds the main feedback loop and explicit expense assignment.
4. Add **US3** optional weekly pacing and **US5** dashboard summary as incremental extensions; neither changes the core expense-capture flow.
5. Finish with offline, accessibility, localization, privacy, migration, and full quickstart verification.

## Independent Acceptance Tests by Story

- **US1**: First-use prompt can be viewed/dismissed once; the three slides include understandable examples for all four canonical categories and support back/forward/skip/direct planning; subsequent home visits show Learn without repeating the prompt.
- **US2**: Plans save and reload offline with/without overall available amount; synchronized controls and exact totals are correct; over-allocation and failed writes preserve the old plan and draft.
- **US4**: Current expenses produce the correct plan/spent/remaining/health/chart; unassigned and other-currency records stay separate; guarded assignment and saved reflection behave correctly through cancel/failure/reopen.
- **US3**: Locale-week date segments are correct, blank values equal zero, skipped pacing preserves the monthly plan, and weekly category totals cannot exceed monthly limits.
- **US5**: Dashboard shows the no-plan prompt, replaces it with a fresh most-constrained current summary, distinguishes unavailable from zero, and never disables Kakeibo navigation.
