# Quickstart: Phase 1 — Local Profile Foundation

This guide validates the profile setup feature after the Android application module is
available. It is a verification guide, not an implementation recipe.

## Prerequisites

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

Expected outcome: unit tests pass for profile validation, identity uniqueness,
persistence, ViewModel state transitions, saved-state recovery, and save failures;
connected tests pass for profile creation and accessible Compose interactions.

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
