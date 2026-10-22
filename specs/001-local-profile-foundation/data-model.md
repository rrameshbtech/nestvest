# Data Model: Phase 1 — Local Profile Foundation

## PrimaryProfile

Represents the single primary user identity for one app installation.

| Field | Meaning | Requirements |
|---|---|---|
| `profileId` | Stable identity for this profile | Random UUID generated once at creation; unique; immutable; primary key; preserved by any supported restore/migration |
| `name` | User's display name | Required; must contain at least one non-whitespace character |
| `preferredCurrency` | Currency selected for financial presentation | Required; must be one of the app's locally available currency choices |
| `avatarId` | Optional identifier for a built-in avatar | Nullable; when present, must identify an available avatar |
| `colorId` | Optional identifier for a profile color | Nullable; when present, must identify an available color |

The entity is stored locally in app-private storage. Phase 1 supports at most one
`PrimaryProfile` per installation. No email address, cloud account, family membership,
or remote identifier is part of this entity.

## Relationships

- A `PrimaryProfile` has one `profileId`.
- Later family-circle records may reference `profileId`; Phase 1 creates no such records.
- Expenses, assets, liabilities, and other financial records are outside this phase.

## Restorable Setup Draft

A lightweight UI-state value, not a completed profile or a new Room entity. It contains
the entered name, selected currency code, and applied nullable avatar/color identifiers.
Name and currency may be incomplete while editing; the PrimaryProfile validation rules
apply before creation. Keep this value in ViewModel `SavedStateHandle`, separate from the
durable profile and with no generated persistent identity.

Restore it only through an Android system-restored task. Unapplied sheet choices are not
part of the recovery guarantee. There is no durable draft table or recovery promise after
force-stop, task dismissal, reboot, or termination before Android captures task state.

## Lifecycle

1. **Absent**: No primary profile exists; profile setup is available.
2. **Editing**: User enters required values and may select optional appearance.
3. **Saving**: The app validates input and attempts a local database write.
4. **Created**: The profile is persisted and its identity is stable.
5. **Save failed**: No success is shown; entered form values remain available for retry.

A profile ID is generated once as part of a successful creation operation and persisted
with the profile. Repeated setup or application restart must not create a second primary
profile. Failed persistence must not leave a profile that the UI reports as created.

After system-initiated process recreation, read the database before selecting a UI state:
a committed profile wins and clears the restored draft; confirmed absence permits draft
editing/retry; a read error retains the draft but exposes only read retry. Do not replay
an in-flight save automatically or present a restored draft as a completed record.

## Validation and Integrity

- Trim surrounding whitespace from the name for validation; reject empty or
  whitespace-only values.
- Require a valid locally available currency selection.
- Avatar and color are optional; unavailable values must not block profile creation.
- Enforce uniqueness of `profileId` and the one-primary-profile-per-installation invariant.
- The stored UUID must never be recomputed from mutable profile fields.
- Keep profile data in app-private local storage; cloud backup and internet transmission
  are prohibited.
- When a supported external device migration restores the profile, retain its stored UUID.
  Implementing transfer itself is outside Phase 1.
