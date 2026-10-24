# Phase 3 Dashboard Mockup Review

## Workflow status

| Stage | Status | Note |
|---|---|---|
| Context | complete | Spec, constitution, Phase 1/2 specs, mockups, and app routing reviewed |
| Flow decision | complete | User chose dashboard launch with prominent Add expense (2026-10-24) |
| Design artifacts | complete | Mockup, design research, and journey map created |
| Browser verification | complete | 5 isolated journeys; 3 defects fixed and rechecked; Mermaid **unverified** (validator requires sign-in) |
| Reviewer A (spec fidelity) | complete | Fresh task `808848fe-a7fe-4595-b01a-7c650a1c1e60` (prior `9e6c2109` no longer exists); 5 findings assessed, 1 fixed and rechecked |
| Reviewer B (cross-feature) | complete | Fresh task `c2c64d80-d471-41ef-b12e-13040ad4ad0f` (prior `72ccc497` no longer exists); 4 findings assessed |
| Final review | complete | Partially verified: Mermaid syntax unverified; multi-locale, screen reader, and persistence are outside mockup verification |

Interrupted-run note: a resumed run launched duplicate reviewers `d9a4907d` and
`d708bac4`. They are superseded and not counted as the independent reviews.

## Scope and flow decision

- **Target:** [Phase 3 dashboard spec](./spec.md).
- **Prototype:** [Interactive dashboard mockup](./ui-mockups.html).
- **Approved flow choice (2026-10-24):** After profile setup and for returning users,
  launch to the dashboard; the prominent **Add expense** option opens the existing
  amount-first capture. Current repository behavior launches capture directly, so this
  is a Phase 3 navigation change, not a claim about current production UI.
- **Shared design guidance:** [NestVest design research](../../.specify/memory/design-research.md).
- **Journey integration:** [Cross-feature user journeys](../../.specify/memory/user-journeys.md).
- **Evidence status:** Phase 1/2 UI source is present in the repository; no release/device
  certification is inferred. Phase 3 is mockup-only.

## Spec coverage

| Spec requirement/scenario | Screen and state | Interaction or visible evidence | Verification status |
|---------------------------|------------------|----------------------------------|---------------------|
| US1.1; FR-001 | Dashboard | Default screen representing post-profile and return entry | Mockup only; onboarding transition represented in journey map |
| US1.2; FR-003 | Dashboard → Capture | One action opens amount keypad | Browser pass (J1) |
| US1.3; FR-002, FR-004, FR-006; SC-005 | Dashboard / More | All implemented actions are pinned or discoverable under More | Browser pass (J1, J2) |
| US1.4; FR-011 | Dashboard / Capture | Local-only cue and offline-independent prototype | UI cue only; offline behavior is not testable in this mockup |
| US2.1; FR-005 | Customize | Pin and unpin actions, with a full available-actions list | Browser pass (J2) |
| US2.2; FR-007 | Customize / dashboard | In-session preferences update and route back; prototype intentionally does not persist across refresh | Browser pass for in-session update (J2); real persistence not demonstrated |
| US2.3 | Dashboard | Default pinned actions: Add expense and Expenses (assumed default; spec does not define it) | Browser pass (J2, J3 reset) |
| US2.4; FR-006 | Customize / More | Unpin moves an action to More without deleting it | Browser pass (J2) |
| US3.1; FR-008 | Dashboard, normal state | Expenses tile shows count and total matching the synthetic history | Browser pass (J1) |
| US3.2; FR-008, FR-009 | Dashboard | No-summary state hides the number and keeps Expenses selectable | Browser pass (J3) |
| US3.3; FR-009 | Dashboard → Expenses | Summary is informational; selecting tile opens history | Browser pass (J1) |
| US3.4; FR-012 | All screens | Semantic headings/buttons, text badges, focus outline, non-color-only labels | Browser pass for roles, labels, focus, Tab order (J4); screen-reader check not performed |
| Edge: no extra options | More | Empty state when all available actions are pinned | Browser pass (J2) |
| Edge: pinned action unavailable | Dashboard, unavailable state | Disabled action labelled unavailable; other actions remain available | Browser pass (J3) |
| Edge: summary unavailable/stale | Dashboard, no-summary state | Removes numeric summary and retains the Expenses route | Browser pass (J3) |
| Edge: meaningful zero vs no data | Dashboard / history, "No expenses recorded" and "Nothing spent today" states | Absent data shows "No data"; meaningful zero shows `0 today · ₹0.00` with earlier history | Browser pass (A5 recheck) |
| Edge: preference save failure | Customize, save-failure state | Error says choices were not saved; prior pins stay unchanged | Browser pass (J2) |
| Edge: small screen/enlarged text | Responsive phone | Narrow layout, wrapping cards, scrollable content, visible focus | Browser pass at 360×780 and 1440×1000 (J4); text zoom not tested |
| FR-010 | Dashboard, no-summary state | Summary is omitted rather than presented as current | Browser pass (J3); staleness criteria unspecified in spec |
| FR-013 | Dashboard / history | Locale-aware date and currency formatting in sample prototype | Source review; multi-locale/RTL validation pending |
| SC-001 | Dashboard / More | One selection for pinned choices, two for unpinned choices | Browser pass: pinned = 1 selection, unpinned = 2 (More → item) (J1, J2) |
| SC-002 | Customize | Three actions can be pinned with three toggle controls | Usability timing not measured; participant test required |
| SC-003 | Overall | Calm hierarchy and visible actions | Not measurable from mockup alone; participant rating required |
| SC-004 | Dashboard / history | Sample badge total is derived from displayed sample entries | Browser pass (J1: 4→5, ₹1,725.00→₹1,965.00) |

## Design decisions and limitations

- Reuses the established system-sans, charcoal, muted sage, warm paper, thin border,
  and pale-green action language; see [shared design research](../../.specify/memory/design-research.md).
- Uses only the currently represented local actions: capture, expense history, and
  reminder settings. Future planned modules are not shown as available.
- The Phase 2 capture/history content is a small integration sketch; the full existing
  behavior remains in [Phase 2 mockups](../002-daily-expense-capture/ui-mockups.html).
- All data and actions are synthetic. Preference and expense changes exist only in page
  memory, reset on refresh, and do not represent real persistence or notification delivery.
- `FR-011`, production persistence in `FR-007`, localization coverage in `FR-013`,
  implementation-level behavior, and user research thresholds cannot be proven by this UI
  mockup. The browser checks below are not a production test or full accessibility audit.
- Mockup review does not modify the Phase 3 specification or application implementation.

## Browser verification

Run 2026-10-24 in the VS Code integrated browser (Playwright) against
`file://…/specs/003-dashboard-navigation/ui-mockups.html`, as isolated journeys with
container-scoped locators (`#pinned-list`, `#more-list`, `#customize-list`). Default
viewport 390×844.

| Journey | Check | Result |
|---|---|---|
| J1 Navigation | Load → dashboard only screen visible | Pass |
| J1 | `#pinned-list` Add expense → capture; footer reads "Save expense" | Pass |
| J1 | Save with empty amount → "Enter an amount greater than zero to save." | Pass |
| J1 | Enter 240 → save → "Saved on this device in this mockup. Details remain optional." | Pass |
| J1 | Back → dashboard; Expenses badge 4 → 5 today, ₹1,725.00 → ₹1,965.00 (SC-004) | Pass |
| J1 | Expenses tile → history; day/week/month toggles set `aria-pressed` | Pass |
| J1 | More → Reminders → toggle `aria-checked` false→true → back to dashboard | Pass |
| J1 | Footer on dashboard | **Defect found**: footer "＋ Add expense" visible on first load but hidden after returning (initial render skipped `showScreen`). Fixed: footer action hidden on dashboard from load; dashboard note consistent. Rechecked: hidden, note "Shortcuts are yours to choose." — Pass |
| J2 Customization | Customize lists Unpin Add expense / Unpin Expenses / Pin Reminders | Pass |
| J2 | Unpin Expenses → save → pinned [Add expense]; "See all (2)"; More lists Expenses, Reminders (FR-006) | Pass |
| J2 | Pin all → save → three tiles; More shows "There are no additional actions right now…" | Pass (first attempt failed from stale test handles after re-render; reran with fresh locators) |
| J2 | Save-failure state → unpin → save → error "Could not save these choices. Your previous dashboard is unchanged."; stays on Customize; pins unchanged | Pass |
| J3 States | No-summary → "Summary unavailable · open to review", tile enabled | Pass |
| J3 | No-data → dashboard "No expenses recorded yet" | Pass |
| J3 | No-data → history | **Defect found**: history still listed 4 sample entries. Fixed: preview-state change re-renders history. Rechecked: "Today · 0 expenses ₹0.00 … Nothing recorded in this period yet." and normal restores 4 — Pass |
| J3 | Module-unavailable → Expenses tile `disabled`, label "Expenses, unavailable", text "Temporarily unavailable"; click blocked; Add expense still enabled | Pass |
| J3 | Reset → state normal, default pins, "Mockup reset. No app data was changed." | Pass |
| J4 Responsive | 360×780: one column, phone 328px, no horizontal overflow | Pass |
| J4 | 1440×1000: two columns (phone + notes), phone 390px, no overflow | Pass |
| J4 Keyboard | Tab order: state select → Reset → Customize → Add expense → Expenses → See all → reference link; 3px focus outline on each | Pass |
| J4 | Enter on Add expense → capture; focus moves to "What did you spend?" heading | Pass |
| J4 Targets | In-phone buttons ≥44px | **Defect found**: "‹ Back to dashboard" 38px tall. Fixed `min-height: 44px`; rechecked 44px, no phone overflow — Pass. ("Reset mockup" 40px is a preview control outside the phone; accepted.) |
| J5 Phase 2 comparison (390×844) | Body bg rgb(233,236,229), font system-ui, phone 358px / 30px radius / paper rgb(250,251,247), primary button ink rgb(32,37,33) / 8px radius / 49px, in-phone title 29px weight 500 (Phase 2 `h3` line 46) | Pass — identical |
| Mermaid | `.specify/memory/user-journeys.md` flowchart via `mermaid-diagram-validator` | **Unverified** — validator returned "Sign in to Mermaid to validate diagrams." Not retried; syntax not claimed valid |

Not covered: screen-reader announcements, 200% text zoom, RTL/multi-locale, and
real persistence/offline behavior. No screenshot files were saved.

## Independent review A — specification fidelity

Task `808848fe-a7fe-4595-b01a-7c650a1c1e60` (fresh; prior task `9e6c2109` no longer
exists and could not be resumed). Source-only review; reviewer did not render the page.

| # | Severity | Finding (summary) | Origin | Disposition |
|---|---|---|---|---|
| A1 | High | FR-007 / US2.2: pins not retained after close/reopen (`spec.md:64-67,132-133`; mockup in-memory) | Spec gap / prototype limit | **Not a mockup defect** — the skill requires in-memory prototype state unless persistence is approved; already disclosed. Spec does not define the persistence mechanism (open question for planning); the handoff needs an acceptance test for retention |
| A2 | Medium | FR-001 / US1.1: profile-setup → dashboard handoff not modelled | Claimed mockup defect | **Rejected** — profile setup is Phase 1 and unchanged; the handoff is in the [journey map](../../.specify/memory/user-journeys.md) (`C → D`) and the mockup's flow notes. Duplicating Phase 1 screens would exceed the minimal screen set |
| A3 | Medium | Edge case "module unavailable": tile disabled but not moved or explained (`spec.md:97-99`) | Claimed mockup defect | **Rejected** — spec requires no broken navigation, clear handling, and other options still accessible; browser J3 confirmed all three (disabled, "Temporarily unavailable", Add expense enabled). Moving it to More is not specified |
| A4 | Medium | FR-013: INR hard-coded, copy English-only (`spec.md:141-143`) | Inherited convention | **Limitation, not fixed** — same as B1; the mockup uses locale-aware `Intl` formatting with a labelled sample currency. Multi-locale/RTL remains **not verified** |
| A5 | Low | FR-008/FR-010, `spec.md:105-106`: meaningful zero vs absent data not demonstrated; preview did not change the data source | Mockup defect | **Accepted and fixed** — the "No expenses recorded" state now shows "no expenses recorded yet" / "No data" (no ₹0.00); a new "Nothing spent today" state shows a meaningful `0 today · ₹0.00` with earlier week/month history; preview states drive the data source (saving in no-data shows `1 today · ₹50.00`). Browser recheck: pass, no page errors. Reviewer A recheck: **resolved** (line evidence `ui-mockups.html:324-329, 371-378, 442-452, 500-505`) |

## Independent review B — cross-feature consistency

Task `c2c64d80-d471-41ef-b12e-13040ad4ad0f` (fresh; prior task `72ccc497` no longer
exists). Source-only review; reviewer did not render pages (browser comparison above
covers rendering).

| # | Severity | Finding (summary) | Origin | Disposition |
|---|---|---|---|---|
| B1 | Medium | INR hard-coded (`ui-mockups.html:215`, `:314`) while app formats from `profile.preferredCurrency` (`ExpenseRoute.kt:46`, `ExpenseCaptureScreen.kt:45-52`) | Claimed introduced | **Rejected as defect** — mockup already labels "Sample profile currency · INR" (line 215), and Phase 2 mockup uses the same fixed INR sample (`002…/ui-mockups.html:54-56`, `:352`). Inherited prototype convention; implementation must use profile currency (FR-013) |
| B2 | Low | Mockup tokens (`#202521`, `#3d5844`…) differ slightly from production `Color.kt:5-8` (`#18211C`, `#365C46`…) | Inherited | **Noted, not fixed** — mockups match approved shared research; production drift predates Phase 3 |
| B3 | Low | Labels hard-coded rather than resource-backed/localized | Claimed introduced | **Implementation note** — HTML mockup cannot use Android resources; implementation should use string resources and localized plurals |
| B4 | Low | Numeric keypad keys lack explicit accessible names | Claimed introduced | **Rejected** — browser accessibility tree exposes "1"…"9", "0" from visible text, plus "Decimal point" and "Delete last digit" |

No in-scope fix required from Review B.

## Open questions for planning (not edited in spec)

- Default pinned set is not defined in the spec; the mockup assumes Add expense + Expenses.
- Persistence mechanism and retention test for pinned choices (FR-007).
- Staleness criteria for summaries (FR-010).
- Back-navigation and state preservation between the dashboard and modules.
