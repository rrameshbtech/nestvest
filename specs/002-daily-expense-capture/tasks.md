# Tasks: Phase 2 - Ultra-Fast Daily Expense Capture

**Input**: [plan.md](./plan.md), [spec.md](./spec.md),
[data-model.md](./data-model.md), [research.md](./research.md),
[expense UI contract](./contracts/expense-ui.md), and [quickstart.md](./quickstart.md).

**Prerequisites**: Existing Phase 1 profile implementation and exported Room version-1
schema. All paths below are relative to the repository root.

**Tests**: Included because the specification defines offline, persistence, invalid-input,
enrichment, deletion, accessibility, and reminder acceptance tests, and the constitution
requires amount-only/offline/integrity validation. Write the story tests before the
corresponding implementation and verify that they expose the missing behavior; finish
each story with its tests passing. Do not count compilation failures as behavioral proof.

**Execution status (2026-10-23)**: 42 of 44 tasks complete, including the four UI
feedback follow-ups. Initial feature validation passed 39 JVM tests and 50 Android
tests on API 37. The UI follow-up passed 39 JVM tests and 11 focused Android UI tests,
plus debug assembly and lint, with native renders inspected against the mockup.
The reminder follow-up passed 3 focused Android tests and 39 JVM tests, build, and lint.
T038 remains open for the unavailable API-26/device-level manual matrix; T039 has a
written protocol but no participant results. See the validation record and explicit
outstanding checks in [quickstart.md](./quickstart.md). Checked test-authoring tasks
record implemented automated coverage, not completion of those remaining manual checks.
Small related declarations/resources share files as documented in [plan.md](./plan.md).

**Organization**: Setup, shared foundation, one phase per prioritized user story, then
cross-cutting validation. `[P]` marks work on separate files that can run concurrently
once its listed prerequisites are complete; it does not waive those prerequisites.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Remove a misleading contract statement before implementation.

- [X] T001 Correct the **Save expense** sentence in `specs/002-daily-expense-capture/contracts/expense-ui.md` so it explicitly stores the creating primary profile ID and clears only the entered amount after a successful write; never clear or mutate the primary profile ID. Keep FR-003/FR-021 and the existing data model authoritative.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish shared record contracts, the additive schema, and dependency wiring.
No story implementation begins until T002-T007 are complete.

- [X] T002 Define the shared expense domain types in `app/src/main/java/com/nestvest/expense/domain/Expense.kt` and `app/src/main/java/com/nestvest/expense/domain/KakeiboCategory.kt`, preserving every data-model field constraint: `expenseId` is required and a "Stable primary key; generated once per new expense; never derived from amount or timestamp"; `amountMinorUnits` is required and "Must be greater than zero; stores exact currency minor units without floating-point rounding"; `currencyCode` is required and "Captured from the primary profile at creation; immutable for ordinary detail edits"; `capturedAtEpochMillis` is required and a "Capture instant in UTC epoch milliseconds; display/group using the device's current local calendar and locale"; `createdByProfileId` is required and the "Stable ID of the profile that created the expense; set from the current primary profile and immutable across edits and future sync"; nullable `vendor` must "Trim whitespace; blank means absent; maximum 80 Unicode code points"; nullable `category` is "`Needs`, `Wants`, `Culture`, or `Unexpected`"; nullable `receiptFileName` is an "App-private local receipt image reference; absent unless a user confirms an image selection and the copy succeeds". Use UUID domain identities and stable category storage values, not enum ordinals.
- [X] T003 Define the initial create/read/period-observation repository contract and typed outcomes in `app/src/main/java/com/nestvest/expense/domain/ExpenseRepository.kt`; distinguish absence, loading, validation failure, and storage failure, retain cancellation propagation, and provide injectable time/UUID sources using existing project conventions. Creation accepts the already-loaded primary profile identity and currency without asking the user for extra fields; do not expose creator reassignment.
- [X] T004 Implement the expense mapping and initial DAO in `app/src/main/java/com/nestvest/expense/data/ExpenseEntity.kt` and `app/src/main/java/com/nestvest/expense/data/ExpenseDao.kt` using T002's exact constraints: UUID text primary key without auto-generation, required amount/currency/capture-time/creator columns, nullable vendor/category/receipt columns, capture-time index, insert, lookup by ID, and observable half-open period query sorted newest first with a stable tie-breaker. Store `createdByProfileId` as provenance without a foreign key to the singleton local profile; invalid persisted values must surface as errors, not silently disappear.
- [X] T005 Define shared reminder preference storage in `app/src/main/java/com/nestvest/expense/domain/ReminderPreference.kt`, `app/src/main/java/com/nestvest/expense/data/ReminderPreferenceEntity.kt`, and `app/src/main/java/com/nestvest/expense/data/ReminderPreferenceDao.kt`; persist a singleton row with required `enabled` that "Defaults to false", required `localTimeMinutes` described as "Local wall-clock minutes after midnight, range 0–1439; initial suggested value 20:30", required `permissionPrompted` that "Defaults to false; prevents automatic/repeated runtime permission prompts", and nullable `lastDeliveredLocalDate` that is "Claimed before notification delivery to prevent a WorkManager retry from posting twice in one local day". Include observable settings, validated updates, and an atomic compare-and-claim operation for a local date; this task does not schedule work or enable reminders.
- [X] T006 Extend `app/src/main/java/com/nestvest/profile/data/ProfileDatabase.kt` to register both new entities/DAOs and a non-destructive Room auto-migration from version 1 to 2; generate `app/schemas/com.nestvest.profile.data.ProfileDatabase/2.json` while retaining `app/schemas/com.nestvest.profile.data.ProfileDatabase/1.json`. Preserve the existing primary profile values and database invariants, create empty expense history and disabled reminder defaults, and never add destructive fallback or a second database.
- [X] T007 Refactor `app/src/main/java/com/nestvest/NestVestApplication.kt` to expose one shared `ProfileDatabase` instance and retain the existing profile repository API while making the new DAOs available for later expense/reminder repository construction; preserve lazy initialization and avoid adding a new DI framework.

**Checkpoint**: A version-1 installation can acquire the additive schema without losing
its profile. All later stories share one app-private database.

## Phase 3: User Story 1 - Capture an Expense Immediately (Priority: P1, MVP)

**Goal**: Save an amount with no optional fields, see today's expenses immediately,
switch calendar periods, and identify a saved expense for review.

**Independent test**: Complete setup or reopen with an existing profile, enter an amount,
save offline, and verify one attributed expense remains after restart. Today's expenses
are shown by default from capture; week/month rows show the local day and time.
Invalid/failed saves preserve input and never report success.

### Tests for User Story 1

- [X] T008 [P] [US1] Add exact amount parsing/formatting tests in `app/src/test/java/com/nestvest/expense/ExpenseAmountTest.kt` for empty, malformed, zero/negative, excessive-precision, maximum-valid and overflowing values, locale decimal input, and currencies with zero/two/three fraction digits; enforce the data-model upper bound "`Long.MAX_VALUE` minor units" without silent rounding (FR-004/FR-005/FR-019; SC-004).
- [X] T009 [P] [US1] Add local-calendar period and total tests in `app/src/test/java/com/nestvest/expense/ExpensePeriodTest.kt` for inclusive starts/exclusive ends, midnight/month/year boundaries, daylight-saving changes, Sunday/Monday week starts, current-period rollover, newest-first ordering, exact sums that exceed one expense's `Long` range, and separate totals for different currencies (FR-010/FR-019; SC-006).
- [X] T010 [P] [US1] Add file-backed migration, schema, and create/read persistence tests in `app/src/androidTest/java/com/nestvest/expense/ExpensePersistenceTest.kt` and `app/src/androidTest/java/com/nestvest/expense/ExpenseMigrationTest.kt`; assert version-1 profile UUID/name/currency/nullable appearance and existing invariants survive, version-2 expense PK is not auto-generated, history starts empty, reminders default off, restart preserves currency/time/creator, and distinct explicit same-amount/same-time saves have different IDs. Cover storage failures without partial success (FR-003/FR-006/FR-020/FR-021; SC-003/SC-004/SC-012/SC-013).
- [X] T011 [P] [US1] Add capture/history state tests in `app/src/test/java/com/nestvest/expense/ExpenseViewModelTest.kt` with fake repositories and controlled time: profile-loading failures must not invent a profile, saving disables duplicate in-flight submits, failure retains draft, success clears only amount, today's history refreshes, selected IDs remain stable, reopening history defaults to Today, and no optional detail is required (FR-001/FR-002/FR-005/FR-008/FR-010).
- [X] T012 [P] [US1] Add Compose acceptance tests in `app/src/androidTest/java/com/nestvest/expense/ExpenseCaptureTest.kt` for direct entry after existing/new profile setup, accessible keypad and error/status announcements, amount-only save, default today's list visible from capture, period switching/date labels, empty/read-error/retry states, and selection of the correct same-amount expense for read-only review; verify the Phase 1 setup/error paths still work (FR-001/FR-007/FR-010/FR-018; SC-005/SC-006/SC-012).

### Implementation for User Story 1

- [X] T013 [US1] Implement exact locale-aware amount parsing and formatting in `app/src/main/java/com/nestvest/expense/domain/ExpenseAmount.kt`, reusing `app/src/main/java/com/nestvest/profile/domain/CurrencyCatalog.kt`; enforce required positive minor units and the currency's supported fraction digits, reject malformed/excess-precision/overflow input instead of rounding, and avoid binary floating-point storage or arithmetic.
- [X] T014 [US1] Implement period ranges and overflow-safe currency-separated totals in `app/src/main/java/com/nestvest/expense/domain/ExpensePeriod.kt`; quote and enforce `period` as "`DAY`, `WEEK`, or `MONTH`" where "`DAY` is the default when opening expense history", `startInclusive` as "Local start of selected day/week/month converted to an instant", and `endExclusive` as "Start of next local day/week/month converted to an instant". Use injected current time/zone/locale, locale first weekday, half-open queries, and recompute when the local date/zone changes.
- [X] T015 [US1] Implement create/read/observe in `app/src/main/java/com/nestvest/expense/data/RoomExpenseRepository.kt` using T003/T004 and T013/T014; generate one UUID per explicit new expense, capture the primary profile currency/ID and instant together, leave all optional fields absent, and follow `RoomProfileRepository` typed storage-error/cancellation handling. Do not hide query/mapping failures behind an empty list or overwrite creator/capture identity.
- [X] T016 [US1] Implement capture/history/review state and intents in `app/src/main/java/com/nestvest/expense/ui/ExpenseViewModel.kt`, `app/src/main/java/com/nestvest/expense/ui/ExpenseUiState.kt`, and `app/src/main/java/com/nestvest/expense/ui/ExpenseIntent.kt`; retain retry input, expose truthful per-operation loading/errors, save before navigating to the chosen record, observe current history, and save only lightweight selected ID/period/draft UI state without automatically replaying an interrupted save after recreation.
- [X] T017 [US1] Build amount capture in `app/src/main/java/com/nestvest/expense/ui/ExpenseCaptureScreen.kt` using mockup screen 01's restrained theme, locale currency display, accessible custom keypad, primary **Save expense**, and subtle **Add details now**. Make today's captured expenses visible by default in the capture experience with a reachable history/period control; do not require navigating away just to discover today's entries. Confirm save and clear amount only after success; keep optional enrichment dismissible.
- [X] T018 [US1] Build period history in `app/src/main/java/com/nestvest/expense/ui/ExpenseHistoryScreen.kt` using mockup screen 02: Today/This week/This month exclusive controls, newest-first selectable expense rows, time for today and date plus time for week/month, exact currency-separated totals/counts, and empty/error/retry states that retain **Add an expense**. Keep a settings entry point ready for US3 without showing a nonfunctional control in the MVP.
- [X] T019 [US1] Build reusable summary and read-only review in `app/src/main/java/com/nestvest/expense/ui/ExpenseSummaryCard.kt` and `app/src/main/java/com/nestvest/expense/ui/ExpenseReviewScreen.kt`; show the exact selected record's currency amount/capture date/time and distinguish saved details from absent ones. Route **Add details now** here until US2 supplies editing; no vendor/category/receipt work may be required to complete the MVP.
- [X] T020 [US1] Wire `app/src/main/java/com/nestvest/expense/ui/ExpenseRoute.kt`, `app/src/main/java/com/nestvest/MainActivity.kt`, `app/src/main/java/com/nestvest/NestVestApplication.kt`, and `app/src/main/java/com/nestvest/profile/ui/ProfileRoute.kt` so a loaded/newly saved profile enters capture without the old terminal saved-profile screen, while loading/read-error/setup behavior remains intact; supply the real expense repository and lifecycle-aware ViewModel state, connect capture/history/read-only review and Back, and add US1 labels/errors/plurals/semantics in `app/src/main/res/values/strings.xml`. Keep profile identity intact; do not introduce a creator selector.

**Checkpoint**: US1 is independently usable with no receipt permissions, notification
permission, enrichment, or scheduling implementation. Run T008-T012 and verify the MVP.

## Phase 4: User Story 2 - Add Details When Convenient (Priority: P2)

**Goal**: Enrich the right saved expense independently, correct its amount, or confirm
deletion while retaining already-confirmed data.

**Independent test**: Save an amount, reopen its history row, confirm only one optional
detail, leave, and reopen. The detail remains attached to the same expense and creator;
the summary remains visible during editing. Canceling deletion or receipt selection
changes nothing; failed writes preserve unaffected data and expose retry.

### Tests for User Story 2

- [X] T021 [P] [US2] Add detail/receipt persistence tests in `app/src/androidTest/java/com/nestvest/expense/ExpenseEnrichmentPersistenceTest.kt` and `app/src/androidTest/java/com/nestvest/expense/ReceiptFileStoreTest.kt` for single-field updates, vendor trimming/blank removal/80 Unicode code-point boundary, category toggling, image attach/replace/remove/cancel, inaccessible source/copy/database failures, cleanup after removal/deletion, and restart. Test concurrent/out-of-order updates to different detail fields without lost data, amount correction preserving expense ID/capture time/creator, and error on updating a missing record (FR-009/FR-012/FR-013/FR-014/FR-021; SC-008/SC-009/SC-013).
- [X] T022 [P] [US2] Add detail-state and Compose tests in `app/src/test/java/com/nestvest/expense/ExpenseDetailViewModelTest.kt` and `app/src/androidTest/java/com/nestvest/expense/ExpenseDetailTest.kt`: identify same-amount records by selected ID, keep summary visible with large text/IME/scroll, confirm vendor on blur or Done/explicit Back, wait for persistence before leaving, retain failed drafts, avoid committing an abandoned unconfirmed draft on recreation, save/toggle category independently, preserve data when picker is canceled, and cover corrected amount plus confirmed/canceled/failed deletion (FR-011/FR-012/FR-013/FR-018; SC-008/SC-009/SC-011).

### Implementation for User Story 2

- [X] T023 [US2] Implement image-only selection/copy support in `app/src/main/java/com/nestvest/expense/data/ReceiptFileStore.kt` with app-private internal filenames, Android API 26+ picker compatibility/fallback, no broad media permissions, and no durable external URI. Copy successfully before referencing a file; retain the previous receipt until replacement commits, clean up unreferenced staged/old images, and surface file/copy/deletion failures explicitly rather than showing full success. Handle cancellation separately from failure and keep blocking file IO off the UI thread.
- [X] T024 [US2] Extend `app/src/main/java/com/nestvest/expense/domain/ExpenseRepository.kt`, `app/src/main/java/com/nestvest/expense/data/ExpenseDao.kt`, and `app/src/main/java/com/nestvest/expense/data/RoomExpenseRepository.kt` with focused vendor/category/receipt/amount updates and confirmed local deletion. Enforce T002's verbatim field constraints, use individual field updates rather than stale full-row replacement, check affected rows, preserve ID/capture time/creator, and coordinate T023 file ownership with Room commits so failed changes retain existing details and successful remove/delete releases receipt storage.
- [X] T025 [US2] Implement detail state/confirmation handling in `app/src/main/java/com/nestvest/expense/ui/ExpenseDetailViewModel.kt` and `app/src/main/java/com/nestvest/expense/ui/ExpenseDetailUiState.kt`; load by immutable selected ID, keep vendor text as a draft until focus loss/explicit leave, trim/clear it as appropriate, wait for pending writes before navigating, and retain retryable drafts on failure. Category selection commits immediately and selecting the active category clears it; receipt commits only after copy succeeds; do not automatically persist unfinished drafts on recreation or use a final save-all operation.
- [X] T026 [US2] Build editing in `app/src/main/java/com/nestvest/expense/ui/ExpenseDetailScreen.kt`, reusing `app/src/main/java/com/nestvest/expense/ui/ExpenseSummaryCard.kt`; keep the summary visible while the form scrolls/keyboard opens, show no vendor **Save** button, provide category/receipt attach-replace-remove actions, explicit validated amount correction, **Done for now**, and delete confirmation with safe cancel/error handling. Make all edits optional and preserve the mockup screen 03 visual hierarchy and accessible state.
- [X] T027 [US2] Integrate detail ViewModel/picker/navigation into `app/src/main/java/com/nestvest/expense/ui/ExpenseRoute.kt`, `app/src/main/java/com/nestvest/MainActivity.kt`, and `app/src/main/res/values/strings.xml`; replace the MVP read-only destination with editing for history selection and successful **Add details now**, retain the originating history period on return, clear amount only after the initial record saves, and ensure both explicit Back and **Done for now** honor vendor confirmation/write failures. Remove `app/src/main/java/com/nestvest/expense/ui/ExpenseReviewScreen.kt` only if it has become unused.

**Checkpoint**: US1 and US2 pass independently of reminders; partial enrichment survives
restart and every correction/delete applies to the selected expense only.

## Phase 5: User Story 3 - Build a Consistent Capture Habit (Priority: P3)

**Goal**: Offer a gentle daily reminder, off by default, with local-time choice,
at-most-once delivery, and no permission dependency for capture.

**Independent test**: Open settings from history, enable with permission, select a time,
trigger controlled work, open capture from the notification, and disable. Verify denial,
revocation, channel blocking, restart, and worker retries never block capture or send
more than one notification per local day.

### Story-Specific Setup and Tests

- [X] T028 [US3] Add compatible AndroidX WorkManager runtime and test dependencies through `gradle/libs.versions.toml` and `app/build.gradle.kts`, following the existing version-catalog pattern and minSdk 26/target 36 constraints; add no networking, analytics, exact-alarm, or unrelated library dependency.
- [X] T029 [P] [US3] Add reminder repository/scheduling/worker tests in `app/src/test/java/com/nestvest/expense/ReminderSchedulerTest.kt` and `app/src/androidTest/java/com/nestvest/expense/ExpenseReminderWorkerTest.kt` for off defaults, valid/invalid local minutes, unique work replacement/cancellation, day/zone/DST changes, delayed runs, process/reboot persistence, permission/channel disable, and atomic same-day claim across retried/concurrent workers. Ensure a failed/skipped notification cannot terminate all future daily scheduling (FR-015/FR-016/FR-017; SC-010).
- [X] T030 [P] [US3] Add settings state/UI tests in `app/src/test/java/com/nestvest/expense/ReminderViewModelTest.kt` and `app/src/androidTest/java/com/nestvest/expense/ReminderSettingsTest.kt` for the history settings entry/return, accessible off-by-default switch/time picker, grant/deny/revoke/dismissed permission outcomes without repeated prompting, persistence failures, disabling in one visit, and notification navigation to capture after profile loading (FR-015/FR-016/FR-017/FR-018; SC-010).

### Implementation for User Story 3

- [X] T031 [US3] Implement `app/src/main/java/com/nestvest/expense/domain/ReminderRepository.kt` and `app/src/main/java/com/nestvest/expense/data/RoomReminderRepository.kt` over T005; enforce all quoted preference constraints, persist permission-prompt history before requesting it, expose truthful preference write failures, and atomically claim the current local date before delivery without changing the expense ledger. A claim suppresses same-day retries even if posting fails; errors remain explicit and later-day scheduling remains possible.
- [X] T032 [US3] Implement discreet local notification/channel handling in `app/src/main/java/com/nestvest/expense/reminder/ExpenseReminderNotifier.kt` and update `app/src/main/AndroidManifest.xml` with only the needed notification permission; configure no sound/vibration escalation, a stable notification ID and safe explicit PendingIntent into capture, and permission/channel checks on API 26+. Do not declare internet, broad gallery, or exact-alarm permissions; retain existing backup/data-transfer protections.
- [X] T033 [US3] Implement `app/src/main/java/com/nestvest/expense/reminder/ReminderScheduler.kt` and `app/src/main/java/com/nestvest/expense/reminder/ExpenseReminderWorker.kt` using persisted unique one-time WorkManager scheduling near the next selected local time; re-evaluate enabled/permission/channel/date at execution, claim a date before posting, schedule the next eligible occurrence even when the current day was already claimed, and make disable/revoke cancel work without an in-flight worker resurrecting it. Reconcile stale time/zone/settings on app resume, tolerate OS delays, and do not request exact-alarm access.
- [X] T034 [US3] Build `app/src/main/java/com/nestvest/expense/ui/ReminderViewModel.kt`, `app/src/main/java/com/nestvest/expense/ui/ReminderUiState.kt`, and `app/src/main/java/com/nestvest/expense/ui/ReminderSettingsScreen.kt` from mockup screen 04: off-by-default accessible toggle, local time selection, approximate-delivery copy, inline unavailable/error states, and return to history. Request notifications only after explicit enable on API 33+, never repeatedly prompt after denial, offer a deliberate system-settings path, and keep capture usable on all outcomes.
- [X] T035 [US3] Wire reminder construction, worker dependencies, permission launcher, resume reconciliation, settings entry/return, and notification-open routing in `app/src/main/java/com/nestvest/NestVestApplication.kt`, `app/src/main/java/com/nestvest/MainActivity.kt`, `app/src/main/java/com/nestvest/expense/ui/ExpenseRoute.kt`, `app/src/main/java/com/nestvest/expense/ui/ExpenseHistoryScreen.kt`, and `app/src/main/res/values/strings.xml`; make history's settings gear functional, retain its selected period, and keep initial profile loading/error/setup authoritative before opening capture.

**Checkpoint**: All three stories work; reminders remain entirely optional and disabled
unless deliberately enabled with permitted delivery.

## Phase 6: Polish and Cross-Cutting Concerns

**Purpose**: Verify full-feature quality and record actual evidence, not assumed success.

- [X] T036 [P] Add cross-screen accessibility/localization regression coverage in `app/src/androidTest/java/com/nestvest/expense/ExpenseAccessibilityTest.kt` and `app/src/androidTest/java/com/nestvest/expense/ExpenseLocaleTest.kt` for English fallback, non-US number/currency/date formats, Sunday/Monday weeks, large fonts, selected/checked semantics, touch targets, error/save announcements, and persistent enrichment summary. Fix only resulting feature issues in `app/src/main/java/com/nestvest/expense/ui/` and `app/src/main/res/values/strings.xml` after concurrent test authoring is complete (FR-018/FR-019; SC-011).
- [X] T037 [P] Add local-only/storage-protection regression coverage in `app/src/androidTest/java/com/nestvest/expense/ExpensePrivacyTest.kt` for offline capture/history, private database/receipt locations, no exported receipt access, no financial data in public logs, no cloud backup of new data, and no internet/broad-media/exact-alarm permissions; verify `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/backup_rules.xml`, and `app/src/main/res/xml/data_extraction_rules.xml` preserve Phase 1 protections and document any actual platform limitation rather than claiming unsupported encryption guarantees (FR-007; SC-003).
- [ ] T038 Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` and `./gradlew :app:connectedDebugAndroidTest`, then execute `specs/002-daily-expense-capture/quickstart.md` scenarios on API 26+ and API 33+ with TalkBack, permission changes, local date/zone changes, picker failures, restart, and migration. Record actual task/device/manual outcomes and blockers in that existing quickstart file; fix feature regressions before marking complete and do not claim unavailable device tests passed.
- [ ] T039 Define and, when participants are available, execute the usability checks in `specs/002-daily-expense-capture/quickstart.md` for SC-001 (at least 90% first-attempt amount saves), SC-002 (median capture-to-save at most 10 seconds), and SC-007 (at least 90% correct selected-expense identification). Record sample size, observed results, and unperformed checks separately from automated tests; do not treat build/Compose success as proof of human usability.
- [X] T040 Update `README.md` and verify `specs/002-daily-expense-capture/quickstart.md` and `specs/002-daily-expense-capture/plan.md` describe the shipped flows, actual validation commands/results, and remaining handoffs: Phase 3 canonical category mapping, Phase 4 separate ingestion/confirmation provenance, and Phase 6 creator resolution, duplicate/conflict policy, receipt portability, and deletion tombstones. Keep household sync/import, member selection, budgets, and parsing out of this implementation; do not claim the PRD functional-key question or future merge tests are solved by UUIDs alone.

## Dependencies and Execution Order

### UI Feedback Follow-up (2026-10-23)

- [X] T044 Align reminder settings with mockup screen 04 using the shared app bar, paper/green palette, bordered settings card, time field, and fixed bottom return action. Preserve permission/scheduling behavior; add native large-text/footer/time-dialog coverage and validate reminder/navigation tests, build, lint, and JVM tests. Depends on T043.

The user's latest feedback supersedes the embedded-history clauses in T012/T017:
capture must not render expense history; its Expenses action opens the separate Today
summary. This is an intentional behavior revision, not a regression.

- [X] T041 Add native mockup-alignment regressions in `app/src/androidTest/java/com/nestvest/expense/ExpenseDesignTest.kt`: capture contains no history, bottom actions remain outside scrolling content, history rows have an icon/copy/value layout, and summary cards use pale fill and saved-detail pills. Update existing UI tests for the intentional capture-history separation.
- [X] T042 Implement shared visual tokens, app bar, fixed footers, and decorative icons in `app/src/main/java/com/nestvest/expense/ui/ExpenseDesign.kt`; align capture, history, detail, and summary components with mockup screens 01–03 without changing persistence/confirmation semantics. Preserve minimum 48dp targets, locale-ready strings, and usable compact layouts with large text/IME.
- [X] T043 Validate the redesign with focused native tests, native visual comparison, and build/lint/JVM regression checks; update specification, contract, plan, README, and quickstart with the intentional no-history capture flow and actual outcomes. Leave T038/T039 open for outstanding manual/participant validation.

Follow-up prerequisites: T041 depends on T027/T035; T042 depends on T041; T043 depends
on T042. These follow-ups do not require the pending participant/manual matrix.

### Phase and Story Graph

```text
Setup T001
    |
Foundation T002-T007
    |
US1 T008-T020 (MVP)
    |----------------------|
US2 T021-T027          US3 T028-T035
    |----------------------|
Polish T036-T040
```

US2 and US3 depend on US1's saved records, capture/history navigation, and app wiring,
but neither depends on the other's feature behavior. Their final integration tasks
modify shared files: serialize T027 and T035 or explicitly coordinate those changes.
The recommended single-developer order is US1, US2, US3, then polish.

### Task Prerequisites

These are actual prerequisites, not inferred solely from numeric order.

| Task(s) | Prerequisites |
|---|---|
| T001 | Existing design documents |
| T002 | T001 |
| T003, T004, T005 | T002; run sequentially unless coordinating their shared domain contracts |
| T006 | T004, T005 |
| T007 | T003, T006 |
| T008-T012 | T007; separate test files may be authored in parallel |
| T013 | T008 |
| T014 | T009 |
| T015 | T010, T013, T014 |
| T016 | T011, T015 |
| T017, T018, T019 | T012, T016; serialize shared UI/resource decisions |
| T020 | T017, T018, T019 |
| T021, T022 | T020; independent test files |
| T023 | T021 |
| T024 | T021, T023 |
| T025 | T022, T024 |
| T026 | T025 |
| T027 | T026 |
| T028 | T020 |
| T029, T030 | T028; independent test files |
| T031 | T029 |
| T032 | T029, T031 |
| T033 | T031, T032 |
| T034 | T030, T031, T033 |
| T035 | T032, T033, T034; serialize app/route/resource edits with T027 |
| T036, T037 | T027, T035; author tests in parallel, serialize any fixes to shared files |
| T038 | T036, T037 |
| T039 | T038 for an acceptance-ready build; participant availability is a separate gate |
| T040 | T038 and the actual completed/pending status of T039 |

### Parallel Examples

- **US1**: After T007, author T008 amount tests, T009 period tests, T010 Room
  migration/persistence tests, T011 state tests, and T012 Compose tests in their separate
  files concurrently. Implementation then follows the prerequisite table.
- **US2**: After T020, author T021 file/Room tests while T022 authors state/Compose
  tests. Do not run concurrent edits to the shared repository or route files.
- **US3**: After T028, author T029 worker/scheduler tests and T030 settings/UI tests
  concurrently. US3 reminder work can progress alongside US2 detail work until the
  shared-file integrations T027/T035.
- **Polish**: Author T036 accessibility/locale tests and T037 privacy tests concurrently;
  merge shared resource/manifest corrections sequentially before T038.

## Requirement Coverage

| Requirements / outcomes | Primary task coverage |
|---|---|
| FR-001, FR-002 | T011, T012, T016, T017, T020 |
| FR-003, FR-004, FR-005 | T008, T010, T013, T015, T016, T020 |
| FR-006, FR-007 | T006, T010, T015, T037, T038 |
| FR-008, FR-009 | T012, T017, T021, T022, T024-T027 |
| FR-010 | T009, T012, T014, T016-T020 |
| FR-011 | T019, T022, T026, T036 |
| FR-012, FR-013, FR-014 | T021-T027 |
| FR-015, FR-016, FR-017 | T005, T028-T035 |
| FR-018, FR-019 | T008, T009, T012, T017-T020, T022, T026, T030, T033, T036, T038 |
| FR-020, FR-021 | T001-T004, T010, T015, T021, T024, T040 |
| SC-001, SC-002, SC-007 | T017, T019, T026, T039; participant validation remains distinct |
| SC-003, SC-004, SC-005, SC-006 | T008-T020, T037, T038 |
| SC-008, SC-009 | T021-T027 |
| SC-010 | T029-T035 |
| SC-011 | T022, T026, T030, T033, T036, T038 |
| SC-012, SC-013 | T010, T012, T015, T021, T024; actual future sync validation deferred |

## Implementation Strategy

### MVP First

1. Complete Setup and Foundation, then US1.
2. Validate offline amount-only capture, default today's history, local-calendar
   filtering, stable identity, primary-profile attribution, and restart/migration.
3. Stop for a usable capture/history demo; no optional details or notification
   permissions are needed. The MVP's read-only review is replaced by US2 editing.

### Incremental Delivery

1. Add US2; validate independent detail confirmation and selected-record integrity
   before adding reminders.
2. Add US3; prove denial/disable never blocks the ledger and retries cannot duplicate a
   day's reminder.
3. Complete full regression, privacy/accessibility/locale validation, and actual
   usability measurements or clearly documented pending participant checks.

### Scope and Completion Rules

- Mark a task complete only after its persistent outcome and applicable tests are
  verified. A test-authoring task does not imply the corresponding feature passes.
- Do not manufacture a primary profile, silently default failed reads to empty data,
  erase creator attribution, silently round currency, or report failed storage as saved.
- Do not implement family membership, transfer, parsing, budgets, cloud services, or a
  merge algorithm here. T040 retains their explicit dependencies without expanding scope.
- The existing HTML mockup is visual/behavioral guidance only; native persistence,
  platform permissions, assistive technology, and reminders require native evidence.
