# Data Model: Phase 3 — Dashboard Navigation

The dashboard uses the existing app-private Room database and current expense records.
It adds only a local preference; summaries are transient values derived from existing
expenses. No dashboard data is sent to an external service.

## Dashboard Option

A catalog entry representing an actionable destination available in this app version.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `optionId` | Stable string identifier | Yes | Unique within the app catalog; not derived from a localized label |
| `title` | Localized display string | Yes | Resolved from app resources for the current locale |
| `description` | Localized display string | No | Concise description; may be omitted when it adds no value |
| `destination` | App navigation destination | Yes | Must route to an available screen; unavailable destinations are not opened |
| `available` | Boolean | Yes | Derived from the modules/actions available in this app version |

Initial catalog:

| `optionId` | Display name | Destination | Initial pin |
|---|---|---|---:|
| `expense_capture` | Add expense | Existing amount-first capture screen | Yes |
| `expense_history` | Expenses | Existing expense history screen | Yes |
| `reminders` | Reminders | Existing reminder settings screen | No |

Future-phase planning, wealth, and family destinations are not included until they are
available in the app.

## Dashboard Preference

The single local user's saved dashboard customization.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `preferenceId` | Fixed singleton key | Yes | Identifies the one preference row for the local primary profile |
| `pinnedOptionIds` | Collection of stable option IDs | Yes | Contains only catalog IDs; initial default is `expense_capture`, `expense_history`; unpinned available options remain under More |

**Lifecycle**:

1. If no saved preference exists, use the default pinned set from the option catalog.
2. Opening Customize copies the current set into an editable draft.
3. Pin/unpin changes affect only the draft until the user confirms Save choices.
4. On successful persistence, replace the saved preference and reflect it on the
   dashboard.
5. On failure, retain the prior saved preference, remain in the customization flow,
   and communicate that the change was not saved.
6. On app restart, load the saved preference. Catalog options added in a later app
   version remain available under More unless the user pins them. Invalid/unavailable
   identifiers must never produce a broken destination.

The preference is device-local and is not part of household financial sync.

## Dashboard Summary

A transient view model value calculated from the existing Expense records. It is not
persisted.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `state` | Loading / Available today / No expenses today / No expense history / Unavailable | Yes | Distinguishes read-in-progress, current-day activity, no current-day activity with prior history, no saved expenses at all, and failed reads |
| `localDate` | Local calendar date | When available | Summary covers the current device-local day |
| `expenseCount` | Non-negative integer | When records exist | Number of saved expenses in the current day |
| `totalMinorUnits` | Non-negative integer | When records exist | Sum of exact amount minor units; do not use floating-point arithmetic |
| `currencyCode` | ISO 4217 code | When records exist | Use the saved expense/profile currency and locale-aware display formatting |

**Display rules**:

- `No expense history` is not displayed as a zero total.
- If prior expenses exist but none were recorded today, a zero-today summary may be
  shown as zero activity for the current local day; it must not imply the user has
  never recorded expenses.
- When expenses exist today, derive the count and total from their exact positive
  amounts.
- If summary reading fails, show a localized unavailable message and keep the Expenses
  destination available.
- Recompute from the current local expense data when the dashboard is shown or resumed,
  and when the current local date changes. Do not show a value from a previous day or
  stale failed read as current.
- Existing Phase 2 expenses use the primary profile currency; if unexpected records
  contain different currencies, do not combine them as if they were equivalent.

## Persistence and Migration

- Increment the existing `ProfileDatabase` from schema version 2 to version 3.
- Add the dashboard preference entity and DAO using the existing Room conventions.
- Use an additive migration that leaves profile, expense, and reminder rows unchanged.
- Do not add a network permission, cloud dependency, analytics, or destructive migration
  fallback.
- Export the Room schema and test migration from version 2, persistence across database
  reopen, and failed preference writes.
