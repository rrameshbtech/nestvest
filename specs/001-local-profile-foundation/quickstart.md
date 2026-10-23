# Quickstart: Phase 1 — Local Profile Foundation

This guide validates the implemented Android profile setup feature. It is a verification
guide, not an implementation recipe.

## Prerequisites

- Pinned build: AGP 9.2.1 (built-in Kotlin 2.2.10), Compose compiler 2.2.10,
  Gradle 9.4.1, KSP 2.3.4, Compose BOM 2025.04.01, Room 2.7.1.
- JDK 21 used for validation; Java/Kotlin bytecode targets 17.
- Compile/target API 36, minimum API 26. Set `ANDROID_HOME` to your SDK directory
  or use an untracked `local.properties` with `sdk.dir`; never commit local paths.

- Android Studio with a compatible Android SDK and emulator or test device.
- The project Android Gradle wrapper and app module.
- One emulator/device with no existing primary profile for first-run tests.
- For accessibility checks, TalkBack enabled on an emulator/device.

## Build and test

From the repository root:

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Expected outcome: unit tests pass for profile validation, ViewModel state transitions,
saved-state recovery, appearance, and save failures; connected tests pass for Room
persistence, identity/schema integrity, profile creation, localization, privacy, and
accessible Compose interactions.

## Manual acceptance scenarios

Use the [mobile mock screens](./ui-mockups.html) as a visual reference and the
[UI contract](./contracts/profile-ui.md) for expected native behavior. Open the HTML file
directly in a browser to review the studies without a server; its in-memory demo is not
a substitute for the Android persistence tests.

1. **Offline setup**: Disable network access, launch a fresh app, create a profile with
   only name and currency, and confirm setup completes without registration or network
   prompts.
2. **Optional appearance**: Repeat setup while skipping avatar and color; confirm a
   primary profile is created. If appearance choices are selected, confirm they persist.
3. **Persistence**: Close and reopen the app; confirm the same profile appears and a
   second primary profile is not created.
4. **Validation and recovery**: Try a whitespace-only name and no currency; confirm
   setup is blocked with clear guidance. Simulate a local save failure; confirm no success
   is shown and entered values can be retried.
5. **Locale and accessibility**: Test English, a non-English device locale with English
   fallback, and expanded-text/RTL pseudolocales, then navigate setup with TalkBack.
   Confirm English fallback, device-locale currency presentation where supported, focus
   order, and operable actions. English is the only shipped translation; pseudolocales
   test readiness rather than additional language delivery.
6. **Locality and data protection**: Inspect the Android data-extraction behavior to
   verify cloud backup excludes profile data and supported local device-to-device
   migration is not disabled. Confirm app behavior requires no internet access.
7. **Identity continuity**: Where supported OS-level device migration is available,
   restore the profile and verify its UUID is unchanged. This scenario does not require
   app-managed export/import or guarantee that every device supports migration.
8. **Material 3 and layout fidelity**: Compare the Android flow with all four mock studies.
   Confirm Material 3 fields, buttons, outlined surfaces, and theme retain the restrained
   hierarchy. Verify optional personalization opens as a sheet, apply/skip/dismiss return
   to setup correctly, and saving prevents duplicate submission. Test at a compact phone
   width, large system text, right-to-left layout, and with the keyboard open; all required
   fields and actions must remain reachable without clipped text or overlapping controls.
9. **Loading and truthful state**: Delay profile loading/saving and simulate read/write
   failures. Confirm loading is announced, a failed read does not open fresh setup, a
   failed save retains input, and saved-profile content appears only for a persisted record.
10. **System-restored draft**: Enter a name, currency, and applied appearance, background
    the app so Android can capture task state, terminate its background process without
    force-stop or task removal, then return through Recents. Confirm the unfinished values
    resume, not a saved-profile claim. Record the device/API and exact reproduction steps.
    Rotation and Activity recreation alone do not establish process-death recovery.
11. **Interrupted save reconciliation**: Use controlled repository timing to test
    interruption before and after commit. On recreation, a committed profile appears once
    with the same UUID; absence restores the draft for explicit retry. An injected read
    failure preserves the draft behind read retry and does not expose new-profile setup.
    Unit tests should recreate the ViewModel with captured saved state for all three cases,
    complemented by the device process-restoration check above.

## Acceptance evidence

### Implementation run: 2026-10-22

- Setup and foundation: `:app:assembleDebug :app:lintDebug` passed.
- US1 JVM tests: 8 assertions-based tests failed against the initial state-holder stub;
  the same 8 passed after state/recovery implementation.
- US1 connected tests: 6/6 passed on Pixel_8, Android 17 / API 37 emulator.
  File-backed Room tests cover close/reopen, concurrent single-profile creation, and
  rejected-write rollback. Compose tests cover invalid fields, saved-state input
  retention on write failure/retry, and loading/read-error versus setup.
- The initial Espresso 3.6.1 run failed against the API 37 input-injection implementation.
  Upgrading to stable AndroidX Test runner 1.7.0 / Espresso 3.7.0 resolved it.
- Actual task restoration: entered Mira/INR, pressed Home, identified PID 7014,
  terminated only that background PID using `run-as com.nestvest kill -9 7014`,
  opened Recents and selected NestVest. New PID 7580 resumed Mira/INR in editable
  setup with no saved-profile claim. No force-stop, task dismissal, or rotation used.
- With emulator Wi-Fi and mobile data disabled, explicit Create saved the real local
  profile and showed persisted name/currency. This is functional evidence, not a
  first-time-user timing study.
- API 26 runtime coverage, manual TalkBack, real OEM local migration,
  and first-time-user sample measurements have not yet been verified.
- US2: three of four appearance-state tests failed against no-op appearance intents;
  all 12 JVM tests and 9 connected tests passed after implementation. Selected/default
  and independently nullable appearance fields survive file-backed database reopen.
- Applied appearance restoration on API 37: selected Leaf/Sage with Mira/INR, pressed
  Home, terminated background PID 8324, then selected the task through Recents.
  New PID 8673 restored all four values in editable setup, with no success claim.
  Unavailable choices normalize to defaults with visible feedback; unit tests verify
  they never prevent otherwise valid setup.
- Backup policy: API 26-27 excludes all backup (no transport-flag support); API 28-30
  includes only database data when the OS advertises device-to-device transfer.
  API 31+ excludes cloud domains and allows database-only device transfer.
- US3: identity/schema tests and prior regressions passed (13 connected tests at this
  checkpoint). Three independent databases created distinct UUIDs for equal names.
  A controlled fixture retained UUID `78a18d43-a375-48e3-8cf2-e0e4d2aa4689` across
  repeated reads, attempted creation, and close/reopen. This is fixture evidence,
  not a claim of tested OEM migration.
- Final automated command passed:
  `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest :app:assembleRelease`
  with `ANDROID_HOME` pointing to the local SDK. Results: 12 JVM tests and 23
  connected tests passed, no failures or skipped tests. Device: Pixel_8 emulator,
  Android 17 / API 37, arm64. Debug APK and minified unsigned release APK built.
- Lint completed with 0 errors and 16 dependency/toolchain update warnings; pinned
  compatible versions are intentional. These warnings are not suppressed.
- Accessibility Test Framework checks passed for error, saved, and sheet states.
  Compose tests cover selected-state/error/progress semantics, 48 dp controls, and
  a 320 dp wide / 520 dp high setup with 2x text, RTL, and keyboard interaction.
  Computed Ink/Paper, white/Sage, and Ink/SoftSage contrast pairs meet 4.5:1.
  This does not substitute for manual TalkBack evaluation.
- Localization tests passed for English, French-device English fallback and localized
  currency labels, and generated en-XA/ar-XB pseudolocales with Unicode names.
- Privacy tests passed for credential-protected private database placement, no internet
  permission, cloud exclusions, and database-only local-transfer policy. Static inspection
  confirms base API 26-27 backup excludes every domain. Release manifest exports only
  the launcher and AndroidX's system-DUMP-permission-protected profile-install receiver;
  debug-only preview/test activities are absent from release.
- Additional persistence tests passed: malformed UUIDs and an unsupported schema return
  read errors, failed creation does not overwrite the existing record, and no destructive
  recreation occurs.

### Outstanding acceptance gates

Accessibility regression check (2026-10-23): the compact large-text RTL test now uses
the real ViewModel instead of a no-op intent handler, and asserts that typed name input
is retained. The old fixture discarded input and exposed an empty, partially clipped
field during the final screen-reader-label check. Accessibility checks remain enabled.
The focused test and full `:app:connectedDebugAndroidTest` run passed on Pixel_8 / API 37
(23 tests, no failures or skips).

| Task | Status and required evidence |
|---|---|
| T030 | Automated checks pass; manual TalkBack navigation/announcements still require a reviewer. |
| T033 | Full automated command passes on API 37; repeat on API 26 and complete visual comparison of all four native states with the mockups. |
| T034 | No first-time-user study conducted (sample size 0); SC-001 timing and SC-004 optional-appearance completion percentages are unmeasured. |

SC-002/SC-003/SC-005 have passing automated persistence/offline/failure-retry evidence
for the covered scenarios, plus a real offline minimal-creation check. They are not
claims about all devices or an unmeasured participant population. Keep the three tasks
above unchecked until their remaining evidence is collected. An API 26 device is not
currently available in this environment; only the installed API 37 emulator was used.

- SC-003 covers otherwise valid profile-creation flows working offline, not submissions
  with invalid fields or injected storage failures.
- SC-005 covers failed profile writes: every case must state creation did not complete,
  retain entered values, and offer retry. Field correction and profile-read recovery have
  separate tests.
- Record first-time-user sample size, timings, and completion results for SC-001/SC-004;
  do not replace those measurements with automated interaction timings.
- No draft-recovery guarantee applies to force-stop, task dismissal, reboot, or state
  not captured by Android. Existing successfully saved profiles must still reopen.

## Related design

- Entity and lifecycle: [data-model.md](./data-model.md)
- UI states and components: [contracts/profile-ui.md](./contracts/profile-ui.md)
- Mobile visual reference: [ui-mockups.html](./ui-mockups.html)
- Decisions and references: [research.md](./research.md)
- Requirements and acceptance scenarios: [spec.md](./spec.md)
