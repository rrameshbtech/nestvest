# Data Model: Phase 3 — The Kakeibo Engine (Planning & Reflection)

Kakeibo data is stored in the existing app-private Room database, encrypted with
SQLCipher and keyed by a random value protected with Android Keystore. The existing
plaintext database must be converted without data loss before normal Room access.
Spending and dashboard summaries are derived from saved `Expense` records; they are not
separately persisted. Amounts use the currency's integer minor units, matching
[Expense](../../app/src/main/java/com/nestvest/expense/domain/Expense.kt).

## Kakeibo Monthly Plan

One current plan per local calendar month. The month is identified by `YearMonth` and
uses the primary profile's preferred currency.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `planId` | UUID string | Yes | Stable identity for the plan |
| `yearMonth` | `YearMonth` / canonical `YYYY-MM` | Yes | Unique; one current plan per month |
| `currencyCode` | ISO 4217 code | Yes | Matches the primary profile currency when created |
| `availableMinorUnits` | Nullable non-negative `Long` | No | Null means omitted; never infer income or a remaining balance |
| `savingsIntentionMinorUnits` | Non-negative `Long` | Yes | An intention only; no transfer or reservation occurs |
| `weeklyPacingEnabled` | Boolean | Yes | False when optional weekly pacing is skipped |
| `createdAtEpochMillis` | `Long` | Yes | Creation instant |
| `updatedAtEpochMillis` | `Long` | Yes | Last successful plan update |

### Category Plan

Four rows per saved monthly plan, one for each canonical category: Needs, Wants,
Culture, and Unexpected.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `planId` | UUID string | Yes | Foreign key to Kakeibo Monthly Plan; cascade on delete |
| `categoryCode` | Stable category identifier | Yes | One of the four canonical categories; unique within a plan |
| `plannedMinorUnits` | Non-negative `Long` | Yes | Zero is valid and means no amount was allocated |

**Key**: `(planId, categoryCode)`.

If `availableMinorUnits` is set, the savings intention plus all four planned amounts
must not exceed it. If it is null, category totals remain valid but no available or
unassigned remainder is calculated.

The planning/review UI may display categories plus savings as a labeled calculated
overall amount. This is a derived display only: it does not populate
`availableMinorUnits` or imply available funds. Explicit overall entry sets the cap;
returning to calculated mode clears it. Slider bounds use the explicit amount when
present and otherwise the current calculated total.

## Weekly Allocation

Optional breakdown rows belong to a monthly plan and category; they do not create
additional funds.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `planId` | UUID string | Yes | Foreign key to Kakeibo Monthly Plan |
| `categoryCode` | Stable category identifier | Yes | Must refer to a category row in that plan |
| `weekStartDate` | `LocalDate` | Yes | Inclusive start of the displayed month-clipped locale week |
| `weekEndDateInclusive` | `LocalDate` | Yes | Inclusive end; actual calendar dates shown to the user |
| `plannedMinorUnits` | Non-negative `Long` | Yes | Blank UI input is stored as zero |

**Key**: `(planId, categoryCode, weekStartDate)`.

Week boundaries follow the existing locale-based `WeekFields` convention and are
clipped to the plan month, so the first/last segment can be shorter than seven days.
For each category, the sum of weekly allocations must not exceed that category's
monthly amount. No weekly rows and `weeklyPacingEnabled = false` represent skipped
pacing. Plan, category, and weekly rows are saved or replaced atomically.

## Period Reflection

One saved reflection per month, stored separately from the plan and expenses.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `reflectionId` | UUID string | Yes | Stable identity |
| `yearMonth` | Canonical `YYYY-MM` | Yes | Unique; identifies the reflected calendar month |
| `whatWentWell` | Text | Yes | User-authored response; may be empty if the user chooses |
| `whatToChange` | Text | Yes | User-authored response; may be empty if the user chooses |
| `nextMonthIntention` | Text | Yes | User-authored response; may be empty if the user chooses |
| `createdAtEpochMillis` | `Long` | Yes | Creation instant |
| `updatedAtEpochMillis` | `Long` | Yes | Last successful save |

Plan/expense comparison is derived from the selected month when the reflection is
opened. Later expense edits update the displayed current totals but do not alter the
saved reflection text or expense records.

## Learning Preference

A single local preference records whether to show the first-use learning prompt.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `preferenceId` | Fixed singleton key | Yes | Identifies the device-local Kakeibo preference |
| `learningPromptState` | Enum | Yes | `UNSEEN`, `VIEWED`, or `DISMISSED` |

Transition `UNSEEN → VIEWED` when the introduction opens, or `UNSEEN → DISMISSED`
when the user chooses Not now. Neither terminal state repeats the prompt. Learning
remains available from Kakeibo home.

## Derived Views (Not Persisted)

### Category Progress

Derived from a plan and the latest successful local expense read for the month.

| Value | Rule |
|---|---|
| Planned amount | Category plan amount in plan currency |
| Spent amount | Sum of matching-category expenses in the selected local month and plan currency |
| Remaining amount | Planned minus spent; may be negative |
| Health | `No allocation` for zero/zero; `Within plan` below allocation; `At limit` equal; `Over plan` above |
| Daily series | Every date in the selected month, including zero-spend days; only matching same-currency saved expenses |
| Unassigned expenses | Same-month expenses with no category, displayed separately |
| Other-currency expenses | Displayed separately; never converted or included in category totals |

Use overflow-safe exact arithmetic for aggregate totals. A failed expense or plan read is
`Unavailable`, not a zero value or a stale cached result.

### Dashboard Summary

Transient current-month state: `Loading`, `NoPlan`, `Available`, or `Unavailable`.
`NoPlan` prompts plan creation. `Available` contains same-currency categorized spending,
total category allocations, and a text health state derived from the most constrained
applicable category; an overspent category cannot be hidden by underspending elsewhere.
Rank applicable category states `Over plan` above `At limit` above `Within plan`.
Ignore a `No allocation`/zero-spend category when another category has an allocation or
spending; use overall `No allocation` only when no category has allocated or spent
anything. Unavailable state keeps the tile actionable and shows no stale amount.

## Assignment State Transition

An eligible assignment changes only an expense's category:

`Uncategorized → Needs | Wants | Culture | Unexpected`

Selection is a transient set of expense UUIDs, not a new persisted record. Before
writing, atomically verify that every selected record still exists, is unassigned, is
within the selected month using the device-local period bounds, and uses the plan currency.
Successful assignment triggers progress/chart recalculation. Cancellation, ineligible
or changed records, and storage failure leave all selected records unchanged and report
a non-success result. Reject empty batches; roll back earlier updates if any member
fails its conditional update.

## Validation and Persistence

- Validate each entered amount as non-negative and representable in the selected
  currency's minor units. Reject malformed/excess precision rather than rounding
  silently.
- Use exact integer arithmetic; do not calculate financial amounts with floating-point
  values.
- Use stable UUIDs or unique composite keys, not auto-increment identifiers.
- Convert the existing plaintext database to SQLCipher-encrypted storage before Room
  opens it. Preserve all profile, expense, reminder, and dashboard preference rows;
  never silently fall back to plaintext if encryption setup or conversion fails.
- Add the new tables through an additive Room migration from schema version 3 to 4.
- Do not persist derived expense totals or send financial data outside the device.
