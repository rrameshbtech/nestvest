# Dashboard UI Contract: Phase 3

This contract describes the native Android behavior corresponding to
[ui-mockups.html](../ui-mockups.html). The mockup is illustrative and uses synthetic
in-memory state; the feature specification, this contract, and real local data govern
the implementation.

## Entry and Navigation

- If no primary profile exists, show the existing profile setup/loading/error flow.
- After a profile is saved, show the dashboard as the ordinary app starting point.
- A reminder notification capture request continues to open the existing amount-first
  expense capture directly.
- Show an app bar identifying NestVest and indicating that data stays on this device.
- Show a welcome note personalized with the saved profile name, and a localized current
  date without synthetic sample-activity claims.
- Make the NestVest app-brand/logo action return directly to the dashboard from every
  post-profile screen, including capture, history, expense detail, reminders, More, and
  Customize. System Back may continue to follow established intra-expense navigation.
- The dashboard has a **Pinned for you** group, **More** group, and a **Customize**
  action. Place Customize in the Pinned for you heading row before the first tile.
- Selecting **Add expense** opens the existing amount-first capture. It does not add
  required details or embed expense history.
- Selecting **Expenses** opens the existing expense history, with Today selected when
  entering from the dashboard.
- Selecting **Reminders** opens the existing reminder settings. Permission and delivery
  behavior remain as implemented; reminders stay optional and local.
- Back navigation from an opened destination returns to the dashboard, except for
  established intra-expense navigation (for example, history to a selected detail and
  reminder settings back to history). Returning from those flows must preserve the
  existing Phase 2 screen behavior.

## Pinning and Customization

- Initially pin **Add expense** and **Expenses**; keep **Reminders** under **More**.
- All available destinations remain reachable whether pinned or not.
- **Customize** opens a list of all available options with explicit **Pin**/**Unpin**
  actions and a persistent status such as “Pinned on dashboard” or “Available under
  More.”
- Changes in Customize are a draft. **Save choices** commits the whole chosen set.
- On save success, return to the dashboard with the updated groups and show a brief,
  mild toast-style confirmation. Do not leave a persistent success message on screen.
- On save failure, retain the previous persisted pinned set, keep the user in
  Customize, and announce that the changes were not saved.
- If there are no unpinned destinations, hide the More section and its See all action
  on the dashboard. Do not imply that More has content.
- If a pinned destination becomes unavailable, do not navigate to a broken screen;
  communicate its unavailable state and preserve access to remaining destinations.

## Optional Expense Summary

- The Expenses tile may show a concise current-day count and currency-formatted total
  from saved local expense data.
- When no expenses have ever been recorded, state that no expenses have been recorded
  rather than showing a fabricated zero total. If prior history exists but none were
  recorded today, an accurate “Nothing spent today”/zero-today state may be shown.
- When summary data is unavailable or stale, say so and retain the Expenses action.
- Treat a summary as current only when it comes from the latest successful local expense
  read for the current device-local day. Refresh when the dashboard is shown/resumed and
  after the local date changes; never fall back to a previously cached amount after a
  read failure.
- Use the profile/expense currency and locale-aware formatting; do not combine
  unexpected different currencies into one total.
- A tile summary or badge is informational only and never gates navigation.
- Treat the mockup's displayed expenses and totals as synthetic examples only.

## Visual and Responsive Behavior

- Follow the mockup's restrained paper/ink/muted-green palette, flat bordered surfaces,
  generous spacing, compact descriptive tiles, and selective emphasis.
- Give each actionable dashboard tile a relevant leading icon. On the Expenses tile,
  show available or zero-today count and total as a concise trailing pill; keep
  no-history and unavailable states as readable text rather than a fabricated total.
- Preserve the project's minimum 48dp touch target expectations and clear focus
  indicators.
- Keep dashboard content scrollable on small screens and at enlarged font sizes; do not
  clip or overlap pinned and secondary options.
- Use actual localized strings and locale-aware dates and amounts; the static English
  mockup text is not a production string source.

## Accessibility and Feedback

- Give each tile and pin action an accessible name that includes its purpose and
  meaningful current state.
- Expose selected/pinned state and summary meaning to assistive technology; do not rely
  on color, shape, or icon alone.
- Keep reading and focus order aligned with the visual order: app bar, welcome, pinned
  heading and Customize action, pinned options, then More.
- Give the app-brand dashboard action a meaningful accessible name.
- Announce loading, transient save success, save failure, summary unavailable, and
  destination unavailable states using accessible status/error feedback.
- Provide usable keyboard, switch-access, and screen-reader navigation.

## Local Data and Failure Behavior

- Read and write preferences using the local app database, and derive summaries from
  local expense records.
- No dashboard operation requires internet connectivity or sends personal/financial
  data to public services.
- Preference-read failure must not falsely show that saved customization was applied;
  report the failure and offer a retry or a safe, clearly identified default view.
- Summary-read failure must not prevent opening a destination or show an old amount as
  current.
