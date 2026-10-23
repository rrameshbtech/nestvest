# Data Model: Phase 2 — Ultra-Fast Daily Expense Capture

This model extends the existing app-private Room database. It must preserve the
version-1 primary profile and add no network, analytics, or app-managed export path.

## Expense

One explicitly saved manual outflow.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `expenseId` | UUID string | Yes | Stable primary key; generated once per new expense; never derived from amount or timestamp |
| `amountMinorUnits` | Signed 64-bit integer | Yes | Must be greater than zero; stores exact currency minor units without floating-point rounding |
| `currencyCode` | ISO 4217 code | Yes | Captured from the primary profile at creation; immutable for ordinary detail edits |
| `capturedAtEpochMillis` | Signed 64-bit integer | Yes | Capture instant in UTC epoch milliseconds; display/group using the device's current local calendar and locale |
| `createdByProfileId` | UUID string | Yes | Stable ID of the profile that created the expense; set from the current primary profile and immutable across edits and future sync |
| `vendor` | Nullable string | No | Trim whitespace; blank means absent; maximum 80 Unicode code points |
| `category` | Nullable string | No | `Needs`, `Wants`, `Culture`, or `Unexpected` |
| `receiptFileName` | Nullable string | No | App-private local receipt image reference; absent unless a user confirms an image selection and the copy succeeds |

**Relationships**: Expense detail fields belong to one expense record and do not require
separate records for vendor/category/receipt. The user's profile is the source for the
default currency, but the expense stores its currency so historical values remain
interpretable if the profile preference changes in a later feature. In Phase 2,
`createdByProfileId` is the Phase 1 primary profile's stable UUID. It is stored as
provenance, not as a foreign key: a future trusted household sync may receive expenses
before it has resolved the originating member's local profile record.

**Amount display**: Convert `amountMinorUnits` using the currency's supported fraction
digits and locale-aware currency formatting. Do not use binary floating-point as the
persisted value. Reject malformed, zero/negative, out-of-range, and excessive-precision
input; do not silently round. The domain amount parser must use the profile's currency
and its valid fraction digits. The representable upper bound is `Long.MAX_VALUE` minor
units; reject decimal input that overflows this value rather than applying an arbitrary
product-specific cap.

**Lifecycle**:

1. Amount input exists only as uncommitted UI state.
2. Explicit **Save expense** commits an amount-only expense, or the subtle
   **Add details now** action commits it and opens its detail screen.
3. A confirmed optional detail updates only that expense. The vendor is confirmed when
   focus leaves its field, including when the user chooses **Done for now**; do not
   navigate away until that confirmation has succeeded. A draft abandoned before that
   confirmation is discarded, and previously persisted details remain unchanged.
4. Explicit amount correction updates the same expense ID and capture time; it does not
   create another expense.
5. Confirmed deletion removes the expense from local history. Canceling deletion changes
   nothing. A later sync feature must decide on a tombstone strategy before adding sync;
   this phase does not retain deleted expense contents for future merging.
6. Capture records the current primary profile ID as `createdByProfileId`. Amount and
   detail edits never change the creator. Phase 6 sync must carry the source profile ID
   unchanged and resolve it through trusted household membership rather than replacing
   it with the receiving device's primary profile.

## Expense Period

A query value, not a persisted entity.

| Field | Type | Rules |
|---|---|---|
| `period` | `DAY`, `WEEK`, or `MONTH` | `DAY` is the default when opening expense history |
| `startInclusive` | Epoch milliseconds | Local start of selected day/week/month converted to an instant |
| `endExclusive` | Epoch milliseconds | Start of next local day/week/month converted to an instant |

Use a half-open time range `[startInclusive, endExclusive)` so an expense is included
exactly once at calendar boundaries. Day and month follow the current device locale and
calendar. Week starts on the locale's first day of week. History rows show time for
today and local date plus time for week/month. Sort newest first; show local-currency
amounts and a sum for the selected period.

## Reminder Preference

One local user-controlled daily reminder setting and its delivery guard. Persist it as
a singleton Room row so permission-prompt and at-most-once delivery state survive
process death.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `enabled` | Boolean | Yes | Defaults to false |
| `localTimeMinutes` | Integer | Yes | Local wall-clock minutes after midnight, range 0–1439; initial suggested value 20:30 |
| `permissionPrompted` | Boolean | Yes | Defaults to false; prevents automatic/repeated runtime permission prompts |
| `lastDeliveredLocalDate` | Nullable ISO local date | No | Claimed before notification delivery to prevent a WorkManager retry from posting twice in one local day |

Persist the preference locally. Notification permission is controlled by the operating
system and is not represented as granted merely because `enabled` is true. Permission
denial or revocation disables/cancels scheduled delivery without disabling capture.
Request permission only after the user enables reminders. If denied, do not prompt
again automatically; explain how to enable notifications in system settings. Use unique
one-time WorkManager work for the next local-time occurrence, then schedule the next
occurrence after delivery. Before posting, atomically claim the current local date; do
not post if that date is at or before the last claimed date, including after calendar
rollback. Execution can be delayed by Android.

## Receipt Image Storage

The chosen image must be copied into app-private internal storage before an expense
points to it. Store no external provider URI as the durable reference. If image selection,
copy, or persistence fails, report the failure and leave the expense and its other details
unchanged. Deleting an expense also removes its associated private image after its
reference is no longer in use. Do not request broad media-library access.
If the record change commits but old-file cleanup fails, keep the committed change,
report cleanup pending, and reconcile unreferenced files on the next receipt operation
or app restart. Never represent that outcome as an uncommitted record change.

## Persistence and Migration

- Increment `ProfileDatabase` from schema version 1 to version 2 and add the expense
  and reminder-preference entities plus the exported schema snapshot.
- Use Room's additive auto-migration from version 1 to 2. It must preserve the primary
  profile byte-for-byte in value (UUID, name, currency, nullable appearance) and create
  an empty expense history with reminders disabled.
- Do not use destructive migration fallback.
- Add indexes for capture-time period queries. Query and index names should follow the
  existing Room DAO conventions.
- Cover open-from-version-1 migration, file-backed create/read/update/delete, and failed
  writes with Android tests.
