# Tasks: Phase 1 - Local Profile Foundation

**Input**: Design documents in `specs/001-local-profile-foundation/`.

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md),
[research.md](./research.md), [data-model.md](./data-model.md),
[UI contract](./contracts/profile-ui.md), [mockups](./ui-mockups.html), and
[quickstart.md](./quickstart.md).

**Tests**: Included to satisfy the explicit Compose-test requirements in the UI contract
and the constitution's quality gates, as detailed in the plan. Write story tests before
their implementation and demonstrate the relevant failing assertions, then make them pass.
An unresolved symbol or missing tool is not evidence of a behavioral failure.

**Organization**: One deliverable phase per story, preceded by shared setup and foundation.
All implementation paths below are repository-relative and planned, not existing app files.

## Format: `[ID] [P?] [Story] Description`

- `[P]` marks independent work within the stated dependency boundary, not permission to
  bypass prerequisites.
- `[US1]`, `[US2]`, and `[US3]` map to the prioritized stories in the specification.
- Leave checkboxes unchecked until the task's implementation and validation are complete.

## Path Conventions

- Android application: `app/src/main/java/com/nestvest/`.
- Local unit tests: `app/src/test/java/com/nestvest/`.
- Room/device/Compose tests: `app/src/androidTest/java/com/nestvest/`.
- Resources: `app/src/main/res/`. Feature documentation: this directory.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish a buildable native Android app without adding a backend.

- [ ] T001 Initialize the single Android app and checked-in Gradle wrapper in `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`, and `app/build.gradle.kts`; select and pin a compatible stable Kotlin/AGP/Gradle/JDK/SDK set, use API 26 as the minimum, record choices in `specs/001-local-profile-foundation/quickstart.md`, and keep machine-local SDK paths out of version control.
- [ ] T002 Configure Compose Material 3, lifecycle/ViewModel with saved-state support, Room with KSP schema export, coroutines, JUnit, coroutine test utilities, AndroidX test runner, Compose UI tests, and compatible accessibility checks in `gradle/libs.versions.toml` and `app/build.gradle.kts`; configure Android lint using the existing Android toolchain rather than adding an unrelated linter.
- [ ] T003 Create the minimal launchable application shell in `app/src/main/java/com/nestvest/NestVestApplication.kt`, `app/src/main/java/com/nestvest/MainActivity.kt`, `app/src/main/AndroidManifest.xml`, and `app/proguard-rules.pro`; verify `./gradlew :app:assembleDebug :app:lintDebug` succeeds before foundation work.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish common UI and local-data boundaries before story work.

- [ ] T004 [P] Define the flat monochrome/sage Material 3 theme in `app/src/main/java/com/nestvest/profile/ui/theme/Theme.kt`, `Color.kt`, and `Type.kt` within that directory; use theme roles, no default dynamic color, no decorative shadows or gradients, and contrast of at least 4.5:1 for normal text and 3:1 for large text.
- [ ] T005 [P] Configure local-only runtime and backup boundaries in `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/backup_rules.xml`, and `app/src/main/res/xml/data_extraction_rules.xml`; omit internet and unnecessary permissions, use app-private credential-protected storage, exclude profile data from cloud backup on legacy and modern Android, and permit only supported OS local migration without adding export/import or a device-bound database key.
- [ ] T006 Establish English default resources for all setup, validation, loading, error, privacy, and saved-status text in `app/src/main/res/values/strings.xml`, ensure English fallback for untranslated device languages, enable RTL in `app/src/main/AndroidManifest.xml`, and implement locally available currency codes with device-locale-aware labels in `app/src/main/java/com/nestvest/profile/domain/CurrencyCatalog.kt`; complete after T005 to avoid manifest conflicts, ship no additional translations, and do not copy the mockup's illustrative currency list as an unstated production limit.
- [ ] T007 [P] Define the pure Kotlin profile, creation draft, and repository result contracts in `app/src/main/java/com/nestvest/profile/domain/PrimaryProfile.kt`, `ProfileDraft.kt`, and `ProfileRepository.kt`; preserve these model constraints verbatim: `profileId` is "Random UUID generated once at creation; unique; immutable; primary key; preserved by any supported restore/migration", `name` is "Required; must contain at least one non-whitespace character", `preferredCurrency` is "Required; must be one of the app's locally available currency choices", `avatarId` is "Nullable; when present, must identify an available avatar", and `colorId` is "Nullable; when present, must identify an available color"; distinguish absence, read failure, write failure, and an existing profile.

For T007, `ProfileDraft` is the restorable UI value, not a Room entity or completed profile:
"Name and currency may be incomplete while editing; the PrimaryProfile validation rules
apply before creation." It holds entered name, selected currency code, and applied nullable
appearance identifiers with no generated persistent identity. The ViewModel owns its
Android saved-state representation; no durable draft table is introduced.

**Checkpoint**: T001-T007 complete. Native components, localized copy, local-only policies,
and domain contracts are available; no story may bypass this boundary.

---

## Phase 3: User Story 1 - Create a Local Primary Profile (Priority: P1) - MVP

**Goal**: Save a valid primary profile offline and show the same persisted profile on return,
without requiring optional appearance.

**Independent Test**: On a fresh offline installation, create a profile with only name and
currency, close/reopen the app, and verify the same single profile appears. Exercise invalid
inputs and read/write failures; no false success or second profile is allowed. Restore
unfinished setup after system-initiated background process termination through the
restored task, checking for a committed profile before offering draft retry (FR-010).

### Tests for User Story 1

- [ ] T008 [P] [US1] Add profile validation and MVI tests in `app/src/test/java/com/nestvest/profile/ProfileViewModelTest.kt` and saved-state tests in `app/src/test/java/com/nestvest/profile/ProfileRecoveryTest.kt` using a controllable fake repository; cover whitespace-only name, missing/invalid currency, valid minimal setup, read failure versus absence, save failure retaining the draft, retry, delayed saving, duplicate-submit prevention, and success only after repository confirmation; recreate a fresh ViewModel with captured saved state to verify draft restoration on confirmed absence, committed-profile precedence and draft clearing, read-error retry retaining draft values, and no automatic replay of interrupted saves.
- [ ] T009 [P] [US1] Add file-backed Room persistence tests in `app/src/androidTest/java/com/nestvest/profile/ProfilePersistenceTest.kt`; verify close/reopen retains values and UUID, concurrent or repeated creation leaves one primary profile, and failed writes do not expose a completed record; do not use a new in-memory database as a persistence test.
- [ ] T010 [P] [US1] Add UI contract tests in `app/src/androidTest/java/com/nestvest/profile/ProfileSetupTest.kt` for required-field errors, minimal offline setup, loading/read-error retry, persistent save-error feedback with retained values, disabled repeated submission while saving, and existing-profile rendering; include restored-task UI cases for draft versus committed profile and delayed/failed reconciliation, ensuring setup stays hidden until absence is confirmed; use injected failures rather than actual disk exhaustion.

### Implementation for User Story 1

- [ ] T011 [US1] Implement the Room entity, DAO, and database in `app/src/main/java/com/nestvest/profile/data/ProfileEntity.kt`, `ProfileDao.kt`, and `NestVestDatabase.kt`; retain all T007 field constraints, use the UUID as primary identity without auto-increment, enforce at most one primary profile transactionally even under concurrent creation, export the initial schema under `app/schemas/`, and never use destructive database recreation as a failure fallback.
- [ ] T012 [US1] Implement `app/src/main/java/com/nestvest/profile/data/RoomProfileRepository.kt` with local-only reads and atomic creation, one-time UUID generation for a new record, explicit typed read/write errors, cancellation propagation, and privacy-safe diagnostics without profile values; never convert a read failure to an absent profile or silently replace an existing record.
- [ ] T013 [US1] Implement intents and immutable loading/setup/saving/error/saved state in `app/src/main/java/com/nestvest/profile/ui/ProfileIntent.kt`, `ProfileUiState.kt`, and `ProfileViewModel.kt`; validate trimmed names and catalog currency codes, retain input through retry/configuration changes, and update `SavedStateHandle` with lightweight name, currency code, and applied nullable appearance identifiers as draft values change; on recreation read Room first, clear draft state only for a committed profile, restore editing only on confirmed absence, preserve the draft behind read retry on failure, never replay a transient Saving operation, prevent duplicate writes, and await persistence before success; add no durable draft table or draft-recovery guarantee after force-stop, task dismissal, reboot, or termination before state capture.
- [ ] T014 [US1] Implement Material 3 setup fields and the local currency picker in `app/src/main/java/com/nestvest/profile/ui/ProfileSetupScreen.kt` and `CurrencyPicker.kt`; follow mock 01 without a fixed phone frame, localize all copy, expose field errors and labels, keep touch targets at least 48 dp, and leave appearance optional with a default rendering until US2.
- [ ] T015 [P] [US1] Implement the confirmed saved/returning-profile view in `app/src/main/java/com/nestvest/profile/ui/SavedProfileScreen.kt` using mock 04's outlined-card hierarchy; render persisted name/currency/default appearance without exposing the UUID as a setup field or adding a dashboard or family invitation.
- [ ] T016 [US1] Implement the lifecycle-aware route in `app/src/main/java/com/nestvest/profile/ui/ProfileRoute.kt` for loading, read failure, setup, saving progress, and persistent save-error/retry states; announce errors/progress accessibly, retain input, and use truthful error copy rather than assuming low storage caused every failure.
- [ ] T017 [US1] Wire the repository, database, ViewModel, route, and theme through `app/src/main/java/com/nestvest/NestVestApplication.kt` and `MainActivity.kt` using a small explicit application container and a saved-state-aware ViewModel factory with owner-provided creation extras; ensure reopening reads the existing record, restored handles reach the ViewModel, and no composable performs database writes directly.
- [ ] T018 [US1] Run T008-T010 tests plus build/lint and document actual offline minimal setup, close/reopen, required-field errors, and injected read/write failure results in `specs/001-local-profile-foundation/quickstart.md`; verify FR-001, FR-002, FR-004, FR-006, FR-007, FR-010 and the default-appearance path without requiring US2; separately background the app until task state is captured, terminate only its background process without force-stop/task removal, restore via Recents, and record device/API, reproduction steps, resumed name/currency, and interrupted-save reconciliation evidence; rotation or Activity recreation alone is not process-death proof.

**Checkpoint**: US1 is a demonstrable MVP: offline setup and reliable local retrieval.
Basic UUID identity is already implemented here; US3 verifies its future-ready guarantees.

---

## Phase 4: User Story 2 - Personalize the Profile (Priority: P2)

**Goal**: Add optional built-in avatar/color selection without adding required setup steps.

**Independent Test**: Using US1's profile flow, compare skip/default and explicit appearance
selection, dismiss uncommitted changes, then save/reopen and verify the chosen appearance.

### Tests for User Story 2

- [ ] T019 [P] [US2] Add appearance state tests in `app/src/test/java/com/nestvest/profile/ProfileAppearanceTest.kt` for apply, skip to default, dismiss retaining the prior choice, name/currency retention, independent nullable avatar/color values, and unavailable options not blocking creation; recreate ViewModels with captured saved state to verify applied choices and skipped defaults resume while uncommitted sheet edits do not replace the previous selection.
- [ ] T020 [P] [US2] Add Compose bottom-sheet tests in `app/src/androidTest/java/com/nestvest/profile/ProfileAppearanceSheetTest.kt` for Choose/apply/skip/dismiss, named options, announced selected states, non-color selection indicators, and successful setup without opening the sheet.

### Implementation for User Story 2

- [ ] T021 [US2] Define bundled appearance choices in `app/src/main/java/com/nestvest/profile/domain/AppearanceCatalog.kt` and their names in `app/src/main/res/values/strings.xml`; enforce `avatarId` as "Nullable; when present, must identify an available avatar" and `colorId` as "Nullable; when present, must identify an available color", keeping unavailable selections optional with explicit user feedback and a usable default.
- [ ] T022 [P] [US2] Implement the mock 02 adaptation in `app/src/main/java/com/nestvest/profile/ui/ProfileAppearanceSheet.kt` as an optional Material 3 `ModalBottomSheet` with at least 48 dp labeled choices, preview, Use this look and Keep it simple actions; require no network, photo, or camera permissions.
- [ ] T023 [US2] Extend `app/src/main/java/com/nestvest/profile/ui/ProfileIntent.kt`, `ProfileUiState.kt`, and `ProfileViewModel.kt` with draft sheet state and apply/skip/dismiss behavior; applying updates appearance only and records applied identifiers in `SavedStateHandle`, skipping records null/default appearance, dismissal preserves the prior choice, and failures retain the chosen appearance; do not require uncommitted sheet edits to survive process termination.
- [ ] T024 [US2] Integrate the sheet with `app/src/main/java/com/nestvest/profile/ui/ProfileSetupScreen.kt` and `ProfileRoute.kt`, render persisted appearance in `SavedProfileScreen.kt`, and extend `app/src/androidTest/java/com/nestvest/profile/ProfilePersistenceTest.kt` to verify selected and null/default values survive database close/reopen.
- [ ] T025 [US2] Run appearance and US1 regression tests and document selected/default/unavailable-option paths in `specs/001-local-profile-foundation/quickstart.md`; repeat the background process-restoration check with applied avatar/color choices to verify FR-010 for appearance, demonstrate FR-003, and confirm personalization never becomes a required step.

**Checkpoint**: US2 adds optional recognition without changing US1's minimum setup.

---

## Phase 5: User Story 3 - Establish a Future-Ready Identity (Priority: P3)

**Goal**: Prove that the already persisted UUID is stable, distinct across independent
profiles, and retained when a supported external process restores the record.

**Independent Test**: Compare the UUID across repeated reopen and a controlled local
restore fixture; different freshly created installations must yield distinct identities.
No family circle, synchronization, backup UI, or app-managed transfer is implemented.

### Tests for User Story 3

- [ ] T026 [P] [US3] Add identity continuity tests in `app/src/androidTest/java/com/nestvest/profile/ProfileIdentityTest.kt`; use independently created local databases to verify distinct new identities, repeated reads/reopens retain identity, equal names do not imply equal identities, and a known restored profile UUID is read unchanged rather than regenerated.
- [ ] T027 [P] [US3] Add schema-contract tests in `app/src/androidTest/java/com/nestvest/profile/ProfileSchemaTest.kt` against `app/schemas/`; verify UUID identity is non-auto-increment, unique, independent of display attributes, and separate from the installation's one-primary-profile enforcement.

### Implementation for User Story 3

- [ ] T028 [US3] Verify and, only where T026-T027 expose gaps, correct `app/src/main/java/com/nestvest/profile/data/RoomProfileRepository.kt`, `ProfileEntity.kt`, and `ProfileDao.kt` so existing/restored records retain their UUID and fresh creation does not derive identity from mutable values; keep solo operation valid without family/device linkage.
- [ ] T029 [US3] Run identity/schema and prior story regression tests, then document controlled restore-fixture results and any available supported OS local-migration check in `specs/001-local-profile-foundation/quickstart.md`; explicitly distinguish fixture proof from unverified OEM migration and do not add a transfer product feature.

**Checkpoint**: FR-005 is validated without expanding Phase 1's transfer scope.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validate the integrated feature against the constitution, UI reference, and
measurable outcomes. Do not mark a task complete when its required evidence is unavailable.

- [ ] T030 [P] Add and run layout/accessibility tests in `app/src/androidTest/java/com/nestvest/profile/ProfileAccessibilityTest.kt` for setup, sheet, error, and saved states; verify 48 dp controls, selected-state/error/progress semantics, compact width, large system text, keyboard insets, RTL, and no clipped required actions, then record manual TalkBack and theme contrast results in `specs/001-local-profile-foundation/quickstart.md`.
- [ ] T031 [P] Validate English default resources and currency presentation in `app/src/androidTest/java/com/nestvest/profile/ProfileLocalizationTest.kt` using English, a non-English device locale, expanded-text pseudolocale, and RTL pseudolocale; verify English fallback for untranslated languages, locale-aware currency presentation where supported, Unicode names, unchanged stored currency codes, and complete labels/messages in `app/src/main/res/values/strings.xml`; English is the only shipped interface language, and pseudolocales validate readiness rather than count as translations.
- [ ] T032 [P] Add and run local-storage/backup policy checks in `app/src/androidTest/java/com/nestvest/profile/ProfilePrivacyTest.kt` and inspect the merged `app/src/main/AndroidManifest.xml` plus `app/src/main/res/xml/backup_rules.xml` and `data_extraction_rules.xml` within that directory; confirm private database placement, platform protection assumptions, cloud exclusions on API 26-30 and API 31+, no internet/analytics runtime dependencies, and no accidental exported data components.
- [ ] T033 Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest`, compare all four states with `specs/001-local-profile-foundation/ui-mockups.html`, and record commands, device/API coverage, pass/fail counts, and remaining blockers in `specs/001-local-profile-foundation/quickstart.md`; test on API 26 and a current supported API and do not treat the HTML demo as implementation evidence.
- [ ] T034 Validate SC-001 through SC-005 using a documented first-time-user sample and scripted lifecycle/failure cases in `specs/001-local-profile-foundation/quickstart.md`; report sample size and measured completion time, require at least 90% unassisted setup under two minutes, 100% of successfully saved test profiles retained on reopen, offline completion of 100% of otherwise valid creation flows, at least 95% completion without appearance, and a clear not-created message with retained values and retry for 100% of injected profile-write failures; evaluate invalid-field correction and profile-read recovery separately rather than counting them as SC-003/SC-005 failures, and report unmet criteria instead of substituting proxy metrics.
- [ ] T035 Update `README.md` with Android prerequisites, build/run instructions, Phase 1 scope, and links to `specs/001-local-profile-foundation/quickstart.md` and `ui-mockups.html` within that directory; include the local-only storage model and absence of app-managed backup/export without claiming stronger data recovery guarantees.

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Setup T001 -> T002 -> T003
                       |
Foundation T004, T005 -> T006, T007 (independent branches after setup)
                       |
                     US1
                    /   \
                  US2   US3
                    \   /
             Cross-cutting validation
```

US2 and US3 depend on US1, not on each other. They are independently verifiable extensions
of the same foundation, not standalone applications. Their integration edits overlap;
parallel work is limited to the non-overlapping tasks listed below.

### User Story Dependencies

| Story | Prerequisites | Internal execution |
|---|---|---|
| US1 | T001-T007 | T008-T010 first; T011 -> T012 -> T013; T014 after T013; T015 can accompany T014; T016 after T014-T015; T017 -> T018 |
| US2 | US1 checkpoint | T019-T020 first; T021 -> T022; T023 after T021; T024 after T022-T023; T025 last |
| US3 | US1 checkpoint | T026-T027 first; T028 -> T029 |

T007 is the common contract for story tests. T011 is required by persistence-test execution;
test authoring can precede it, but behavioral failure must be confirmed once the tested
interfaces exist. T012 and T013 must satisfy repository/state assertions before UI integration.
US2's T024 and US3's T028 both may touch persistence files: serialize those edits.
T025 and T029 both update quickstart documentation: serialize those writes as well.

### Within Each User Story

- Review the linked requirements before starting; keep model constraints unchanged.
- Write tests first, implement the smallest complete behavior, and run the focused tests.
- Implement models/storage before repository/state logic, then UI and application wiring.
- Run the story's checkpoint before dependent work; do not defer core persistence or error
  handling to polish.
- Do not implement expense entry, financial dashboards, cloud services, family membership,
  synchronization, or app-managed transfer.

### Parallel Opportunities

- After T003: T004, T005, and T007 can run independently; T006 follows T005 because
  both update the manifest.
- US1: T008-T010 are separate test files; after T013, T014 and T015 use different files.
- US2: after T021, T022 and T023 can proceed together; sheet tests use separate files.
- US3: T026 and T027 are separate tests and can be authored together after US1.
- T030-T032 have separate test files, but their quickstart evidence and any shared resource
  corrections must be integrated serially. T033 follows all feature/test corrections;
  T034 follows T033; T035 follows final validation documentation.

---

## Parallel Example: User Story 1

After the foundational checkpoint, author T008, T009, and T010 in their distinct test files.
After T013, implement T014's setup/currency components alongside T015's saved-profile view.
Join both at T016; never run competing edits to the ViewModel.

## Parallel Example: User Story 2

After US1, author T019 and T020 together. Once T021 defines appearance identifiers, build
T022's bottom sheet alongside T023's state logic; integrate them in T024.

## Parallel Example: User Story 3

After US1, author T026's identity tests and T027's schema tests together. Run them against
the existing implementation before making any narrowly required T028 corrections.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Setup and Foundation.
2. Complete US1 through T018 and demonstrate name/currency-only setup offline.
3. Confirm persistence, error/retry behavior, and no duplicate profile on reopen.
4. Run applicable cross-cutting checks before declaring any MVP release ready.

### Incremental Delivery

1. US1 supplies the useful minimum with default appearance and stable identity.
2. US2 adds optional personalization without changing minimum input requirements.
3. US3 strengthens evidence for identity continuity; it does not introduce new transfer UI.
4. Complete cross-cutting accessibility, localization, privacy, performance, and documented
   verification before considering Phase 1 complete.

### Parallel Team Strategy

Use separate ownership only for marked tasks after their prerequisites. Prefer sequential
story integration because the application is small and shared state/storage files overlap.
One integrator owns quickstart results and shared-resource changes.

## Notes

- **Requirement coverage**: FR-001/002/004/006/007: US1; FR-003: US1 default path plus US2;
  FR-005: US1 identity foundation plus US3; FR-008: T004/T006, story UI tasks, T030/T031;
  FR-009: T005, T011/T012, and T032; FR-010: T008/T010/T013/T017/T018 and
  T019/T023/T025 for applied appearance. SC-001 through SC-005: T018/T025/T029 and T033/T034.
- The UI contract's loading/read-error states belong to US1, not to a later feature.
- Nullable appearance fields are available from the first profile schema even before
  optional selection UI ships. The restorable draft is saved UI state, not a new Room entity.
- FR-010 basic recovery is part of the US1 checkpoint; US2 extends it to applied appearance.
  The existing story dependencies and parallel examples still apply: T008 owns both unit
  test files, and T013/T023 edit shared state files sequentially.
- English-only delivery and English fallback are explicit. No task requires additional
  shipped translations; non-English device locales test fallback and currency behavior.
- Execution evidence must be real. Missing SDKs, test devices, or participant
  measurements are blockers to the corresponding validation task, not automatic passes.
