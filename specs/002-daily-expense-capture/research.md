# Research: Phase 2 — Ultra-Fast Daily Expense Capture

## Decisions

### 1. Keep expense persistence in the existing Room database

**Decision**: Add expense and reminder-preference entities/DAOs to the existing
`ProfileDatabase`, migrate the database from schema version 1 to 2 with Room's
non-destructive additive auto-migration, and export the new schema.

**Rationale**: The app already has a single app-private `nestvest.db`, Room setup,
schema export, UUID primary-key convention, sealed repository results, and file-backed
migration/persistence tests. Reusing it keeps profile and expense data under the same
local storage boundary and avoids introducing another database.

**Alternatives considered**: A separate expense database was rejected because there is
no independent lifecycle or storage boundary for expenses. Manual SQL migration is not
needed for adding new tables without changing the profile schema. Destructive migration
was rejected because it could erase the existing profile and would violate data
durability.

### 2. Store exact currency minor units and a per-expense currency code

**Decision**: Store amount as a positive `Long` number of currency minor units and store
the ISO 4217 currency code on each expense. Resolve supported fraction digits from
`java.util.Currency`; parse and validate before persistence, reject excess precision and
overflow beyond `Long.MAX_VALUE` minor units, and format using the current locale.

**Rationale**: Integer minor units avoid floating-point currency errors and work with
SQLite integer storage and period totals. A currency code on each record preserves its
meaning if a profile's preferred currency changes later. The existing profile
`CurrencyCatalog` already derives fraction digits from `Currency.defaultFractionDigits`.

**Alternatives considered**: `Float`/`Double` were rejected due to rounding error.
`BigDecimal` plus a Room converter was rejected because the persisted string would add
conversion/comparison complexity without improving the amount-entry contract.

### 3. Use random UUID expense identity

**Decision**: Generate a UUID once when an expense is created, store its string as a
non-auto-generated primary key, and never derive or change it during amount/detail
edits.

**Rationale**: This matches Phase 1's profile identity convention and gives distinct
expenses unique identities even when amount and timestamp match, without coordination
across devices.

**Alternatives considered**: Timestamp/amount-derived IDs and auto-increment IDs were
rejected because they can collide across devices or change meaning when a record is
edited.

### 4. Persist immutable creator-profile provenance

**Decision**: Store a required `createdByProfileId` UUID string on every expense,
initialized from the Phase 1 primary profile ID at creation. Keep it immutable for
amount/detail edits and future sync imports. Do not add a foreign key to the local
profile row.

**Rationale**: [prd.md](../../prd.md) defines Phase 1's Primary User, anticipates later
Family Circle identity, and introduces trusted household sync in Phase 6. Recording the
profile ID now preserves who created each record without adding family membership or
sync to Phase 2. A foreign key could prevent a future receiving device from storing an
expense before it has resolved the source member locally.

**Alternatives considered**: Storing only the creator's display name was rejected
because names can change or collide and cannot robustly identify a trusted sync member.
Reassigning imported expenses to the receiving device's primary user was rejected
because it destroys provenance.

**Cross-feature dependency**: Phase 6 must preserve `createdByProfileId` in sync
payloads, resolve it through trusted household membership, and reconcile the PRD's
functional-key examples with stable UUID identity and explicit repeated transactions.
Creator attribution is not a duplicate-detection or conflict-resolution algorithm.
Phase 4 ingestion must add a distinct source/provenance representation for parsed versus
user-confirmed transactions; `createdByProfileId` alone does not establish manual entry
or confirmation. Phase 3 planning must consume the expense categories consistently.

### 5. Store optional enrichment on the expense row and update one field at a time

**Decision**: Store nullable vendor, Kakeibo category, and receipt filename columns on
the expense row. Use focused repository/DAO operations per detail. Trim the vendor and
commit it when the field loses focus or the user explicitly leaves the detail editor;
commit category selection immediately; commit receipt state only after local file copy
succeeds.

**Rationale**: The summary card needs these details with the amount and timestamp, and
each detail has a separate confirmation lifecycle. Single-field updates prevent one
unfinished form field from overwriting another confirmed detail. The confirmation
behavior also matches the mockup's vendor hint, category controls, and receipt action.

**Alternatives considered**: A separate 1:1 details table was rejected for this phase
because details do not have an independent lifecycle or history. A single final
save-all form was rejected because users must be able to stop after any confirmed
detail. A vendor Save button was rejected to keep the path seamless.

### 6. Copy receipt images into app-private files

**Decision**: Use the system image picker without broad media permission, copy the
selected image to app-private internal storage, and store only a relative local filename
in Room. Remove the local image when its expense is deleted.

**Rationale**: A picker-provided content URI depends on another provider and can be
unreadable later or on a future trusted device. A private copy keeps receipt bytes inside
the app's local data boundary and gives a future household transfer a portable file
payload. Copy/persistence failure must leave the expense and other details unchanged.

**Alternatives considered**: Persisting a `content://` URI requires persistent read
grants and remains tied to a provider on the source device. Storing image bytes directly
in SQLite was rejected because a file reference is simpler for potentially large data.
No broad gallery permission is needed.

### 7. Use local-calendar half-open period ranges

**Decision**: Query using `[startInclusive, endExclusive)` instants computed from local
calendar dates and the device locale's first weekday. Default to today. Show time only
for today and date plus time for week/month views.

**Rationale**: This gives exact boundary behavior across daylight-saving changes and
respects locale-specific week starts instead of assuming a rolling seven days or a
Monday-first week. It follows the intent of the mockup while correcting its
Monday-hardcoded sample logic.

**Alternatives considered**: Rolling 24-hour/7-day/30-day ranges and a hardcoded
Monday-start week were rejected because they do not match calendar period labels for all
locales.

### 8. Schedule optional reminders as inexact local work

**Decision**: Use a persisted, unique one-time WorkManager chain to schedule each
reminder near the selected local time, calculating the next occurrence from the current
local date/time after each run. Keep it off by default. Persist the date of the last
delivery and claim the current local date before posting so retries cannot send a second
reminder that day. Request Android 13+ notification permission only after the user
enables reminders; persist that the prompt was already shown, and direct a denied user
to system settings rather than repeatedly prompting. If permission is denied or
revoked, stop scheduling and keep capture usable. Re-check permission when the app
resumes.

**Rationale**: A gentle reminder is not a precise alarm. Inexact background work avoids
the special exact-alarm permission and its added system-settings friction. Unique work
avoids duplicate schedules; persisted work survives process restart and reboot. A
persisted local-date guard provides at-most-once delivery even if work is retried.
Android notification denial or a system-level notification disable must not block core
expense use.

**Alternatives considered**: Exact `AlarmManager` scheduling was rejected because it
requires additional special access on recent Android versions and conflicts with a
non-intrusive, optional reminder. WorkManager has less precise execution timing and
may be delayed by battery policy; the user setting is an approximate local time, not a
guaranteed exact delivery time.

### 9. Keep screen state and interactions aligned with the mockup

**Decision**: Use the existing Compose/Material 3, ViewModel, and lifecycle-aware
StateFlow conventions. Model amount entry as accessible custom keypad input, keep
selected expense ID and period in screen state, expose persistent summary data while
enriching, and navigate to reminder preferences from the history settings icon.

**Rationale**: This matches the four screens and their demonstrated interactions, while
keeping business state outside view-local state. Compose semantics and explicit
selected-state roles support assistive technology.

**Alternatives considered**: A webview/HTML implementation was rejected; the mockup is
visual/behavioral reference only. Relying only on an IME keypad was rejected as the
primary capture path because active IMEs vary in locale decimal-key behavior.

## Evidence and References

### Repository evidence

- `app/src/main/java/com/nestvest/profile/data/ProfileEntity.kt`: string UUID primary
  key and domain validation convention.
- `app/src/main/java/com/nestvest/profile/data/ProfileDatabase.kt`: Room version 1,
  app-private database, and schema export.
- `app/src/main/java/com/nestvest/profile/data/RoomProfileRepository.kt`: transaction,
  storage-error mapping, and cancellation handling.
- `app/src/main/java/com/nestvest/profile/domain/CurrencyCatalog.kt`: currency
  fraction digits are based on `java.util.Currency.defaultFractionDigits`.
- `specs/001-local-profile-foundation/data-model.md`: the Phase 1 primary profile UUID
  is stable and intentionally available for later family-circle references.
- `app/src/androidTest/java/com/nestvest/profile/ProfileSchemaTest.kt` and
  `ProfilePersistenceTest.kt`: file-backed Room validation and schema test patterns.
- `spec.md` and `ui-mockups.html`: confirmed requirements, period interactions, and
  four-screen UI behavior.

### Android platform guidance

- [Room database migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions):
  preserve stored user data with explicit/verified migrations; do not use destructive
  fallback for this feature.
- [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input)
  and [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics):
  constrain amount input and expose clear accessible labels/state for keypad controls.
- [Compose focus](https://developer.android.com/develop/ui/compose/touch-input/focus/request-focus):
  use focus changes to confirm vendor edits, and confirm explicitly when leaving the
  detail screen so back navigation does not lose a deliberate completed edit.
- [Compose state saving](https://developer.android.com/develop/ui/compose/state-saving):
  retain lightweight UI state across recreation; keep records in Room and keep only
  selected IDs/period/draft input in saved UI state.
- [WeekFields API](https://developer.android.com/reference/java/time/temporal/WeekFields):
  derive first weekday from the device locale rather than hardcoding a week boundary.
- [Android alarms](https://developer.android.com/develop/background-work/services/alarms):
  exact alarms require additional permission; a user-selected gentle reminder does not
  warrant exact-alarm access.
- [WorkManager periodic work](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work):
  persistent inexact work is suitable for non-critical reminder delivery; execution can
  be delayed by OS constraints.
- [Managing WorkManager work](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work):
  unique work prevents multiple active schedules for the same reminder chain.
- [Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission):
  Android 13+ requires runtime permission; request only from the explicit reminder
  enable action and handle denial without blocking capture.
- [Photo picker](https://developer.android.com/training/data-storage/shared/photopicker):
  select an image without broad media-library permissions; copy selected content to
  local app storage for durable local-only handling.
- [NestVest product requirements](../../prd.md): Phase 1 establishes the Primary User;
  Phase 6 introduces trusted Family Circle identity and P2P sync. The PRD also calls
  for functional unique keys; Phase 6 must reconcile those examples with the
  constitution's stable device-independent IDs and preserve intentional same-amount
  expenses as distinct records.

## Remaining Validation Notes

- Verify how the system image picker behaves on the supported API range and ensure
  selected image copying handles revoked/unavailable source URIs explicitly.
- Exercise weekly boundaries under locales whose week begins on Sunday and Monday.
- Validate the chosen inexact reminder schedule under Doze and on representative OEM
  battery settings. Do not promise exact delivery time.
- A future synchronization feature must define deletion tombstones before cross-device
  merge; Phase 2 uses confirmed local deletion without retaining removed receipt files.
