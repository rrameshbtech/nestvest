# Feature Specification: Phase 3 — The Kakeibo Engine (Planning & Reflection)

**Feature Branch**: `003-kakeibo-planning-reflection`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "Phase 3: The Kakeibo Engine (Planning & Reflection) from @prd.md"

## Clarifications

### Session 2026-10-24

- Q: When a category’s recorded spending exceeds its plan, how should the remaining amount be shown? → A: Show a negative remaining amount and clearly identify it as overspending.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Learn the Kakeibo Approach (Priority: P1)

As a user new to Kakeibo, I want a concise, interactive introduction to its spending
categories and planning questions so that I can make an informed plan without needing
prior knowledge of the method.

**Why this priority**: Users need to understand the meaning of the categories before
making a useful plan. The introduction must help without becoming a barrier to planning.

**Independent Test**: Start the introduction, review each Kakeibo category and planning
prompt, then either finish or leave it; verify the user can begin planning and reopen
the introduction later.

**Acceptance Scenarios**:

1. **Given** I have not seen the introduction, **When** I choose to learn about Kakeibo,
   **Then** I can review the four categories—Needs (Survival), Wants (Optional), Culture,
   and Unexpected (Extra)—with clear, brief examples of what belongs in each.
2. **Given** I am viewing the introduction, **When** I leave before finishing,
   **Then** I can still create a plan and can return to the introduction later.
3. **Given** I have completed or skipped the introduction, **When** I start a plan,
   **Then** the introduction does not block or repeat as a required setup step.

---

### User Story 2 - Set an Intentional Period Plan (Priority: P1)

As a user planning household spending, I want to set an amount available, a savings
intention, and category limits for a month so that I can decide what matters before I
spend.

**Why this priority**: A usable, user-defined plan is the foundation for meaningful
remaining-funds views and end-of-period reflection.

**Independent Test**: Create a plan for a month with an available amount, savings goal,
and category limits; verify that the plan is saved, its arithmetic is clear, and it
remains available after closing and reopening the app.

**Acceptance Scenarios**:

1. **Given** I have no plan for the current month, **When** I begin planning,
   **Then** I can enter the amount I intend to make available for the period, set a
   savings goal, and set limits for Needs (Survival), Wants (Optional), Culture, and
   Unexpected (Extra).
2. **Given** I am creating or changing a plan, **When** I review the amounts,
   **Then** the app clearly shows the available amount, savings goal, category limits,
   and any amount not yet assigned to a category; it does not silently change my
   entries to make them balance.
3. **Given** my savings goal and category limits exceed the amount available,
   **When** I try to save the plan, **Then** the app identifies the amount that needs
   adjustment and does not save an inconsistent plan.
4. **Given** I have saved a plan, **When** I update it during the period,
   **Then** I can review the proposed amounts and confirm the change before it becomes
   the current plan.
5. **Given** the month has begun, **When** I have not created a plan,
   **Then** I can create one for the current month without losing or changing expenses
   already recorded.
6. **Given** I am offline, **When** I create or update a plan,
   **Then** it is saved and available on the device without internet access.
7. **Given** saving a plan fails, **When** the app reports the failure,
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

### User Story 4 - See Remaining Funds and Reflect (Priority: P1)

As a user following a plan, I want to see remaining funds by Kakeibo category and
reflect on the completed period so that I can understand my choices and improve my next
plan without being judged.

**Why this priority**: Honest progress and reflection connect the plan to actual
spending and deliver the mindful-finance value of the feature.

**Independent Test**: With a saved plan and categorized expenses, review remaining
amounts, then complete a period reflection and verify the review compares planned and
recorded spending and can be reopened.

**Acceptance Scenarios**:

1. **Given** I have a saved plan and categorized expenses in the plan currency,
   **When** I view plan progress,
   **Then** I see the amount spent and amount remaining for each of the four categories.
2. **Given** recorded spending in a category exceeds its planned limit,
   **When** I view plan progress,
   **Then** the remaining amount is shown as negative and the overspend is clearly
   identified.
3. **Given** an expense has no category,
   **When** I view plan progress,
   **Then** it is shown separately as unassigned and is not silently attributed to a
   Kakeibo category.
4. **Given** a saved expense is edited, deleted, or categorized,
   **When** I next view plan progress,
   **Then** the category amounts reflect the saved expense data without changing the
   expense itself.
5. **Given** an expense is recorded in a different currency from the plan,
   **When** I view plan progress,
   **Then** that expense is identified separately and is not silently converted or
   included in a category total.
6. **Given** a plan period has ended or I choose to review it,
   **When** I start reflection,
   **Then** I can compare planned amounts with recorded spending and answer what went
   well, what I would change, and what I want to improve next period.
7. **Given** I skip or leave reflection before saving,
   **When** I return to the app,
   **Then** my plan and expenses remain unchanged and I can complete reflection later.
8. **Given** I save a reflection,
   **When** I reopen that period,
   **Then** its saved reflection and plan are available without altering actual expense
   records.

### Edge Cases

- A savings goal or category limit cannot be negative, and the combined savings goal
  and category limits cannot exceed the amount available for the period.
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
- Expenses are assigned to periods using their saved capture date and time in the
  device's local calendar, consistent with Phase 2 expense history; weeks begin on the
  locale's first day of the week.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST introduce the Kakeibo categories Needs (Survival), Wants
  (Optional), Culture, and Unexpected (Extra) using understandable examples.
- **FR-002**: The app MUST allow users to skip or revisit the Kakeibo introduction
  without blocking plan creation.
- **FR-003**: The app MUST allow users to create a monthly plan containing an amount
  available for the period, a savings goal, and limits for each of the four categories.
- **FR-004**: The app MUST allow an amount to remain unassigned when the savings goal
  and category limits total less than the available amount, and MUST display that
  amount separately.
- **FR-005**: The app MUST prevent saving a plan when the savings goal and category
  limits exceed the amount available, and MUST identify the discrepancy without
  silently changing the user's values.
- **FR-006**: The app MUST let users review and confirm changes before replacing the
  current plan for a period.
- **FR-007**: The app MUST offer optional weekly pacing within the monthly category
  limits; the sum of weekly amounts for a category MUST NOT exceed its monthly limit,
  and weekly amounts MUST NOT create additional funds.
- **FR-008**: The app MUST display planned, spent, and remaining amounts by category
  using saved expenses in the same currency as the plan. If spending exceeds the
  planned limit, it MUST show a negative remaining amount and clearly identify the
  overspend.
- **FR-009**: The app MUST show uncategorized and different-currency expenses separately
  from category totals and MUST NOT infer a category or perform an undisclosed currency
  conversion.
- **FR-010**: The app MUST recalculate plan progress from current saved expense amounts,
  dates, currencies, and categories when a user reviews the plan.
- **FR-011**: The app MUST provide a period reflection that lets users compare plan
  amounts with recorded spending and record what went well, what they would change, and
  what they want to improve.
- **FR-012**: The app MUST let users postpone or skip reflection without changing the
  plan or expense records.
- **FR-013**: The app MUST retain saved plans and reflections on the device and make
  them available offline, protect them at rest using platform-appropriate safeguards,
  and not send household financial data to public internet services or third-party
  analytics.
- **FR-014**: The app MUST report plan or reflection save failures clearly, retain
  unsaved user-entered values for retry, and never show a success state for an
  unsuccessful save.
- **FR-015**: The app MUST present financial values honestly, distinguish planned
  amounts from actual recorded expenses, and avoid shame-based or judgmental
  descriptions.
- **FR-016**: All planning, progress, and reflection flows MUST be usable with
  assistive technologies and support localized text, dates, numbers, and currency
  presentation.

### Key Entities

- **Planning Period**: A calendar month with a defined start and end, available amount,
  savings goal, plan currency, and current plan.
- **Category Plan**: A user-defined monthly limit for one Kakeibo category and its
  optional weekly pacing amounts.
- **Plan Progress**: A view of planned amounts compared with matching saved expenses,
  remaining funds, unassigned expenses, and expenses in other currencies.
- **Period Reflection**: A user's saved review of the period, including what went well,
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

## Assumptions

- A user plans for one household on the device's primary profile; family sharing and
  synchronization are outside Phase 3.
- The user supplies the amount available for a period because automated income and
  cashflow tracking belongs to a later phase.
- The savings goal is an intention, not an automatically transferred or reserved
  balance.
- Monthly plans use the primary profile's preferred currency. No automatic foreign
  currency conversion is available in this phase.
- Weekly pacing is optional and is a breakdown of monthly category limits, not a
  separate budget.
- Category progress uses the expense's saved date and category; missing categories are
  not inferred. The date is the Phase 2 expense capture date and time, interpreted
  using the device's local calendar; weeks start on the locale's first day of the week.
- The Phase 2 expense categories Needs, Wants, Culture, and Unexpected are the
  canonical categories. Survival, Optional, and Extra may be shown as explanatory
  labels for Needs, Wants, and Unexpected; they do not represent additional categories.
- Progress includes only saved Phase 2 manual outflow expenses; income, transfers,
  recurring transactions, and imported transactions are outside this phase.
- Reminders and automated prompts are not required for Phase 3; reflection can be
  started by the user.
- The introduction, planning, progress, and reflection are available offline and use
  calm, non-judgmental language consistent with the product principles.
