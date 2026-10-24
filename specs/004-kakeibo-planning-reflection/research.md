# Research: Phase 3 — The Kakeibo Engine (Planning & Reflection)

Research is grounded in the Kakeibo specification, its UI mockup, and existing
NestVest implementation patterns. No external service or new dependency is required.

## Platform and UI architecture

**Decision**: Implement the feature in the existing Kotlin 2.2.10 / Jetpack Compose
Material 3 app, using feature-scoped domain, data, and UI packages; expose immutable
`StateFlow` state from ViewModels and keep composables state/callback driven.

**Rationale**: The app already uses Compose, Lifecycle ViewModels, coroutines, typed
repository results, and feature-scoped packages. Dashboard screens follow a route
collecting ViewModel state and delegating to screen composables. The Kakeibo mockup
provides the intended screen order and interactions without requiring another UI or
navigation toolkit.

**Alternatives considered**:
- Add another UI toolkit or navigation library: rejected because the existing
  Compose/Activity routing already supports the flow.
- Treat the HTML prototype as production code or a data source: rejected because it
  explicitly uses synthetic sample data and temporary in-memory state.

## Persistence and failures

**Decision**: Encrypt the existing Room database with SQLCipher for Android, wrapping a
random database key with Android Keystore. Convert the existing plaintext schema-v3
database to encrypted storage without data loss before Room opens it, then apply the
additive schema-v3-to-v4 migration. Store monthly plans, category allocations,
optional weekly amounts, reflections, and first-use learning state locally. Use
transactional writes and typed repository read/write results; preserve old data and
rethrow coroutine cancellation.

**Rationale**: `ProfileDatabase` is the current Room database and already owns profile,
expense, reminder, and dashboard records. The constitution requires platform-appropriate
at-rest protection; the user selected application-level database encryption. SQLCipher
provides encrypted SQLite storage compatible with the Room 2 support-open-helper
integration. Android Keystore protects the randomly generated key. Existing dashboard
and profile repositories use Room Flows, explicit error mapping, and transactions.
Existing migration tests create prior-version databases and verify row preservation;
the encryption rollout must additionally prove plaintext-to-encrypted conversion.

**Alternatives considered**:
- Store plans and reflections only in ViewModel state: rejected because the spec
  requires retention across app restarts and offline use.
- Add a remote database or analytics service: rejected by local-first privacy
  requirements.
- Rely only on app-private directory and device storage encryption: rejected because
  the user requested application-level database encryption.
- Destructively recreate the database: rejected because it would risk existing
  household records and violate data integrity.

## Money, calendar, and expense integration

**Decision**: Store plan and expense amounts as integer currency minor units. Use
overflow-safe integer aggregation; derive period bounds from the device-local time
zone. Reuse the app's locale-based week boundary convention and clip weekly segments to
the selected month. Derive progress from current saved expense rows; perform explicit
assignment through a conditional update that rechecks month, currency, and unassigned
status at write time.

**Rationale**: `Expense.amountMinorUnits` and `currencyCode` are the canonical saved
amount representation. `ExpensePeriod` derives local day/week/month boundaries using a
`Clock` and `WeekFields.of(locale)`. The expense DAO already supports observing a
date-bounded period, and expense editing returns typed success/failure results. A
conditional write prevents stale UI selection from assigning an expense that changed
after it was displayed.

**Alternatives considered**:
- Store monetary values as floating-point decimal values: rejected because rounding
  could misstate balances.
- Persist duplicate category totals or dashboard summaries: rejected because expense
  edits/deletes could make them stale.
- Assign using only the selected expense ID and a screen-time check: rejected because
  eligibility could change before the write.
- Use a separate week-start convention: rejected because it would disagree with the
  existing locale-aware expense period behavior.

## Dashboard and app navigation

**Decision**: Add a dedicated Kakeibo tile to the existing dashboard and route it
through the current `DashboardDestination`/`MainActivity` pattern. Keep it outside the
customizable pinned-option set so the required monthly prompt or summary remains
available. Leave the existing amount-first expense and reminder deep-link destinations
unchanged.

**Implementation feedback refinement**: Keep the tile fixed, but place it in the
usual dashboard shortcut section and reuse its icon/card/arrow presentation.
When no overall amount is entered, show a read-only calculated sum of categories
plus savings with an Edit action, without persisting it as available funds.
Slider bounds use the explicit or current calculated total; exact entry builds a
calculated plan from zero. This confirmed feedback refines the original mockup.

**Rationale**: The Kakeibo functional requirements require a dashboard tile even when
there is no plan, and require an accessible destination when summary data is
unavailable. The mockup shows a dedicated Kakeibo planning tile before separate
existing expense actions. The dashboard route currently owns destination callbacks,
and `MainActivity` maps those destinations to the existing feature routes.

**Alternatives considered**:
- Make the Kakeibo tile a regular pin/unpin option: rejected because unpinning could
  hide a required tile and prevent the specified nudge/summary from being found.
- Replace or redirect Add expense: rejected because it would change the established
  capture route and conflict with the existing dashboard/expense contract.
- Add a push notification for monthly planning: rejected because the spec requires a
  non-blocking in-app dashboard prompt.

## Visual and interaction patterns

**Decision**: Follow the supplied Kakeibo mockup's restrained paper/ink/sage design,
interactive optional learning, review-before-replacing plan, optional weekly pacing,
per-category daily chart and health, explicit expense assignment, and postponable
reflection. Use localized strings and Compose semantic labels; provide chart meaning
through accessible text in addition to drawn bars.

**Rationale**: The mockup covers the requested behavior and follows the existing
NestVest visual language. The constitution requires localization and accessibility;
icon/color-only financial states are insufficient. The app's existing screens already
use semantic state descriptions, test tags, and Compose accessibility/UI tests.

**Alternatives considered**:
- Add an external chart library: rejected because a compact monthly chart can be drawn
  with existing Compose primitives and the spec does not require a new dependency.
- Convey health using color or icons alone: rejected because health must remain
  understandable to screen-reader users and users who do not perceive the accent color.

## Source references

- [Feature specification](./spec.md) and [UI mockup](./ui-mockups.html).
- `app/src/main/java/com/nestvest/profile/data/ProfileDatabase.kt`
- `app/src/main/java/com/nestvest/expense/domain/Expense.kt`
- `app/src/main/java/com/nestvest/expense/domain/ExpensePeriod.kt`
- `app/src/main/java/com/nestvest/expense/data/ExpenseDao.kt`
- `app/src/main/java/com/nestvest/dashboard/domain/DashboardOption.kt`
- `app/src/main/java/com/nestvest/MainActivity.kt`
- `app/src/main/java/com/nestvest/dashboard/ui/DashboardViewModel.kt`
- `specs/003-dashboard-navigation/contracts/dashboard-ui.md`
- `app/src/androidTest/java/com/nestvest/expense/ExpenseMigrationTest.kt`
- `app/src/androidTest/java/com/nestvest/dashboard/DashboardPreferenceMigrationTest.kt`
