# Quickstart: Phase 2 — Ultra-Fast Daily Expense Capture

Validation guide for the native implementation. The HTML mockup is a behavior and visual
reference only; it uses sample data and is not evidence of local persistence,
accessibility, reminder delivery, or receipt privacy.

## Prerequisites

- JDK 21, Android SDK platform/build tools 36, Android Gradle Plugin 9.2.1,
  Gradle 9.4.1 wrapper, Kotlin 2.2.10, Room 2.7.1.
- `ANDROID_HOME` or an untracked `local.properties`.
- Android emulator or device running API 26 or later.
- Existing profile database at schema version 1 for migration tests.
- For connected acceptance: API 26+ test device/emulator; API 33+ for notification
  runtime-permission cases; TalkBack for assistive-technology verification.

## Build and automated tests

From the repository root:

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Expected: amount parsing and date-period unit tests pass; Room migration and expense
CRUD/accessibility UI tests pass; no lint errors; debug app builds.

## Manual acceptance scenarios

Use [ui-mockups.html](./ui-mockups.html) for visual and interaction comparison and
[contracts/expense-ui.md](./contracts/expense-ui.md) for expected screen behavior.

1. **Fast capture**: Open with a configured profile, enter a positive amount, and choose
   **Save expense**. Confirm the local confirmation, cleared amount, selected profile
   currency, and new item in today's history via the app-bar **Expenses** action.
   Add Expense itself must not show history. Verify the stored `createdByProfileId`
   equals the stable primary profile ID. Repeat offline.
2. **Invalid amount**: Try empty, zero, negative, malformed, too-precise, and out-of-range
   values. Confirm no item is created, accessible feedback is announced, and retry input
   is retained as appropriate.
3. **Immediate details**: Enter an amount and choose the subtle **Add details now**
   action. Confirm the record is saved before details open; inject a failed write and
   confirm it stays on capture with the amount retained.
4. **Period history**: Compare Today, This week, and This month. Confirm local-calendar
   boundaries, newest-first order, date plus time in week/month rows, correct totals,
   and a useful empty state with the add action available.
5. **Vendor confirmation**: Open an expense, enter a vendor, and leave the field. Confirm
   the normalized vendor is saved with no separate vendor Save button. Tap **Done for
   now** while the field is focused and confirm navigation waits for persistence; inject
   a failed write and verify the draft remains for retry. Verify earlier confirmed
   details remain.
6. **Enrichment card**: Add category and a receipt independently. Confirm amount,
   currency, capture time, and saved details remain visible. Cancel receipt selection or
   inject a storage failure and confirm other detail fields remain intact.
7. **Correction and deletion**: Correct an amount and verify the same expense remains
   one history row. Cancel a delete confirmation and verify it remains; confirm deletion
   and verify it disappears from local history.
8. **Reminders**: Open the history settings icon. Confirm reminders are off initially;
   enable, choose time, disable, and verify scheduling/cancellation. Deny/revoke
   notification permission and confirm expense capture remains fully usable without
   repeated permission prompts. Verify a retried worker cannot send twice on one local
   day.
9. **Persistence and migration**: Create an expense with details, close/reopen the app,
   and verify its ID-linked data remains. Open a database fixture from version 1 and
   migrate to version 2; verify the profile fields and UUID are preserved and history
   starts empty. Verify the creator profile ID remains unchanged after detail updates
   and amount correction. Verify a failed write does not produce a success state.
10. **Privacy, accessibility, and locale**: Verify no internet permission or external
    requests are introduced; receipt files remain app-private. Navigate all four flows
    with TalkBack and keyboard/switch access. Test English and an untranslated device
    locale, larger text, and a non-US currency/locale. Confirm accessible state,
    currency fractions, local dates, and week start are correct.
11. **Family-sync handoff**: Confirm the expense record has an immutable creator ID
    suitable for a future trusted household transfer. Phase 6 validation must verify
    imported expenses retain the source creator identity rather than adopting the
    receiving device's primary profile.

## Evidence to record

Record Gradle task results and Android API/device for connected tests. Keep functional
test outcomes separate from the 90% first-attempt usability and 10-second median timing
targets; those require participant testing rather than build/test results.

## Usability protocol (SC-001, SC-002, SC-007)

Recruit representative first-time users without coaching the capture or enrichment
controls. Record participant count, device/API, language, accessibility settings, and
the number who save a valid amount on the first attempt. Measure elapsed time from the
capture screen appearing until successful-save feedback, then calculate the median
across participants. Show at least two similar-amount expenses with different times or
details and record whether each participant selects and identifies the requested
expense from its summary.

Acceptance thresholds: first-attempt saves at least 90%, median time at most 10 seconds,
and correct expense identification at least 90%. Keep raw observations local and omit
personal financial data. Participant testing has not been performed in this coding
session; these outcomes must not be inferred from automated tests.

## Implementation validation record (2026-10-23)

Executed on macOS with the existing `Pixel_8` emulator, Android API 37 (Android 17):

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --console=plain
```

- **JVM**: 39 tests, zero failures/errors/skips.
- **Instrumented**: 50 tests, zero failures/skips, including the existing profile suite.
- **Build**: debug APK assembled successfully; both Kotlin test source sets compile.
- **Lint**: passed with no errors. Remaining warnings concern dependency/toolchain
  upgrades; an unused feature resource found during validation was removed.
- **Persistence/integrity**: version-1 migration preserves profile identity and
  invariants; same-time expenses retain distinct IDs; reopening preserves attribution;
  half-open queries sort deterministically; invalid stored records surface errors.
- **Details/receipts**: independent updates, Unicode vendor boundary, failed drafts,
  category toggle, correction, delete confirmation, private image copy, replacement
  rollback, inaccessible sources, cleanup-pending commits, and later cleanup are tested.
- **Reminders**: default-off settings, persisted prompt suppression, grant/revoke/deny
  state handling, unique work replacement/cancellation, stale settings/zone, DST,
  atomic concurrent date claims and rollback, and successor-before-claim behavior
  are tested. History settings entry/return and capture without delivery permission
  are covered by native navigation tests.
- **Accessibility/locales/privacy**: 48dp keypad controls, selected/checked semantics,
  error/save live regions, pinned summary while scrolling at 200% font scale, English
  fallback, German currency/date formatting and local-day conversion, forbidden
  permissions, protected exported WorkManager system components, cloud exclusions,
  and database/receipt local-transfer rules are covered.
- **Native launch smoke**: after the test runner removed the test installation, the
  debug APK was installed and launched with `openExpenseCapture=true`; cold launch
  succeeded and correctly displayed primary-profile setup rather than bypassing it.
  This is not evidence of actual notification delivery/tap behavior.

Initial validation exposed a privacy-test assumption that all exported services and
receivers belonged to the profile-only app. The test now permits only WorkManager's
system job service protected by `BIND_JOB_SERVICE` and diagnostic receiver protected by
`DUMP`, alongside the existing protected profile installer receiver. No network/media/
exact-alarm permission was added. Full validation subsequently passed.

### Still outstanding

- **T038 (partially complete)**: no API-26 emulator/device is configured; only API 37
  was available. Manual TalkBack traversal, real notification grant/denial/channel
  changes and taps (cold/warm/profile-gated), real OS reboot, system date/time-zone
  changes, actual media-provider cancel/failure, and keyboard/rotation scenarios still
  need device-level execution. Automated simulations above do not replace these checks.
- **T039 (protocol complete, execution pending)**: no usability participants were
  available. Sample size and observed first-attempt rate, median time, and correct
  identification rate are **not measured**. Run the protocol above before claiming
  SC-001, SC-002, or SC-007.

No household sync, ingestion, member picker, planning budget, or merge policy was
implemented. UUID identity and creator provenance preserve handoffs but do not resolve
the PRD functional-key/duplicate/conflict question.

## UI feedback validation (2026-10-23)

The latest feedback intentionally separates amount entry from history. The app-bar
Expenses action opens Today; capture no longer renders period controls, totals, or
expense rows. History has icon-led rows, count/total alignment, and a fixed bottom add
action. Capture and details have fixed dark primary actions. The detail summary uses
the mockup's pale card and saved/absent-detail pills, with rectangular category controls
and an aligned receipt row.

Executed on the same Pixel_8 API-37 emulator:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nestvest.expense.ExpenseDesignTest,com.nestvest.expense.ExpenseAccessibilityTest,com.nestvest.expense.ExpenseCaptureTest,com.nestvest.expense.ExpenseDetailTest,com.nestvest.expense.ExpenseNavigationTest \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

- 11 focused Android UI tests passed, zero failures/skips. New tests initially failed
  on embedded capture history, off-screen history add action, and missing summary
  styling, then passed after implementation.
- 39 JVM tests passed; debug build and lint passed. No feature-specific lint warnings.
- Native capture/history/detail renders with synthetic INR sample data were inspected
  against the mockup palette, card layout, typography hierarchy, and bottom actions.
  Temporary rendering helpers/images were removed after inspection.
- Pixel checks verify paper, primary-button, and summary fills. Bounds checks verify
  fixed footer positioning and icon/copy/amount ordering. Existing 200% font coverage
  and a new 430dp-height detail test confirm summary/Done remain visible while form
  content scrolls. Compact layouts omit decorative headings and allow horizontal
  scrolling of summary pills.
- A real-IME visual check was attempted, but the Compose test host did not expose an
  IME inset, even with the emulator's soft-keyboard-with-hardware setting enabled.
  That setting was restored. Focused-field renders and short-viewport simulation
  do not constitute verified real-keyboard behavior; the manual IME item in T038
  remains open, as do the existing API-26/TalkBack/participant checks.

### Reminder screen alignment

Reminder settings now follows mockup screen 04 and the shared expense scaffold: brand
and local-data app bar, paper/green palette, eyebrow/two-line heading, bordered settings
card, off-state explanation, full-width time control, permission guidance, and a fixed
bottom **Back to expenses** button. Time display respects the device's 12/24-hour
preference, and the native time dialog remains cancelable.

Validation on Pixel_8 API 37:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nestvest.expense.ReminderSettingsTest,com.nestvest.expense.ExpenseNavigationTest \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

Passed: 3 focused Android tests, 39 JVM tests, debug assembly, and lint. Coverage checks
the off/on/off switch, disabled time control while off, time-dialog cancel/confirm,
fixed footer and scrolling at 200% font scale, paper-color pixel assertion, and
history/settings/capture navigation. Initial layout tests failed on the old screen
before implementation. No scheduling or permission logic changed. Existing T038/T039
manual and participant checks remain outstanding.
