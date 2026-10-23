# Feature Specification: Phase 2 — Ultra-Fast Daily Expense Capture

**Feature Branch**: `002-daily-expense-capture`

**Created**: 2026-10-23

**Status**: Draft

**Input**: User description: "Phase 2: Ultra-Fast Daily Expense Capture"

## Clarifications

### Session 2026-10-23

- Q: When should an optional expense detail count as provided and be saved while the user is enriching an expense? → A: Save each detail when the user confirms that detail; unfinished edits are not committed.
- Q: Should users be able to remove an expense they saved by mistake, or only edit its details and amount? → A: Allow users to delete an expense after confirming the action.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Capture an Expense Immediately (Priority: P1)

As a user who has just paid for something, I want to record the amount with minimal
effort so that I do not have to rely on memory or postpone tracking my spending.

**Why this priority**: Fast, reliable capture is the central purpose of this phase and
must be useful even when the user does not want to add any other details.

**Independent Test**: Open the expense capture experience, enter a valid amount, save it,
and verify the expense remains available after closing and reopening the app, including
when the device is offline.

**Acceptance Scenarios**:

1. **Given** I have completed primary profile setup, **When** I open the app, **Then** I
   can begin entering an expense amount without first navigating through unrelated
   screens.
2. **Given** I enter a valid amount, **When** I choose Save without entering any other
   details, **Then** the app saves the expense and confirms completion without requiring
   a category, description, vendor, or receipt.
3. **Given** I have saved an expense, **When** I close and reopen the app, **Then** the
   expense remains available with its original amount, currency, and capture time.
4. **Given** I open expense capture, **When** I want to review expenses already recorded,
   **Then** I can open Expenses directly from its app bar and see today's expenses by
   default, including their currency-formatted amounts. The focused amount-entry screen
   itself does not display expense history.
5. **Given** I am viewing saved expenses, **When** I choose the day, week, or month
   period, **Then** I see expenses captured within that period and can select one to
   review or update.
6. **Given** I am offline, **When** I save a valid expense, **Then** it is saved locally
   without waiting for or sending data to an internet service.
7. **Given** the amount is empty, zero, invalid, or outside the supported amount range,
   **When** I try to save it, **Then** the app explains what needs correction and does
   not create an expense.
8. **Given** saving an expense fails, **When** the app reports the failure, **Then** it
   does not show a success confirmation, preserves the entered amount, and lets me retry.
9. **Given** I save an expense, **When** it is stored, **Then** it records the stable
   identity of the profile that created it, which is the Phase 1 primary user in this
   release.

---

### User Story 2 - Add Details When Convenient (Priority: P2)

As a user who wants more context for an expense, I want to add or change optional details
after the amount has been saved so that enrichment never slows or blocks capture.

**Why this priority**: Context improves later review, but making it optional preserves
the speed and reliability of the core capture flow.

**Independent Test**: Save an amount without details, then add a vendor, a Kakeibo
category, and a receipt image to the saved expense; verify each detail remains attached
to the correct expense and can be changed or removed.

**Acceptance Scenarios**:

1. **Given** I have saved an expense, **When** I am offered optional details, **Then** I
   can dismiss the offer and continue without entering anything else.
2. **Given** I have saved an expense, **When** I choose to add details, **Then** I can
   add a vendor, select a Kakeibo category, or attach a receipt image independently, and
   each provided detail is saved without requiring a separate final save for the whole
   enrichment flow.
3. **Given** I am adding or reviewing details, **When** the expense is displayed, **Then**
   a clear summary keeps its currency-formatted amount and capture time visible alongside
   the vendor, category, and receipt details already provided, with missing details
   distinguishable from saved ones.
4. **Given** I have provided one or more details, **When** I leave the detail editor or
   stop before adding more, **Then** every detail already provided remains saved and the
   expense can be reopened with those details intact.
5. **Given** I have confirmed one detail and started editing another, **When** I leave
   without confirming the second detail, **Then** the confirmed detail remains saved and
   the unfinished edit is not committed.
6. **Given** I have saved an expense, **When** I later reopen it, **Then** I can add,
   change, or remove optional details without changing the original amount unless I
   explicitly edit it.
7. **Given** a detail is unavailable or cannot be saved, **When** I provide or view the
   expense, **Then** the expense and its other saved details remain intact and the app
   clearly reports the unsuccessful change.
8. **Given** I have saved an expense with the wrong amount, **When** I explicitly edit
   and save the corrected amount, **Then** the corrected amount is shown and the change
   is not confused with an additional expense.
9. **Given** I have selected a saved expense to remove, **When** I confirm deletion,
   **Then** it is removed from the saved expense history; if I cancel, it remains
   available and unchanged.

---

### User Story 3 - Build a Consistent Capture Habit (Priority: P3)

As a user who sometimes forgets cash spending, I want an optional, gentle reminder so
that I can remember to record expenses without being interrupted or pressured.

**Why this priority**: Reminders can help establish a routine, but must remain secondary
to user-controlled, interruption-free capture.

**Independent Test**: Enable a daily reminder, verify no more than one reminder is
scheduled per day at the selected time, open capture from it, and disable reminders.

**Acceptance Scenarios**:

1. **Given** reminders are not enabled, **When** I use expense capture, **Then** the app
   does not send expense reminders.
2. **Given** I choose to enable a daily reminder and select a time, **When** that time
   occurs, **Then** the app may show one discreet reminder asking whether I have
   unrecorded spending and provides a direct path to capture.
3. **Given** a reminder is shown, **When** I dismiss it or choose not to record an
   expense, **Then** the app does not require an explanation or interrupt another task.
4. **Given** reminders are enabled, **When** I disable them or withdraw notification
   permission, **Then** the app stops scheduling or showing reminders.

### Edge Cases

- Amount entry must respect the selected profile currency's supported fractional units;
  a value with too many fractional digits is explained and cannot be silently rounded
  into a different amount.
- A save attempt with no amount, a zero or negative amount, malformed input, or an
  amount outside the supported range must not create a record.
- If a user leaves capture before saving, the app must not present the unsaved value as
  a completed expense.
- If a user begins deleting an expense and cancels the confirmation, the expense must
  remain available and unchanged.
- If a user dismisses optional enrichment, the saved expense must remain complete and
  available for later review.
- If the user stops enriching an expense after providing only some details, each detail
  already provided must remain saved; additional optional details must not be required.
- A detail counts as provided only after the user confirms it. If the user leaves while
  editing a detail that has not been confirmed, that unfinished edit is not committed;
  previously confirmed details remain saved.
- While an expense is being enriched, its currency-formatted amount and capture time
  must remain visible with the details already saved, so the user can confirm which
  expense is being edited and what context it already has.
- If no expenses were captured in the selected period, the list must clearly show that
  it is empty while keeping amount capture available.
- Period filtering must use the device's local calendar so expenses around day, week,
  and month boundaries appear in the period that matches their capture time.
- If the user cancels choosing or attaching a receipt, no unrelated expense details may
  be lost.
- If an expense save or detail update fails, the app must distinguish the failed
  operation from a successful save and preserve unaffected data.
- If reminders are unavailable because notification permission is denied or withdrawn,
  capture remains fully usable and the app communicates the reminder limitation without
  repeatedly prompting.
- If multiple expenses are recorded close together with the same amount, each explicit
  save must remain a distinct expense.
- Every expense must retain the stable identity of the user profile that created it.
  Future family sync must preserve the source identity rather than reassigning an
  imported expense to the receiving device's primary user.
- If the app is used in an untranslated device language, user-facing capture text must
  fall back to English and amounts must remain understandable in the selected currency.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST provide direct access to a focused manual expense capture
  experience after primary profile setup.
- **FR-002**: The app MUST allow a user to save an expense by providing only a valid
  amount and an explicit save action.
- **FR-003**: The app MUST use the primary profile's selected currency for a new expense
  and MUST record the capture date and time with it.
- **FR-004**: The app MUST validate that an amount is present, greater than zero, within
  the supported range, and valid for the currency's supported fractional units before
  saving.
- **FR-005**: The app MUST clearly report invalid amounts and failed saves without
  showing a success state or discarding the amount being retried.
- **FR-006**: The app MUST save expenses on the device and make completed expenses
  available after the app is closed and reopened.
- **FR-007**: Expense capture and access to saved expenses MUST work without internet
  connectivity, and expense data MUST NOT be sent to public internet services or
  third-party analytics.
- **FR-008**: The app MUST keep vendor, Kakeibo category, and receipt image optional;
  none may block or delay saving an otherwise valid amount.
- **FR-009**: After saving, the app MUST allow a user to dismiss optional enrichment or
  add, change, and remove available details later without losing the saved expense. Each
  optional detail MUST be saved independently when the user confirms that detail, without
  requiring a final save for the complete enrichment flow. Leaving an unconfirmed detail
  MUST NOT discard any previously confirmed details.
- **FR-010**: The app MUST show saved expenses from the current day by default and allow
  users to view expenses from the current day, week, or month. Users MUST be able to
  select an expense from these lists for review or enrichment.
- **FR-011**: While a user reviews or enriches an expense, the app MUST keep a concise
  summary visible that presents the amount in the selected currency's familiar format,
  the capture time, and the optional details already provided, distinguishing saved
  details from details not yet provided.
- **FR-012**: The app MUST allow a user to explicitly correct a saved amount without
  creating a second expense, and MUST allow a user to delete a saved expense only after
  the user confirms the deletion.
- **FR-013**: The app MUST associate optional details with the expense the user chose and
  clearly report failed detail changes without falsely indicating success. If a user
  stops enrichment at any point, all details already provided MUST remain saved and
  available when the expense is reopened.
- **FR-014**: The app MUST support the Kakeibo categories Needs, Wants, Culture, and
  Unexpected when a user chooses to categorize an expense.
- **FR-015**: The app MUST keep reminders disabled unless the user enables them, allow
  the user to choose a daily reminder time, and allow reminders to be disabled at any
  time.
- **FR-016**: When reminders are enabled and permitted, the app MUST schedule no more
  than one discreet expense reminder per day and MUST provide a direct path to expense
  capture.
- **FR-017**: The app MUST keep expense capture available when notifications are denied
  or disabled and MUST NOT repeatedly prompt for notification permission.
- **FR-018**: Capture, saved expense details, validation feedback, and reminder controls
  MUST be operable with assistive technologies and without relying solely on color,
  precise gestures, or an internet connection.
- **FR-019**: User-facing capture and reminder content MUST be ready for localization;
  unsupported device languages MUST receive the English fallback, and currency amounts
  MUST be presented clearly.
- **FR-020**: Each saved expense MUST have a stable, unique identity that distinguishes
  it from other expenses even when their amounts and capture times match, and supports
  future merging between trusted household devices.
- **FR-021**: Each saved expense MUST record the stable identity of the profile that
  created it. This phase MUST attribute new expenses to the local primary profile;
  future trusted household synchronization MUST preserve that attribution and MUST NOT
  replace it with the receiving device's primary profile.

### Key Entities

- **Expense**: A distinct user-recorded outflow, including its amount, currency, capture
  date and time, the stable identity of the creating profile, and any optional details;
  it can be corrected or deleted after confirmation.
- **Expense details**: Optional vendor, Kakeibo category, and receipt image associated
  with one expense; details can be added, changed, or removed independently and remain
  saved even if the user stops before providing other details.
- **Expense period**: The current day, week, or month used to filter saved expenses for
  review, with the current day shown by default.
- **Reminder preference**: The user's choice to enable or disable a daily expense
  reminder and, when enabled, the selected reminder time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In usability testing, at least 90% of participants can save a valid
  amount-only expense on their first attempt without assistance.
- **SC-002**: In usability testing, the median time from opening capture to successful
  save for an amount-only expense is 10 seconds or less.
- **SC-003**: 100% of valid amount-only expenses in offline acceptance tests remain
  available after closing and reopening the app.
- **SC-004**: 100% of tested invalid amounts and failed save attempts avoid creating or
  falsely confirming an expense.
- **SC-005**: Users can complete capture without entering any optional detail, and
  dismissing enrichment never prevents access to the saved expense.
- **SC-006**: When opening expense review, users see today's saved expenses by default
  and can switch to current-week or current-month expenses to locate an expense for
  enrichment.
- **SC-007**: In usability testing, at least 90% of participants can correctly identify
  the expense being enriched from its visible currency-formatted amount, capture time,
  and already-provided details.
- **SC-008**: In all tested enrichment flows, 100% of details confirmed before the user
  leaves or stops remain saved without requiring a final save of the entire expense.
- **SC-009**: In all tested deletion flows, confirming deletion removes the expense from
  saved history, and canceling deletion leaves it unchanged.
- **SC-010**: Users who enable reminders can disable them in one visit to reminder
  settings, and no more than one reminder is sent per day.
- **SC-011**: Users can correctly identify the amount, currency, capture time, and any
  selected Kakeibo category during accessibility and localization checks.
- **SC-012**: In all tested repeated-capture cases, separately saved expenses remain
  individually identifiable even when their amounts and capture times match.
- **SC-013**: 100% of newly saved expenses retain the creating primary profile's stable
  identity after app restart, detail edits, and amount correction. Any future sync/import
  implementation preserves the source identity rather than attributing the expense to
  the receiving profile.

## Assumptions

- Phase 1 primary profile setup is available; each new expense uses that profile's
  selected currency. Changing currency for individual expenses is not required in this
  phase.
- A manual expense represents an outflow. Income, transfers, recurring transactions,
  budgets, and financial summaries are outside this phase.
- The device's local date and time at capture are used as the expense's capture time.
- A receipt image is optional and remains subject to the app's local-only financial
  data boundary; users may decline or cancel image selection.
- Daily reminders are off by default, are locally generated, and depend on user
  permission where required by the operating system.
- English is the initial shipped interface language, with English fallback for
  untranslated device languages.
- Expense history is limited to reviewing and enriching saved expenses within the
  current day, week, or month, and correcting individual saved expenses; reporting,
  budget analysis, and synchronization are not included.
- Phase 2 records the Phase 1 primary profile UUID as the creator identity. Family
  membership, member-name resolution, and cross-device synchronization are deferred to
  Phase 6; that phase must retain this source attribution when expenses are merged.
