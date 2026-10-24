# Quickstart: Phase 3 — Dashboard Navigation

Validation guide for the native implementation. Use
[ui-mockups.html](./ui-mockups.html) for visual and interaction comparison and
[contracts/dashboard-ui.md](./contracts/dashboard-ui.md) for expected screen behavior.
The mockup uses synthetic in-memory data and does not verify persistence, app privacy,
or production accessibility.

## Prerequisites

- JDK 21, Android SDK platform/build tools 36, and the repository Gradle wrapper.
- `ANDROID_HOME` or an untracked `local.properties`.
- Android emulator or device running API 26 or later.
- For connected acceptance tests, an Android emulator/device; TalkBack for
  assistive-technology verification.

## Build and Automated Tests

From the repository root:

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Expected: dashboard state and summary unit tests pass; Room migration and preference
persistence tests pass; Compose navigation, pinning, error, accessibility, and locale
tests pass; lint reports no errors; and the debug app assembles.

## Manual Acceptance Scenarios

1. **Profile gate and first launch**: Clear app data or use a fresh database. Confirm
   profile setup remains the first screen. Complete profile creation and verify the
   dashboard becomes the starting point, greets the saved profile by name, and shows
   the local date without presenting mockup sample activity.
2. **Default destinations**: Confirm Customize appears in the **Pinned for you**
   heading row before its tiles; Add expense and Expenses appear under **Pinned for
   you**, and Reminders is under **More**. Verify each tile has a relevant leading
   icon, the Expenses summary uses a concise count/total pill when available, and
   unavailable later-phase modules are not shown.
3. **Capture handoff**: Select Add expense and verify the existing amount-first capture
   opens, saving still requires only a valid amount and explicit save, and existing
   history/detail behavior remains available.
4. **Expenses handoff**: Select Expenses and confirm Today history is shown with local
   records, correct currency formatting, and no reliance on mockup sample data.
5. **Reminders handoff**: Select Reminders from More. Confirm existing opt-in,
   permission, and local scheduling behavior is unchanged. Tap the NestVest logo from
   reminders and verify it returns directly to the dashboard.
6. **Return to dashboard**: Tap the NestVest logo from capture, history, expense
   detail, reminders, More, and Customize. Confirm each action returns to the dashboard
   without losing saved expense or pin data. System Back may retain established
   history/detail navigation.
7. **Pinning persistence**: Open Customize, pin Reminders, unpin Expenses, and save.
   Confirm the dashboard groups update. Close/reopen the app and confirm choices
   persist; all three destinations remain accessible. Pin every destination and
   confirm the dashboard hides the More section and See all action. Save and verify
   the brief “Dashboard saved” confirmation disappears without leaving a persistent
   success message. Restore the defaults and confirm the default set.
8. **Failed preference save**: Inject or simulate a preference write failure. Confirm
   the old saved pinned set remains in force, the customization remains available for
   retry, and accessible failure feedback appears.
9. **Summary states**: With local expenses today, verify the count and total match the
   underlying records. With no expense history, verify no false zero is presented.
   With older history but no expenses today, verify the tile communicates zero activity
   for today without implying the user has never recorded expenses. With a summary read
   failure, verify the tile states that the summary is unavailable and still opens
   history.
10. **Unavailable destination**: Simulate a pinned option becoming unavailable. Confirm
   it cannot open a broken destination and other available options remain usable.
11. **Deep link from reminder**: Trigger the existing reminder capture intent. Confirm
    a configured profile opens directly to amount capture and a missing profile still
    receives setup before capture.
12. **Offline, accessibility, and localization**: Disable network access and repeat
    navigation and preference saving. Navigate with TalkBack and keyboard/switch
    access. Test enlarged text and an untranslated or non-English device locale;
    verify readable layout, meaningful announced state, localized strings, and
    locale-aware dates/currency.
13. **Migration and privacy**: Open a database fixture from schema version 2 and verify
    profile, expenses, and reminder preferences remain intact with default dashboard
    preferences. Confirm no internet or analytics integration is introduced.

## Evidence to Record

Record Gradle task results and Android API/device for connected tests. The quantitative
navigation, customization-time, and satisfaction criteria in the specification require
usability testing; automated tests alone do not demonstrate those participant outcomes.

## Execution Record

- `./gradlew :app:testDebugUnitTest`: passed.
- `./gradlew :app:compileDebugAndroidTestKotlin`: passed; migration, navigation,
  customization, accessibility, and summary instrumentation test sources compile.
- `./gradlew :app:assembleDebug`: passed.
- `./gradlew :app:lintDebug`: passed.
- `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug`: passed after aligning the dashboard welcome, More screen, and app-logo navigation.
- `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug`: passed after aligning the dashboard tile icons, Customize placement, and concise expense-summary pill.
- Connected instrumentation and manual acceptance scenarios were not run because
  `adb` is unavailable in the implementation environment. No device/API results or
  usability-study outcomes are claimed.
