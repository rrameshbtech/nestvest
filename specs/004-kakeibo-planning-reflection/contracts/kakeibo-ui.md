# Kakeibo UI Contract

This contract describes the native Android experience corresponding to
[ui-mockups.html](../ui-mockups.html). The mockup is an interaction and visual
reference with synthetic, in-memory values only. The [feature specification](../spec.md),
this contract, and current local Room/expense data define production behavior.

## Entry and Navigation

- The Kakeibo tile is available on the post-profile dashboard independently of the
  user's pinned shortcut configuration. Present it within the usual dashboard tile
  section, using the same bordered card, circular icon, and trailing arrow as Add
  expense; it remains fixed rather than becoming a pin/unpin choice.
- The tile shows a gentle current-month planning prompt when no plan exists. With a
  current plan and fresh summary, it shows same-currency categorized spending versus
  category allocation and textual health reflecting the most constrained applicable
  category (`Over plan` before `At limit` before `Within plan`; `No allocation` when
  nothing is allocated or spent). Loading/unavailable state never displays a stale
  amount and never disables navigation.
- Existing Add expense, Expenses, Reminders, and reminder-notification capture
  destinations retain their Phase 2 behavior.
- Kakeibo home links to monthly plan, progress, reflection, and learning. The app brand
  returns to the dashboard from Kakeibo screens. Home has a meaningful planner heading
  and no redundant Back control; child screens retain local back/cancel navigation.
- Home uses a selected-month summary, progress/reflection entry tiles with explanatory
  subtitles and trailing arrows, a subtle Learn action, and a primary create/edit-plan
  action. Keep the selected month and all amounts derived from local state.
- Back/cancel returns to the originating Kakeibo screen and discards only unsaved
  drafts; it does not modify saved expenses or plans.

## Learning

- On first Kakeibo entry, show one non-blocking inline soft card with options to explore
  slides, dismiss, or plan immediately; do not interrupt entry with a modal dialog.
- Slides explain Kakeibo's purpose, the four canonical categories, and how the app
  supports the method. Provide clear previous/next, skip, and plan actions.
- Viewing or dismissing the prompt prevents it from appearing again. Keep a subtle
  Learn action on Kakeibo home.
- Learning is optional and never gates plan creation.

## Monthly Plan and Weekly Pacing

- Display the selected calendar month and profile currency. Plans are month-by-month.
- Support a savings intention and each of Needs, Wants, Culture, and Unexpected. Offer
  synchronized slider and exact-value controls for category amounts.
- Overall available amount is optional. When present, show savings, category total,
  total allocation, and unassigned remainder; reject over-allocation without altering
  entered amounts. When omitted, show category totals but no invented remainder.
- When omitted, display categories plus savings as a read-only, explicitly calculated
  overall amount. An Edit action enables explicit entry, and Use calculated amount
  clears the explicit cap. The calculated display is not persisted as available
  funds; `availableMinorUnits` remains null.
- Each slider's maximum is exactly the explicit overall amount, or the current
  calculated total when no explicit amount is provided. Do not expand an explicit cap
  to fit an overallocated category. At a zero/invalid maximum, disable the slider but
  retain exact entry; users build/increase a calculated total through exact amounts.
- Review shows localized currency amounts in simple bordered category and allocation/
  savings cards, distinguishing a calculated overall amount from an explicit cap.
- Require a review/confirmation before replacing the saved month's plan. A failed save
  keeps the draft and previous plan, displays an accessible error, and permits retry.
- Review explicitly shows whether weekly pacing is included. Successful confirmation
  opens progress with a save-success announcement; Back returns to Kakeibo home.
- Weekly pacing is optional and remains within the monthly plan. Show each locale-week
  period's actual dates, including partial first/last weeks; blank entries mean zero.
  Reject weekly totals above the associated category's monthly amount.
- Group weekly inputs into bordered category cards, with currency-prefixed date-range
  fields and a weekly-total/monthly-limit summary. Use two columns normally and one
  when the system font scale exceeds 1.3. Save validates before returning to planning;
  Skip disables weekly pacing without changing monthly allocations.
- Plan creation/update does not change existing expenses or move/reserve money.

## Progress and Category Detail

- Show recorded expense amounts separately from plan amounts. Use only current,
  saved, same-currency expenses in the selected local calendar month for category
  totals.
- Show unassigned and different-currency expenses separately; never infer a category
  or silently convert currency.
- Progress uses a soft overview and compact category cards with allocation, a health
  track, spent/remaining values, a textual health badge, and a details action. Show
  unassigned spending and each foreign-currency total in a separate exception card.
- Category detail shows planned, spent, remaining, a textual health label and icon,
  plus a compact daily spending chart spanning the whole month. Health is `No
  allocation` for zero allocation and zero spend, `Within plan` below allocation,
  `At limit` at allocation, and `Over plan` above allocation. Negative remaining is
  visible when overspent.
- Distinguish no plan, zero spend, empty chart, loading, and unavailable data. With no
  plan, keep recorded expenses visible and offer plan creation without implying spend
  is within budget.
- From a category, allow explicit selection/confirmation of only same-month,
  same-currency unassigned expenses. Cancel leaves the expense unassigned. Failure
  reports the error and refreshes from saved data; do not claim assignment succeeded.
- Present eligible expenses with independent checkboxes and separate Assign/Cancel
  controls. Show the selected count in Assign; disable it when nothing is selected.
  Selecting/deselecting alone never writes, and Cancel clears the whole selection.
  Show date, vendor, and formatted amount; prevent duplicate assignment writes,
  selection/month changes, and leaving through local Back while a write is pending.
- Confirm all selected expenses in one transaction. If any record is no longer
  eligible, roll back the entire batch, refresh the list, and ask for a new selection.
  On storage failure, keep the selection for retry and leave all saved data unchanged.
- Refresh progress and chart from current saved amounts, categories, currencies, and
  dates after return/resume and after successful assignment.

## Reflection

- Allow reflection for a selected month with a comparison of the current plan and
  current recorded spending, when available.
- Provide user-authored fields for what went well, what they would change, and what to
  improve next month. Keep wording calm and non-judgmental, with labels above multiline
  fields and the four-category comparison in a soft card.
- Not now/back leaves the plan and expenses unchanged. Saving failure keeps the draft
  for retry and does not show success.
- Disable saving until the reflection read succeeds, and provide retry on read failure
  rather than allowing existing answers to be overwritten. Cancel pending reads when
  leaving and do not apply a late response to another month.
- Reopening a saved reflection shows its saved answers alongside current expense
  totals. Updating expenses refreshes the totals but does not rewrite the answers.
- Saving opens a separate saved-reflection view with an explicit empty-answer label,
  Back to plan progress, and Edit reflection. Editing replaces that view instead of
  accumulating editor/saved destinations; returning to progress reuses its existing
  back-stack entry when present.

## Visual, Accessibility, and Localization

- Follow the mockup's NestVest paper/ink/sage palette, restrained typography, flat
  bordered cards, compact category rows, and emphasized primary actions. Do not
  introduce a new theme or imply the prototype's sample data is real.
- Reuse the existing expense scaffold/brand header and button styles: ink primary
  buttons, outlined secondary buttons, 8dp control corners, sage sliders, and flat
  paper/soft-sage summary cards throughout learning, planning, progress, and reflection.
- Use localized resources, locale-aware currency formatting, and local calendar dates;
  do not hardcode the mockup's October 2026, INR, or English-only strings.
- Use semantic labels and state descriptions for category/health/assignment controls.
  Expose chart data through an accessible textual summary; never rely on color or icon
  alone.
- Support TalkBack, keyboard/switch access, visible focus, minimum 48dp touch targets,
  font scaling, and scrolling on short/narrow screens.
- Announce loading, save success, save failure, and unavailable states accessibly.

## Local Data and Failure Behavior

- Store plans, reflections, and learning preference in the existing app-private Room
  database encrypted with SQLCipher; protect the random database key using
  Android Keystore. Convert existing plaintext local data without loss before normal
  encrypted Room access. Keep all core behavior available offline.
- Use current local reads only. A failed read is unavailable; never convert it to
  meaningful zero or fall back to a stale total.
- Persist plan/category/weekly changes atomically. A failed write preserves the prior
  saved plan and unsaved user values.
- Guard expense assignment at write time by verifying that the expense remains
  uncategorized and eligible for the selected month and plan currency. Apply this to
  every member of a batch inside the same all-or-nothing transaction.
- If the encrypted database key is unavailable or plaintext-to-encrypted conversion
  fails, report a recoverable storage error and do not open or recreate the database in
  plaintext or discard the source data.
- No Kakeibo operation sends financial data to public internet services or third-party
  analytics.
