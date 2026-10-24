# Kakeibo Planning & Reflection Quickstart

Use this guide to validate the Android feature end to end. The
[UI contract](./contracts/kakeibo-ui.md) defines navigation and visible behavior;
[data-model.md](./data-model.md) defines persisted and derived values.

## Prerequisites

- JDK 17 and the Android SDK required by the repository (`compileSdk` 36).
- A configured Android emulator/device for instrumentation tests.
- The app's existing Gradle wrapper and dependencies.
- A primary profile with a preferred currency.

No internet connection is required for plan, expense, progress, or reflection
operations after build dependencies are available.

## Build and Test

From the repository root:

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:lintDebug :app:assembleDebug
```

The connected test command requires a running emulator or attached test device. Use
`./gradlew :app:compileDebugAndroidTestKotlin` when instrumentation sources need a
compile-only check without a device.

## Validation Recorded for This Implementation

The following completed successfully:

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

The combined validation run also passed. Migration instrumentation was subsequently
executed on the Pixel 8 emulator using:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nestvest.dashboard.DashboardPreferenceMigrationTest,com.nestvest.kakeibo.KakeiboMigrationTest
```

This covers plaintext schema migration, encrypted reopen, source preservation on
conversion failure, and wrong-key verification without deleting the encrypted file.
A broader selection of 17 emulator tests also passed, covering the expense migration,
profile/expense persistence, and `KakeiboPrivacyTest` Android Keystore wrapping checks.

The unfiltered `./gradlew :app:connectedDebugAndroidTest` suite subsequently passed
on the Pixel 8 emulator: **82 tests, zero failures, zero skipped**, including the
Compose UI tests. Two consecutive full-suite runs passed after correcting test
scrolling/semantics selectors, navigation callback wiring, exact-input replacement,
and main-thread ViewModel setup. The database write-failure regression now injects
a rejected SQLite write and verifies saved choices remain intact; database-close
cancellation is tested separately. Unit tests and Android lint also passed in the
combined validation run. Manual end-to-end scenarios remain separate checks.

### UI consistency feedback validation

After the UI feedback changes, this combined command passed on the Pixel 8 emulator:

```sh
./gradlew :app:connectedDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

**84 instrumentation tests passed, zero failures or skips.** New regressions cover
calculated overall amounts, switching to/from an explicit cap, slider scaling,
currency-formatted review cards, dashboard shortcut-section placement, and home
brand navigation without a redundant Back control. Unit regressions cover omitted
cap persistence, explicit caps below category values, zero/invalid maxima, and
calculated-total overflow.

### Whole-feature mockup alignment validation

After aligning home, learning, planning/review, weekly pacing, progress, category
detail/assignment, and both reflection views, the same combined command passed:

```sh
./gradlew :app:connectedDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

**89 instrumentation tests passed on the Pixel 8 emulator, zero failures or skips.**
Unit tests, Android lint, and debug assembly also passed after the final layout
adjustments. Additional coverage verifies inline learning/home navigation, negative
remaining and foreign-currency exceptions, selection without assignment writes,
cancel/failure/retry/success, reflection draft retention, saved-view/edit navigation,
failed-read overwrite prevention, live totals without answer changes, weekly
save/skip validation, and cancellation of a pending reflection read on month changes.

Rendered home, reflection editor, saved reflection, and no-plan progress were manually
inspected using a synthetic USD profile. The checks covered paper/ink/sage styling,
card hierarchy, readable comparison rows, and navigation from saved reflection to
progress. Final compact progress rows, inline learning actions, and reflection copy
were subsequently covered by the successful full automated run. These visual checks
do not replace the broader manual accessibility/privacy/offline scenarios below;
T045 and T046 remain open for their outstanding manual verification.

### Multi-select assignment validation

After adding checkbox-based batch selection, the following command passed:

```sh
./gradlew :app:connectedDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nestvest.kakeibo.KakeiboFeatureUiTest,com.nestvest.kakeibo.KakeiboExpenseAssignmentTest
```

**11 focused instrumentation tests passed on the Pixel 8 emulator, zero failures or
skips.** The unit suite, lint, and debug assembly also passed. Coverage includes
independent checkbox selection/deselection, clearing selection on cancel, selected
count/disabled empty confirmation, batch success, retry after storage failure, stale
selection reset, and prevention of duplicate confirmation while saving. Real database
tests verify category-only updates and transaction rollback when a later selected
record is missing, already categorized, outside the month, in another currency, or
rejected by a SQLite write failure. This was a targeted run, not a new full-suite run.

## Acceptance Scenarios

1. **First-use learning**: Open Kakeibo on a fresh preference. Verify the prompt can
   launch slides, be dismissed, or be bypassed to planning. View or dismiss it, leave,
   and re-enter; verify the prompt does not repeat and Learn remains available.
2. **Plan with and without an overall amount**: Set a savings intention and all four
   categories using both slider and exact entry. Verify both controls stay synchronized
   and total updates. Save once with an overall available amount and verify remainder;
   save again without one and verify no balance is invented. In calculated mode,
   verify the read-only overall amount equals categories plus savings and exact entry
   increases the slider ceiling. Edit the overall amount, verify it becomes the exact
   slider ceiling, then return to calculated mode. Review formatted currency values
   in cards before saving; an omitted available amount remains omitted after reopen.
3. **Plan validation and replacement**: Exceed the provided available amount and verify
   save is blocked without changing input. Change a saved month's plan, inspect the
   review, cancel, then confirm a valid replacement. Simulate/induce storage failure
   and verify the previous saved plan remains while the draft can be retried.
4. **Weekly pacing**: Set weekly amounts for multiple locale-calendar periods,
   including the partial first/last weeks in a month. Verify displayed date boundaries,
   blank-as-zero behavior, optional skip, and rejection when a category's weekly sum
   exceeds its monthly amount. Check the two-column layout and the single-column
   layout at font scales above 1.3; Save returns to planning only after validation.
5. **Progress and chart**: Create a plan and saved expenses across dates/categories.
   Verify same-month/same-currency totals, each day's chart amount (including zero
   days), negative remaining/`Over plan`, `At limit`, `Within plan`, and zero/zero
   `No allocation`.
6. **Expense exceptions and assignment**: Include an unassigned expense and an
   other-currency expense. Verify both remain separate from category totals. Select an
   eligible unassigned expense, then another; verify both checkboxes remain selected
   and selection alone does not write. Deselect one, then Cancel and verify all
   selections clear. Select multiple and confirm; verify only their categories change
   and the correct total/chart update. Make one selected expense ineligible before
   commit; verify no selected expense is assigned and no success is announced. Induce
   a write failure and verify the whole batch is unchanged and selection is kept for retry.
7. **No plan and unavailable data**: Open progress without a plan and verify saved
   expenses remain visible with a create-plan action. Make a plan/expense read fail and
   verify the dashboard tile remains actionable without showing stale or fabricated
   numbers.
8. **Reflection**: Compare plan and recorded amounts, enter and save reflection text,
   verify the saved view, edit, leave, and reopen it. Verify saved answers remain while current expense totals refresh.
   Postpone and induce save failure; verify plan/expense records remain unchanged and
   the draft can be retried. Induce a reflection-read failure and verify Save is disabled
   until retry succeeds, avoiding an accidental overwrite of saved answers.
9. **Offline persistence, encryption, and migration**: With connectivity disabled, create a plan,
   close/reopen the app, and verify plan/reflection availability. Migrate a schema-v3
   plaintext database to SQLCipher and verify profile, expense, reminder, and dashboard
   preference records remain intact alongside the new Kakeibo tables. Inspect the
   resulting database to confirm it is not readable as plaintext SQLite; confirm the
   random encryption key is protected by Android Keystore and that a failed conversion
   leaves the original recoverable and does not open in plaintext.
10. **Accessibility/localization**: Run Compose accessibility/UI tests and inspect
    TalkBack/keyboard traversal, health/status announcements, chart textual
    representation, touch targets, large text, and localized amount/date formatting.

## Expected Outcome

All local plan/reflection data survives app restart and works offline. Progress is
reproducible from saved expenses and the selected month's plan; no unassigned or
different-currency expense is silently counted. Every failed read/write remains visibly
distinct from success or zero, and existing expense/reminder routes continue to work.
