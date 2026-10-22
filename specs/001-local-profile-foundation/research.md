# Research: Phase 1 — Local Profile Foundation

## Decisions

### Android app architecture

- **Decision**: Use the requested Kotlin, Jetpack Compose, Room/SQLite, and MVI-style
  unidirectional data flow. Keep immutable screen state in a ViewModel, expose it through
  `StateFlow`, send user actions as intents/events, and use a repository backed by Room
  for profile persistence.
- **Rationale**: This follows Android's recommended UI architecture and makes saving,
  validation, and error states observable and testable. The repository remains the local
  source of truth.
- **Alternatives considered**: A separate MVI framework or a custom multi-layer state
  management library. Neither is needed for one profile flow; standard Android lifecycle,
  Compose, and coroutine APIs suffice.

### Native Material UI and mockup fidelity

- **Decision**: Recommend Material 3 for Jetpack Compose
  (`androidx.compose.material3:material3`) and adapt the
  [HTML mock screens](./ui-mockups.html) to native components through the
  [profile UI contract](./contracts/profile-ui.md).
- **Rationale**: This is the Material Design implementation for the selected Kotlin and
  Compose stack. Its theme roles and components provide consistent controls while
  permitting the flat, restrained NestVest style. Standard components still require
  explicit accessibility, localization, and layout verification.
- **Alternatives considered**: React MUI (a web library, incompatible with the native
  Compose choice without adding a separate runtime), rendering the mockup in a WebView,
  and building custom equivalents for all controls. None is needed for this flow.
- **Adaptation**: Keep the four mockup states, but present optional personalization in
  a Material bottom sheet. Use a custom Material theme with the reference palette rather
  than default dynamic color. Do not copy desktop gallery framing, decorative system
  bars, simulated saves, or the illustrative currency list into production behavior.
- **Research scope**: This amendment resolves the UI-library choice only; it retains
  the earlier persistence, identity, and migration decisions.

### Profile identity and persistence

- **Decision**: Generate a random UUID once when the primary profile is created and store
  it as the profile's non-auto-increment identity. Preserve it on reads and supported OS
  migration; never regenerate it during restoration.
- **Rationale**: UUIDs are globally unique without a central service and do not depend on
  mutable profile values such as name or currency. This supports future family association
  and the PRD's merge-ready identity requirement.
- **Alternatives considered**: Auto-incrementing database identifiers (not safe across
  devices) and hashes of profile details (mutable and potentially identifying).

### Local data protection and migration

- **Decision**: Keep the Phase 1 profile database in app-private internal storage and rely
  on Android app isolation and platform device storage encryption. Exclude profile data
  from cloud backup while leaving supported local device-to-device migration available.
  Do not add app-managed backup, export/import, or synchronization in this phase.
- **Rationale**: This meets the local-only requirement and supports supported OS-level
  migration without introducing a custom transfer flow. A database key sealed to one
  device's Android Keystore would not automatically be usable after migration; adding
  SQLCipher without a deliberate key-transfer design could therefore make migrated data
  unreadable. Revisit database-level encryption before storing more sensitive financial
  records than this phase's profile metadata.
- **Alternatives considered**: Disable all backup/transfer, which would preclude OS local
  migration; add encrypted app-managed export/import, which is explicitly outside the
  agreed Phase 1 scope; encrypt the database with a device-bound Keystore key, which
  requires a migration/key-rotation design.
- **Boundary**: "Preserve identity on restore/transfer" means that if a supported external
  process successfully restores the profile, the persisted UUID remains the same. Phase 1
  does not implement or promise a transfer service.

### Localization and accessibility

- **Decision**: Put user-visible strings in Android localization resources, use locale-aware
  display for currency, and use Compose semantics and accessible control labels. Ship
  English in default resources and fall back to it for untranslated device languages.
  Validate a non-English device locale for fallback/currency behavior, expanded-text and
  RTL pseudolocales for layout readiness, and TalkBack operation.
- **Rationale**: This is aligned with the constitution and supports translated labels,
  assistive technology, and locale-specific presentation without a custom framework.
- **Alternatives considered**: Hard-coded strings or a third-party localization and
  accessibility layer; both add unnecessary barriers or dependencies. Shipping additional
  translations is outside the clarified Phase 1 scope; pseudolocales are not translations.

### System-initiated interruption recovery

- **Decision**: Use ViewModel `SavedStateHandle` for lightweight name, currency code, and
  applied avatar/color identifiers. Recreate the state holder with saved-state-aware
  ViewModel construction, including when using the explicit application container.
- **Rationale**: ViewModel alone covers configuration changes, not process death.
  Android saved state supports returning through a system-restored task and matches the
  agreed recovery boundary without introducing a draft database or backup feature.
- **Reconciliation**: Always read Room before presenting restored setup. Show a committed
  profile and clear the draft, restore the draft only on confirmed absence, and retain it
  behind retry when reading fails. Never restore a transient Saving flag as an operation
  to replay; retry is explicit and uses atomic single-profile creation.
- **Limitations**: Task saved state is not guaranteed after force-stop, task dismissal,
  reboot, or before Android captures state. Only applied appearance choices must resume.
- **Alternatives considered**: ViewModel-only draft storage loses process-death recovery;
  a durable Room draft table would add recovery beyond the user's selected scope.

### Verification

- **Decision**: Cover the profile repository and uniqueness constraints with Room tests;
  cover ViewModel intents and state transitions with coroutine tests; cover setup flows
  with Compose UI tests; add accessibility checks and manual TalkBack verification.
- **Recovery validation**: Test fresh ViewModels with restored handles for committed,
  absent, and read-failure cases. Separately verify background process termination and
  task restoration on a device; Activity recreation or rotation is insufficient evidence.
- **Acceptance populations**: Validate otherwise valid offline flows for SC-003 and
  failed saves for SC-005; keep field-validation and read-error scenarios distinct.
- **Rationale**: Tests map directly to the acceptance criteria and separate persistence,
  state, and interaction failures.
- **Alternatives considered**: End-to-end UI tests alone, which provide weaker diagnosis
  and do not adequately exercise failure cases or database uniqueness.

## References

- Saved state for ViewModel and system-initiated process death:
  https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate
- Material 3 in Compose (native component dependency and theming):
  https://developer.android.com/develop/ui/compose/designsystems/material3
- Android UI architecture and unidirectional data flow:
  https://developer.android.com/topic/architecture/ui-layer
- Android Auto Backup and data extraction rules:
  https://developer.android.com/identity/data/autobackup
- Android backup/restore testing:
  https://developer.android.com/identity/data/testingbackup
- Android Keystore security:
  https://developer.android.com/privacy-and-security/keystore
- Compose accessibility testing:
  https://developer.android.com/develop/ui/compose/accessibility/testing
- Room database testing:
  https://developer.android.com/training/data-storage/room/testing-db
- Android pseudolocales:
  https://developer.android.com/guide/topics/resources/pseudolocales
- NestVest product requirements: `prd.md`, Phase 1 and Rules 1, 3, and 5.
- Feature requirements: `specs/001-local-profile-foundation/spec.md`.
