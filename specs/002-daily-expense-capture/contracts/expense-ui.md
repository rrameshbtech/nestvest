# Expense UI Contract: Phase 2

This contract describes native Android behavior corresponding to the four screen studies
in [ui-mockups.html](../ui-mockups.html). The mockup is illustrative; the feature
specification and this contract govern functional behavior.

## Entry and Navigation

- A configured profile opens directly to amount capture.
- The capture screen exposes a large locale-formatted amount, accessible numeric
  controls, a primary **Save expense** action, and a visually secondary
  **Add details now** action.
- Empty, malformed, zero/negative, excessive-precision, and out-of-range amounts do
  not save. Explain the correction inline, preserve the input, and announce feedback to
  assistive technology.
- **Save expense** stores amount, profile currency, capture instant, and the current
  primary profile's stable ID. Only the entered amount is cleared after a successful
  write; the profile identity is never cleared or changed.
- **Add details now** validates and persists the same amount-only record first, then
  opens that specific expense's detail screen. On write failure, remain in capture with
  the amount available for retry.
- History has an **Add an expense** action that returns to amount capture.
- Capture does not embed expense history. Its app-bar **Expenses** action opens history
  with Today selected. Keep Save expense and the subtle Add details now action in a
  fixed bottom footer, outside the scrolling keypad/content area.
- History app-bar settings control opens reminder preferences. Its return action
  returns to history, preserving selected period where practical.

## Expense History

- Opening history selects **Today** by default.
- **Today**, **This week**, and **This month** select mutually exclusive local-calendar
  ranges. The current period's rows are newest first.
- Each row presents currency-formatted amount, time, any known vendor/category/receipt,
  and a local date in week/month views. Selecting a row opens the exact expense.
- Each expense retains its creating profile ID as local provenance. Phase 2 has only
  the primary profile, so it need not add a redundant creator label to the current
  single-user history/detail UI. When household sync is implemented, the UI must
  resolve and show the originating member without reassigning imported records.
- A total and expense count summarize the selected period using the currently selected
  profile currency; expenses with another currency must not be combined as if equal.
  Since Phase 2 fixes a new expense's currency to the profile currency, mixed-currency
  totals are not expected unless external migration/data exists; such records should be
  shown separately, not silently converted.
- Empty history communicates that no entries exist in that period and leaves the add
  action available.
- Follow mockup screen 02: brand/local-data app bar, spending eyebrow, period heading
  with local date, equal-width period controls, count and right-aligned currency totals,
  and compact icon-led rows with vendor/context on the left and amount on the right.
  Keep **Add an expense** in a fixed bottom footer even when the list is long.

## Expense Detail and Confirmed Enrichment

- Detail screen keeps the selected expense's currency-formatted amount and local
  capture date/time visible while details are edited.
- Saved vendor, category, and receipt state are distinguishable from absent details.
- A saved vendor pill reads **At • {vendor}**; an absent vendor keeps **No place yet**
  without the prefix.
- Vendor edits are draft text while focused. Trim and persist the vendor when the field
  loses focus or the user explicitly leaves the detail screen; blank text clears the
  vendor. Choosing **Done for now** while the vendor field is focused confirms it by
  moving focus; wait for persistence before returning to history. There is no separate
  vendor **Save** CTA. If persistence fails, retain the draft, remain on the detail
  screen, and report failure.
- Selecting a Kakeibo category confirms and saves it immediately; selecting the active
  category again clears it.
- Choosing a receipt confirms that one receipt selection. Copy it locally before
  reporting success. Cancellation or copy failure leaves other expense fields intact.
- An explicit amount correction updates the selected expense rather than creating
  another record.
- **Done for now** returns to history; confirmed details remain saved. An unconfirmed
  vendor draft is not committed unless leaving the field or choosing **Done for now**
  confirms it.
- **Delete this expense** requires a confirmation step. Confirm removes it from history;
  cancel leaves it unchanged.
- Follow mockup screen 03: pale bordered summary card with saved/absent-detail pills,
  rectangular two-column category controls, bordered receipt row, and a dark fixed-bottom
  **Done for now**. Keep the amount/time summary pinned above the scrolling form.
  On short viewports or large fonts, omit the decorative heading and make the pills
  horizontally scrollable so the form and primary action remain usable.

## Visual Tokens

Use the mockup's paper `#fafbf7`, ink `#202521`, muted `#626a62`, green `#3d5844`,
pale green `#e7eee2`, divider `#dce1d8`, summary fill `#f4f7f1`, and error `#793e2d`.
Use 8–12dp corner radii instead of default pill-shaped Material buttons, flat bordered
cards without elevation, and at least 48dp interactive touch targets. Category icons are
decorative companions to textual category labels; color/icon alone must not convey state.

## Reminder Preferences

- Entry point is the settings icon in the history app bar.
- Follow mockup screen 04 using the shared brand/local-data app bar, paper background,
  eyebrow and two-line heading, flat bordered settings card, muted off-state switch,
  full-width time control, inline permission guidance, and fixed bottom **Back to
  expenses** action. Content scrolls independently so return remains available at large
  font sizes. Keep 48dp touch targets and show checked state without relying on color.
- Daily reminders are off by default. Enabling exposes a local time choice; disabling
  cancels future scheduling.
- If notification permission is denied or revoked, explain that reminders are
  unavailable without repeated permission prompts. Capture and history continue to work.
- Reminders are local and no more than one is delivered per local day. The app does not
  claim exact delivery if Android defers background work.

## Accessibility and Localization

- All actions have meaningful accessible names; period selectors expose selected state;
  category choices expose selected state; the reminder switch exposes checked state.
- Focus order follows screen reading order. Errors and save outcomes are announced as
  status or error feedback.
- Controls remain operable with keyboard, switch access, and screen readers; meaning is
  not conveyed by color alone.
- Currency, amount precision, dates, week boundaries, and user-facing strings use
  locale-aware formatting/resources, with English fallback for untranslated languages.
