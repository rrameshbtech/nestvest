# Feature Specification: Phase 3 — The Kakeibo Engine (Planning & Reflection)

**Feature Branch**: `004-kakeibo-planning-reflection`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "Phase 3: The Kakeibo Engine (Planning & Reflection) from @prd.md"

## Clarifications

### Session 2026-10-24

- Q: When a category’s recorded spending exceeds its plan, how should the remaining amount be shown? → A: Show a negative remaining amount and clearly identify it as overspending.
- Q: How should Kakeibo data be protected at rest? → A: Encrypt the app database with SQLCipher, protect a random database key with Android Keystore, and preserve existing local data while migrating the plaintext database.
- Q: What is the target size for the first-use learning flow? → A: Use the three-slide structure shown in the UI mockup and include an understandable example for each canonical category.
- Q: What is the first usable MVP boundary? → A: Deliver US1 learning and US2 monthly plan together; US1 remains independently testable as an optional learning flow.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Learn Kakeibo in a Visual Walk-Through (Priority: P1)

As a user new to Kakeibo, I want a short, visual, interactive introduction to its
benefits, categories, and use in NestVest so that I can understand mindful planning
before making my first plan.

**Why this priority**: Users need to understand the meaning of the categories before
making a useful plan. The introduction must help without becoming a barrier to planning.

**Independent Test**: Open the first-use learning prompt, navigate through the visual
slides, skip or finish them, and verify plan creation remains available and the learning
link is subtle on subsequent visits.

**Acceptance Scenarios**:

1. **Given** I have not viewed the Kakeibo introduction, **When** I first open the
   Kakeibo planner, **Then** I receive one non-blocking prompt to explore visual,
   interactive slides about Kakeibo's benefits, four categories, and how to use them in
   NestVest.
2. **Given** I am viewing a learning slide, **When** I move forward, backward, or exit,
   **Then** my position and navigation controls are clear and I can leave at any time
   to plan.
3. **Given** I have viewed the introduction or dismissed its first-use prompt,
   **When** I return to the Kakeibo home, **Then** learning is available through a
   subtle button and the first-use prompt is not repeated.
4. **Given** I skip the introduction, **When** I create a plan, **Then** the tutorial
   does not block planning and remains available through the subtle button.

---

### User Story 2 - Plan for a Month (Priority: P1)

As a user planning household spending, I want to set monthly category amounts with
sliders or exact text entry, optionally starting from a total available amount, so that
I can plan month by month in the way that works for me. I can still plan by category
when I do not know my total available amount.

**Why this priority**: A user-defined monthly plan is the foundation for meaningful
category progress, monthly reminders, and end-of-month reflection.

**Independent Test**: Create a monthly plan using category sliders and exact amount
fields, both with and without an overall available amount, then verify the allocation
summary, confirmation, and later retrieval.

**Acceptance Scenarios**:

1. **Given** I have no plan for the current month, **When** I begin planning,
   **Then** I can set a savings intention and amounts for Needs, Wants, Culture, and
   Unexpected using either a slider or an exact-value text field.
2. **Given** I provide an overall amount available, **When** I adjust category
   allocations and a savings intention, **Then** the planner shows the total category
   allocation, savings intention, and any amount remaining unassigned; it identifies
   when the combined amounts exceed the available amount.
3. **Given** I do not provide an overall amount available, **When** I allocate amounts
   to categories, **Then** the plan remains valid and the planner shows the total
   allocated without requiring an overall amount or inventing a remaining balance.
4. **Given** I am creating or changing a plan, **When** I edit an amount with the
   slider or text field, **Then** both controls show the same value and the summary
   updates without silently changing other amounts.
5. **Given** I have saved a plan, **When** I update it during the month,
   **Then** I can review the proposed amounts and confirm the change before it becomes
   the current plan.
6. **Given** the month has begun, **When** I have not created a plan,
   **Then** I can create one for the current month without losing or changing expenses
   already recorded.
7. **Given** I am offline, **When** I create or update a monthly plan,
   **Then** it is saved and available on the device without internet access.
8. **Given** saving a plan fails, **When** the app reports the failure,
   **Then** it does not show a success confirmation, preserves my entered values, and
   lets me retry.

---

### User Story 3 - Pace the Plan Week by Week (Priority: P2)

As a user who prefers shorter planning horizons, I want to break my monthly category
limits into weekly amounts so that I can pace spending while keeping the monthly plan
in view.

**Why this priority**: Weekly pacing can make a monthly intention easier to follow, but
it is optional and must not make monthly planning harder.

**Independent Test**: Create a monthly plan, add weekly amounts for one or more
categories, and verify that weekly pacing is optional, stays within monthly limits, and
does not increase the monthly budget.

**Acceptance Scenarios**:

1. **Given** I have a monthly plan, **When** I choose weekly planning,
   **Then** I can set weekly amounts within the monthly category limits.
2. **Given** the sum of weekly amounts for a category exceeds its monthly limit,
   **When** I save the weekly plan,
   **Then** the app explains the conflict and does not save the inconsistent amounts.
3. **Given** I choose not to set weekly amounts,
   **When** I save or use my monthly plan,
   **Then** the plan remains complete and its monthly category limits remain available.
4. **Given** a week is shorter or longer than seven days because of the calendar,
   **When** I review weekly pacing,
   **Then** each period is identified by its actual calendar dates and its amounts are
   not mistaken for additional monthly funds.

---

### User Story 4 - See Category Progress and Reflect (Priority: P1)

As a user following a plan, I want to see monthly spending patterns, category health,
and remaining funds, then reflect on the month so that I can understand my choices and
improve my next plan without being judged.

**Why this priority**: Honest progress and reflection connect the plan to actual
spending and deliver the mindful-finance value of the feature.

**Independent Test**: With a saved plan and expenses, open a category, inspect its
daily spending chart and health, assign an untagged expense, then complete and reopen
the monthly reflection.

**Acceptance Scenarios**:

1. **Given** I have a saved plan and categorized expenses in the plan currency,
   **When** I open a category for the current month,
   **Then** I see its allocated amount, total spent, remaining amount, a health
   indicator with a text label, and a compact chart of spending by day.
2. **Given** recorded spending in a category exceeds its planned limit,
   **When** I view the category,
   **Then** the remaining amount is negative and the health indicator clearly
   identifies overspending in text as well as visually.
3. **Given** an expense has no category,
   **When** I view monthly progress,
   **Then** it is shown separately as unassigned and is not silently attributed to a
   Kakeibo category.
4. **Given** a saved expense is edited, deleted, or categorized,
   **When** I next view monthly category progress,
   **Then** the category chart and amounts reflect the saved expense data.
5. **Given** an expense is recorded in a different currency from the plan,
   **When** I view category progress,
   **Then** that expense is identified separately and is not silently converted or
   included in a category total.
6. **Given** I am viewing a category and there are same-month, same-currency expenses
   without a category,
   **When** I select one or more expenses and confirm assigning them to this category,
   **Then** all selected expenses are tagged together and their amounts appear in the
   category total and daily chart. Selection alone does not write; I can deselect
   individual expenses or cancel the whole selection.
7. **Given** I cancel assigning an untagged expense or an assignment fails,
   **When** I return to category progress,
   **Then** the expense remains unassigned and the app reports failure without
   claiming it was categorized.
8. **Given** there is no plan for the current month,
   **When** I view progress,
   **Then** I can still see recorded expenses and an action to create a monthly plan
   without implying that unplanned spending is within budget.
9. **Given** a plan period has ended or I choose to review it,
   **When** I start reflection,
   **Then** I can compare planned amounts with recorded spending and answer what went
   well, what I would change, and what I want to improve next month.
10. **Given** I skip or leave reflection before saving,
   **When** I return to the app,
   **Then** my plan and expenses remain unchanged and I can complete reflection later.
11. **Given** I save a reflection,
   **When** I reopen that period,
   **Then** its saved reflection and plan are available without altering actual expense
   records.

---

### User Story 5 - Find the Monthly Plan from the Dashboard (Priority: P2)

As a user who has not planned the current month, I want a gentle dashboard nudge and a
compact summary when a plan exists so that I can start or review it without searching.

**Why this priority**: A monthly prompt and summary support a steady planning habit while
connecting Kakeibo to the dashboard established by the dashboard-navigation feature.

**Independent Test**: View the dashboard with and without a current-month plan; verify
the tile prompts planning when needed, shows accurate spend-versus-plan status when
available, and never presents a missing or stale value as current.

**Acceptance Scenarios**:

1. **Given** no plan exists for the current month, **When** I open the dashboard,
   **Then** a Kakeibo tile gently invites me to make this month's plan.
2. **Given** a current-month plan and current local expense totals are available,
   **When** I view the dashboard,
   **Then** the Kakeibo tile shows a compact spent-versus-planned summary and a
   text-labeled health status.
3. **Given** a plan or expense summary cannot be read or is not current,
   **When** I view the dashboard,
   **Then** the tile avoids fabricated or stale amounts and keeps the Kakeibo
   destination available.
4. **Given** I save the current month's plan,
   **When** I return to the dashboard,
   **Then** the planning nudge is replaced by the available summary and does not recur
   for that month.

### Edge Cases

- A savings goal or category limit cannot be negative. When an overall amount available
  is provided, the savings intention and category limits cannot exceed it; when omitted,
  no unassigned balance is calculated.
- For each category, the sum of its weekly amounts cannot exceed its monthly limit.
- A plan with no amount assigned to one or more categories remains valid; those
  categories show no planned funds rather than an invented allocation.
- Expenses recorded before a plan is created are included in progress when their date,
  currency, and category match the period and plan.
- Unassigned or different-currency expenses remain visible separately so category
  totals do not imply that all spending has been categorized or converted.
- When spending exceeds a category limit, the remaining amount is negative and the
  overspend is clearly identified; the app does not cap or conceal it.
- If an expense is changed or removed after reflection, reopening the period shows
  current expense totals alongside the saved reflection; the reflection text is not
  silently rewritten.
- If plan or reflection saving fails, the app reports the failure and retains the
  entered values for retry without showing a false success state.
- Period boundaries follow the user's local calendar; a plan or expense is not counted
  in two periods.
- If no expense was recorded in a category, its spent amount is zero and its remaining
  amount is shown against the saved category limit.
- If a category has zero allocation and zero spending, the app identifies that no
  amount was allocated rather than showing a positive health status. If any spending
  occurs against a zero allocation, it is identified as over allocation.
- The daily chart covers the full selected calendar month, including days with no
  spending; a month with no category expenses shows an honest empty state.
- Only uncategorized expenses from the current month in the plan currency can be
  assigned from a category view. Assignment is explicit; canceling or a failed save
  leaves the expense unassigned.
- A non-blocking dashboard prompt appears while the current month has no plan and
  disappears when the plan is saved. It is not a push notification.
- The Kakeibo learning prompt is shown once on first use; after it is viewed or
  dismissed, the introduction remains available as a subtle home-screen action.
- Expenses are assigned to periods using their saved capture date and time in the
  device's local calendar, consistent with Phase 2 expense history.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST introduce the four canonical Kakeibo categories—Needs,
  Wants, Culture, and Unexpected—with understandable examples.
- **FR-002**: The app MUST provide short, visual, interactive slides explaining
  Kakeibo's benefits, categories, and how to use NestVest to plan. Users MUST be able
  to navigate or leave the slides without blocking plan creation.
- **FR-003**: The app MUST prompt users to learn Kakeibo once on first use. After the
  user views or dismisses that prompt, learning MUST remain available as a subtle
  action on the Kakeibo home and the first-use prompt MUST NOT recur.
- **FR-004**: The app MUST support calendar-month plans only, with a savings intention
  and category limits for each month.
- **FR-005**: The overall amount available for a month MUST be optional. Users MUST be
  able to create a plan by allocating amounts to categories without providing that
  total.
- **FR-006**: The app MUST let users adjust each category amount using a slider or an
  exact-value text field, keep both controls synchronized, and show the total category
  allocation as values change. Slider maxima MUST use the explicit overall amount,
  or the current calculated total when omitted; exact entry allows users to build or
  increase that calculated total, including from zero.
- **FR-007**: When an overall amount available is provided, the app MUST display the
  amount allocated and the unassigned remainder, and MUST prevent saving when the
  savings intention and category limits exceed that amount. It MUST NOT silently
  change user-entered values.
- **FR-008**: When the overall amount available is omitted, the app MUST NOT require it
  or display an invented remaining balance; it MUST still show the sum allocated to
  categories. It MUST show categories plus savings as a clearly labeled, read-only
  calculated overall amount with an explicit edit action. This display MUST NOT become
  a saved available-funds cap unless the user enters an overall amount.
- **FR-009**: The app MUST let users review and confirm changes before replacing the
  current month's plan and MUST retain plans independently for each calendar month.
- **FR-010**: The app MAY offer optional weekly pacing as a breakdown of monthly category
  limits; the sum of weekly amounts for a category MUST NOT exceed its monthly limit,
  and weekly amounts MUST NOT create additional funds.
- **FR-011**: The app MUST display planned, spent, and remaining amounts by category
  using saved expenses in the same currency as the plan. If spending exceeds the
  planned limit, it MUST show a negative remaining amount and clearly identify the
  overspend.
- **FR-012**: The app MUST provide a compact chart of daily spending for each category
  across the selected month, using only matching saved expenses with clearly labeled
  dates and amounts.
- **FR-013**: The app MUST show a category health icon and textual status comparing
  recorded spending with allocation: below allocation is "Within plan", equal to
  allocation is "At limit", and above allocation is "Over plan". A zero allocation
  with zero spending MUST be labeled "No allocation". It MUST NOT rely on color or
  icon alone.
- **FR-014**: The app MUST show uncategorized and different-currency expenses separately
  from category totals and MUST NOT infer a category or perform an undisclosed currency
  conversion.
- **FR-015**: From a category view, the app MUST let users select and explicitly assign
  one or more same-month, same-currency uncategorized expenses to that category in one
  confirmation. Batch assignment MUST be atomic: if any selected expense is no longer
  eligible or a write fails, none are assigned. It MUST update progress after successful
  assignment and report failure without falsely claiming success.
- **FR-016**: The app MUST recalculate category amounts and daily charts from current
  saved expense amounts, dates, currencies, and categories when users review them.
- **FR-017**: The app MUST provide a monthly reflection that lets users compare plan
  amounts with recorded spending and record what went well, what they would change, and
  what they want to improve next month.
- **FR-018**: The app MUST let users postpone or skip reflection without changing the
  plan or expense records.
- **FR-019**: When the current month has no plan, the dashboard MUST show a gentle,
  non-blocking Kakeibo prompt to plan that month. It MUST replace the prompt with a
  compact summary of same-currency categorized spending versus total category
  allocations and a text-labeled health status once a valid plan and current summary
  are available. The overall status MUST reflect the most constrained applicable
  category so an overspent category is not hidden by underspending elsewhere.
- **FR-020**: Dashboard summaries MUST distinguish unavailable data from meaningful
  zero, MUST NOT present stale or fabricated information as current, and MUST keep the
  Kakeibo destination accessible when a summary cannot be loaded.
- **FR-021**: The app MUST retain saved plans and reflections on the device and make
  them available offline, protect them at rest using platform-appropriate safeguards,
  and not send household financial data to public internet services or third-party
  analytics.
- **FR-022**: The app MUST report plan, reflection, and category-assignment failures
  clearly, retain unsaved user-entered values for retry, and never show a success state
  for an unsuccessful save.
- **FR-023**: The app MUST present financial values honestly, distinguish planned
  amounts from actual recorded expenses, and avoid shame-based or judgmental
  descriptions.
- **FR-024**: All planning, category progress, charts, and reflection flows MUST be
  usable with assistive technologies and support localized text, dates, numbers, and
  currency presentation.
### Key Entities

- **Planning Period**: A calendar month with a defined start and end, available amount,
  optional overall amount, savings intention, plan currency, and monthly plan.
- **Category Plan**: A user-defined monthly limit for one Kakeibo category and its
  optional weekly pacing amounts.
- **Category Progress**: A view of one category's allocation, matching saved expenses,
  remaining amount, health status, daily spending chart, and eligible untagged expenses.
- **Dashboard Summary**: A monthly Kakeibo tile that prompts when no plan exists or
  summarizes current planned and recorded amounts with a health status.
- **Learning Preference**: Whether the first-use Kakeibo prompt has been shown; the
  introduction remains available from the Kakeibo home after that prompt.
- **Period Reflection**: A user's saved monthly review, including what went well,
  what they would change, and an intention for the next period.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At least 90% of first-time users who begin monthly planning can save a
  valid plan on their first attempt without help.
- **SC-002**: A returning user can review the four category balances for a planned
  month in under 30 seconds.
- **SC-003**: At least 90% of users who complete a period reflection can identify the
  planned and recorded amount for each category without assistance.
- **SC-004**: In acceptance testing, all plan and reflection data remains available
  after closing and reopening the app with the device offline.
- **SC-005**: In acceptance testing, every category balance matches the plan and
  eligible saved expenses, and unassigned or different-currency expenses are never
  included in a category total.
- **SC-006**: At least 90% of first-time users can correctly identify that weekly
  amounts are part of, and do not add to, the monthly plan.
- **SC-007**: At least 90% of users can complete a monthly category allocation using
  either the slider or exact amount field and correctly identify the allocated total.
- **SC-008**: In acceptance testing, assigning an eligible untagged expense changes
  only its category and updates the correct monthly category total and daily chart.
- **SC-009**: In acceptance testing, the dashboard prompts when no monthly plan exists,
  and replaces the prompt with an accurate summary after a plan is saved.
- **SC-010**: At least 90% of first-time users can locate and complete or skip the
  Kakeibo introduction, and users who have already viewed or dismissed it do not
  receive the first-use prompt again.

## Assumptions

- A user plans for one household on the device's primary profile; family sharing and
  synchronization are outside Phase 3.
- The overall amount available is optional. If omitted, the app derives and displays
  category allocations but does not infer income, a savings capacity, or unassigned
  balance.
- The savings goal is an intention, not an automatically transferred or reserved
  balance.
- Monthly plans use the primary profile's preferred currency. No automatic foreign
  currency conversion is available in this phase.
- Calendar months are the only plan periods. Optional weekly pacing remains a
  breakdown of monthly category limits, not a separate budget or planning period.
- Category progress uses the expense's saved date and category; missing categories are
  not inferred. The date is the Phase 2 expense capture date and time, interpreted
  using the device's local calendar.
- The Phase 2 expense categories Needs, Wants, Culture, and Unexpected are the
  canonical categories. Survival, Optional, and Extra may be shown as explanatory
  labels for Needs, Wants, and Unexpected; they do not represent additional categories.
- Progress includes only saved Phase 2 manual outflow expenses; income, transfers,
  recurring transactions, and imported transactions are outside this phase.
- Monthly planning prompts are non-blocking, in-app dashboard content, not push
  notifications. Reflection can be started by the user.
- The dashboard-navigation feature is the app's post-profile starting point; this
  feature adds a Kakeibo destination and summary tile when planning becomes available
  without changing amount-first expense capture.
- Daily charts show only same-currency saved outflows assigned to the selected
  category. Users assign untagged expenses explicitly from that category's review.
- The introduction, planning, progress, and reflection are available offline and use
  calm, non-judgmental language consistent with the product principles.
