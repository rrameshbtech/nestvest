# NestVest User Journeys

Cross-feature paths assembled from current feature specifications, mockups, and
repository implementation evidence. Status means implementation evidence in this
repository, not a claim that a build has shipped.

## Journey inventory

| Actor | Trigger and entry | Screens/actions and decisions | Exit, return, and recovery | Feature status and sources |
|---|---|---|---|---|
| New primary user | First app launch with no saved profile | Complete local profile setup; name and currency are required; appearance is optional; a failed save offers retry. On successful profile load/save, the current app route enters the dashboard. | Continue into the app; retry a failed operation; profile remains local. | **Implemented in repository.** [Phase 1 spec](../../specs/001-local-profile-foundation/spec.md), [mockup](../../specs/001-local-profile-foundation/ui-mockups.html), [ProfileRoute.kt](../../app/src/main/java/com/nestvest/profile/ui/ProfileRoute.kt), [MainActivity.kt](../../app/src/main/java/com/nestvest/MainActivity.kt). |
| Returning expense user | App opens with a saved profile | Ordinary launch opens the dashboard; **Add expense** opens amount-first capture. Save requires only an amount; optional details can be deferred. Expense history opens separately; reminder entry is available from history. | Keep capturing, open history, review/edit details, or return to dashboard. A reminder deep link opens capture directly. | **Implemented in repository.** [Phase 2 spec](../../specs/002-daily-expense-capture/spec.md), [mockup](../../specs/002-daily-expense-capture/ui-mockups.html), [ExpenseRoute.kt](../../app/src/main/java/com/nestvest/expense/ui/ExpenseRoute.kt), [ExpenseViewModel.kt](../../app/src/main/java/com/nestvest/expense/ui/ExpenseViewModel.kt), [MainActivity.kt](../../app/src/main/java/com/nestvest/MainActivity.kt). |
| Returning NestVest user | App launch after profile setup or return with a saved profile | Implemented dashboard presents pinned actions; **Add expense** opens the existing amount-first capture; **Expenses** opens existing history; **More** lists unpinned actions such as reminder settings; **Customize** pins/unpins actions. | Return to dashboard from a module; keep all available options accessible; failed customization leaves prior preferences intact. | **Implemented in repository.** [Phase 3 spec](../../specs/003-dashboard-navigation/spec.md), [mockup](../../specs/003-dashboard-navigation/ui-mockups.html), [MainActivity.kt](../../app/src/main/java/com/nestvest/MainActivity.kt). |
| NestVest user planning mindfully (proposed) | Choose Kakeibo from the dashboard tile when the feature is available; a gentle current-month planning prompt is also shown on the dashboard when no plan exists | On first use, optionally view interactive learning slides or dismiss the prompt; create or revise a calendar-month plan with slider/exact amount and an optional available total; optionally add weekly pacing; inspect category allocation, spend, remaining amount, health, and daily chart; explicitly assign eligible untagged expenses; optionally save a reflection. The dashboard tile summarizes categorized spend against the plan when current data is available. | Skip/dismiss or revisit learning; omit the overall amount; leave weekly pacing unset; cancel an assignment; postpone reflection; return to dashboard without changing expenses; retry failed plan, assignment, or reflection saves. No-plan, zero-spend, unassigned, other-currency, stale, unavailable, and overspend states remain distinct. | **Mockup-only proposal.** [Kakeibo spec](../../specs/004-kakeibo-planning-reflection/spec.md), [mockup](../../specs/004-kakeibo-planning-reflection/ui-mockups.html). The dashboard destination and Kakeibo summary integration are proposed; the existing dashboard mockup is unchanged. |

## End-to-end proposed flow — module and sub-module outline

### Profile setup — implemented

- **Entry:** First launch when no primary profile exists.
- **Main path:** Enter required name and currency; optionally set appearance; save
  successfully to open the Dashboard.
- **Branches and recovery:** Invalid input or save failure remains in profile setup;
  correct input or retry. Appearance may be skipped.
- **Exit/link:** Successful profile resolution opens the Dashboard.

### Dashboard — implemented; Kakeibo integration proposed

- **Entry:** Successful profile setup or ordinary app launch with a saved profile.
- **Main path:** Choose Add expense, Expenses, More, or Customize. The proposed
  Kakeibo tile invites planning if the current month has no plan; otherwise it shows a
  current summary. If data is unavailable, avoid stale/fabricated amounts and keep the
  destination accessible.
- **Expense capture sub-module:** Add expense opens Phase 2 amount-first capture.
  Invalid amount stays in capture; successful amount-only save preserves optional
  details as optional. Reminder notification requests also open capture directly.
- **History sub-module:** Expenses opens Phase 2 history. A user may open optional
  expense details, return to history, or add an expense. Dashboard is reachable from
  the established return action.
- **More/reminders sub-module:** More lists unpinned actions; Reminders opens local
  reminder settings; Back returns to Dashboard.
- **Customization sub-module:** Pin/unpin available options and save. Success returns
  to Dashboard; failure retains the previous persisted selection and remains in
  Customize. If there are no unpinned options, More is absent.
- **Exit/link:** Dashboard remains the ordinary launch destination; reminder deep-link
  capture bypasses it as specified.

### Kakeibo learning — proposed

- **Entry:** Choose Kakeibo from Dashboard. On first use, show a non-blocking prompt;
  after it is viewed or dismissed, show only the subtle Learn action on Kakeibo home.
- **Main path:** Navigate visual slides explaining the method, categories, and app use;
  finish to continue to planning.
- **Optional paths:** Back between slides, skip slides, dismiss the first-use prompt,
  or reopen the slides from the subtle action.
- **Failure/recovery:** Learning does not block the monthly plan; return to Kakeibo
  home and start or resume planning.
- **Exit/link:** Continue to the Monthly plan module; dashboard brand returns to
  Dashboard.

### Kakeibo monthly plan and weekly pacing — proposed

- **Entry:** Start a current-calendar-month plan from Kakeibo home or the no-plan
  dashboard prompt.
- **Main path:** Optionally enter an overall available amount; set savings intention
  and four category amounts using synchronized slider or exact entry; review totals;
  confirm plan.
- **Decision branches:** When an overall amount is supplied, show allocated and
  unassigned remainder and block over-allocation without changing values. When omitted,
  accept category planning without inventing a remainder. Monthly plan is the only
  planning period.
- **Optional weekly sub-module:** Add calendar-week amounts within each monthly
  category limit, or skip weekly pacing. Blank weeks mean zero. Over-limit weekly totals
  block that save and retain the draft.
- **Failure/recovery:** Plan-save failure keeps draft values and old plan, reports
  failure, and offers retry. Existing expenses are not altered by plan creation.
- **Exit/link:** Successful review/save opens monthly progress; back returns to
  planning or Kakeibo home.

### Kakeibo monthly progress and category detail — proposed

- **Entry:** Open progress for a month, including a month with no plan.
- **Main path:** Show allocation, same-currency categorized spend, remaining amount,
  health text, and per-category daily chart across the calendar month. Zero allocation
  and zero spend is “No allocation”; exact allocation is “At limit”; excess is negative
  remaining and “Over plan.”
- **Exception sub-module:** Show unassigned and other-currency expenses separately;
  never infer category or convert currency. With no plan, keep recorded expenses
  visible and offer plan creation without implying spend is within budget.
- **Assignment sub-module:** Select an eligible same-month, same-currency unassigned
  expense; confirm to assign. Success updates the category amount and chart. Cancel
  leaves it unassigned; failure reports the error and retains its unassigned state.
- **Empty/recovery states:** Zero-spend categories show zero and full remaining;
  empty chart is explicit. Changed or deleted saved expenses refresh progress when
  reopened.
- **Exit/link:** Return to monthly progress or Kakeibo home; start monthly reflection.

### Kakeibo monthly reflection — proposed

- **Entry:** Choose to reflect on a period from progress or Kakeibo home.
- **Main path:** Compare plan and current recorded amounts; optionally answer what went
  well, what to change, and what to improve next month; save and reopen separately from
  expense data.
- **Optional path:** Not now or leave before saving; plan and expenses remain unchanged
  and reflection can be completed later.
- **Failure/recovery:** Save failure keeps answers, reports failure, and permits retry.
  Later expense edits update current totals but do not rewrite saved answers.
- **Exit/link:** Saved reflection returns to period progress; dashboard brand returns
  to Dashboard.

## Integration notes

- Dashboard is the ordinary app starting point after profile setup and for returning
  users, matching the accepted choice and current `MainActivity` routing. Reminder
  notifications continue to request direct expense capture.
- The dashboard's **Add expense** action provides a direct route to the existing
  amount-first keypad. Saving still requires no category, vendor, or receipt; optional
  details remain skippable. Expense history and reminders retain their current
  terminology and local-only behavior.
- The existing reminder notification deep link requests expense capture directly. The
  Kakeibo mockup does not alter that behavior.
- Future planning, wealth, and family modules are not represented as available actions
  until their implementations exist.
- The Kakeibo study proposes adding a destination to the dashboard when the feature
  becomes available. The feature mockup depicts that transition without changing the earlier
  dashboard mockup; production availability and navigation remain implementation work.
