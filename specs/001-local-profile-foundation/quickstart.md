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
persistence, ViewModel state transitions, and save failures; connected tests pass for
profile creation and accessible Compose interactions.

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
5. **Locale and accessibility**: Run with a supported non-default locale and a
   pseudolocale, then navigate setup with TalkBack. Confirm labels, currency presentation,
   focus order, and action names remain understandable and operable.
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

## Related design

- Entity and lifecycle: [data-model.md](./data-model.md)
- UI states and components: [contracts/profile-ui.md](./contracts/profile-ui.md)
- Mobile visual reference: [ui-mockups.html](./ui-mockups.html)
- Decisions and references: [research.md](./research.md)
- Requirements and acceptance scenarios: [spec.md](./spec.md)
