# Phase 3 Kakeibo Mockup Review

**Feature**: [spec.md](./spec.md)  
**Mockup**: [ui-mockups.html](./ui-mockups.html)  
**Shared design rules**: [design-research.md](../../.specify/memory/design-research.md)  
**Cross-feature journey**: [user-journeys.md](../../.specify/memory/user-journeys.md)  
**Review date**: 2026-10-24

## Workflow status

| Stage | Status | Notes |
|---|---|---|
| 1. Context | complete | Re-read updated spec and current prototype; no unresolved spec question affects the mockup. |
| 2. Flow and design decisions | complete | User approved replacing mandatory Mermaid with a module/sub-module text flow. Journey map refreshed; no unresolved visual/behavioral decision. |
| 3. Build | complete | Prototype includes interactive learning slides, optional overall amount, synchronized category sliders/exact fields, dashboard summary states, category daily chart/health, and explicit unassigned assignment. Coverage matrix refreshed below. |
| 4. Pre-check | complete | `node --check` passed; static IDs are unique and references resolve (dynamic assignment checkbox excluded); initial/navigation render path and dependent screen renderers are present; in-app controls meet the 44px minimum; absent/zero/unavailable previews are distinct. |
| 5. Browser verification | complete | Browser checks J1–J5 and the revised module/sub-module text-flow completeness check are complete. Mermaid validation is no longer required by the approved workflow. |
| 6. Independent reviews | complete | Reviewer A (spec fidelity): `d44edf39-7165-49b4-8d60-de0e7aa16685`; accepted findings A1/A2 fixed and rechecked resolved. Reviewer B (cross-feature consistency): `043fd890-4621-4187-8c6c-276fc66d6101`; focus finding rejected with evidence; reference-link finding fixed and rechecked resolved. |

### Context digest

| Source | Observed date | Relevant finding |
|---|---|---|
| [spec.md](./spec.md) | 2026-10-24 11:53 | Updated requirements include monthly-only planning; optional overall amount; synchronized slider and exact entry; category daily charts and health; explicit uncategorized assignment; dashboard nudge/summary; interactive first-use learning. |
| [ui-mockups.html](./ui-mockups.html) | 2026-10-24 10:54 | Existing mockup predates those additions and contains a single learning screen, required overall amount, no category detail/assignment flow, and no dashboard summary tile. |
| [design-research.md](../../.specify/memory/design-research.md) | 2026-10-24 10:55 | Established paper/ink/sage tokens and local-only prototype constraints remain applicable. |
| [user-journeys.md](../../.specify/memory/user-journeys.md) | 2026-10-24 10:55 | Launch path and dashboard status are stale relative to current app source and accepted dashboard-first behavior. |
| [constitution.md](../../.specify/memory/constitution.md) | 2026-10-24 | Local-first privacy, honest financial presentation, accessibility, localization, and Kakeibo principles apply. |
| [MainActivity.kt](../../app/src/main/java/com/nestvest/MainActivity.kt) | 2026-10-24 | Normal post-profile entry renders DashboardRoute; reminder capture requests route directly to expense capture. |
| [003-dashboard-navigation/ui-mockups.html](../003-dashboard-navigation/ui-mockups.html) | 2026-10-24 | Existing dashboard study establishes the visual baseline; it is not modified by this workflow. |
| [002-daily-expense-capture/ui-mockups.html](../002-daily-expense-capture/ui-mockups.html) | 2026-10-24 | Existing amount-first capture remains a linked, unchanged feature. |

**Current → proposed flow:** Profile setup or ordinary return opens the implemented dashboard. The Kakeibo mockup proposes a monthly-planning destination and dashboard tile; it must show current-month planning, category progress/detail and explicit assignment, optional reflection, and learning without changing expense-capture behavior.

**Open spec questions:** None identified that materially block the mockup.

## Scope and integration

The mockup covers Kakeibo introduction, monthly planning, optional weekly pacing,
progress, category details/assignment, dashboard summary, and reflection. Kakeibo is
shown as a proposed destination on the implemented post-profile dashboard. It leaves
the Phase 1 profile and Phase 2 expense screens/mockups unchanged.

All amounts and expenses are synthetic INR sample data. Plan, weekly pacing, and
reflection saves are held in page memory only; no actual data is read, written, or
transferred. This prototype cannot demonstrate offline persistence, at-rest protection,
database arithmetic, or actual expense recalculation.

## Requirement-to-screen coverage

| Spec requirement/scenario | Screen and state | Interaction or visible evidence | Verification status |
|---|---|---|---|
| US1-AS1 | Kakeibo home, first-use prompt | Optional prompt opens three visual learning slides | Browser verified |
| US1-AS2 | Learning slides | Back, next, finish, and skip controls | Browser verified |
| US1-AS3 | Kakeibo home after dismissal | Prompt hidden; subtle Learn action remains available | Browser verified |
| US1-AS4 | Slides and planner | Skip/finish leads to planning without a tutorial gate | Browser verified |
| US2-AS1 | Monthly planner | Savings intention and four category allocations | Browser verified |
| US2-AS2 | Planner/review | Category total, savings, remaining amount, and over-allocation validation | Browser verified |
| US2-AS3 | Planner/review | Overall amount can be omitted; no invented remainder | Browser verified |
| US2-AS4 | Planner | Category slider and exact field update each other | Browser verified |
| US2-AS5 | Review/update | Confirm replacement or return to editing | Browser verified |
| US2-AS6 | Planner/save | Plan creation retains existing synthetic expenses | UI concept; real record integrity not demonstrable |
| US2-AS7 | Device/local cue | Offline availability described, not simulated | Not demonstrable |
| US2-AS8 | Plan failure preview | Draft retained; no false success; retry available | Browser verified |
| US3-AS1 | Weekly pacing | Calendar-week category amounts remain within monthly limits | Browser verified |
| US3-AS2 | Weekly validation | Excess weekly sum blocks save with explanation | Browser verified |
| US3-AS3 | Weekly pacing | Skip weekly pacing while monthly plan remains | Browser verified |
| US3-AS4 | Weekly pacing | Partial locale-week dates shown explicitly | Browser verified; device locale behavior not simulated |
| US4-AS1 | Category detail | Allocation, spend, remaining, textual health, daily chart | Browser verified |
| US4-AS2 | Overspend preview | Negative remaining and explicit Over plan label | Browser verified |
| US4-AS3 | Progress exceptions | Unassigned amount separate from category totals | Browser verified |
| US4-AS4 | Category detail | Synthetic daily chart and amount reflect sample expense state | UI demonstration; real expense edits/deletes not demonstrable |
| US4-AS5 | Progress exceptions | Other-currency amount separately shown, not converted | Browser verified |
| US4-AS6 | Category detail/assignment | Select and confirm eligible same-month INR expense | Browser verified |
| US4-AS7 | Assignment failure/cancel | Remains unassigned; error or cancel leaves progress intact | Browser verified |
| US4-AS8 | No-plan progress | Expenses remain visible and create-plan action available | Browser verified |
| US4-AS9 | Reflection | Compare plan/spend and enter three reflection answers | Browser verified |
| US4-AS10 | Reflection | Not now returns without changing plan/expenses | Browser verified |
| US4-AS11 | Saved reflection | Reopen answers separately from current expense totals | Browser verified |
| US5-AS1 | Dashboard, no-plan state | Kakeibo tile gently invites monthly planning | Browser verified |
| US5-AS2 | Dashboard, plan state | Spend/plan summary with text health | Browser verified |
| US5-AS3 | Summary-unavailable preview | No stale/fabricated amount; tile remains accessible | Browser verified |
| US5-AS4 | Dashboard after plan save | Nudge replaced by available summary | Browser verified |
| FR-001 | Learning/category views | Canonical categories and examples | Browser verified |
| FR-002 | Learning | Visual slides, navigation, and skip path | Browser verified |
| FR-003 | Learning preference | One-time prompt then subtle home action | Browser verified; durable preference is simulated in memory |
| FR-004 | Planner | Calendar-month-only plan | Browser verified |
| FR-005 | Planner | Optional overall available amount | Browser verified |
| FR-006 | Planner | Synchronized slider/exact values and allocation total | Browser verified |
| FR-007 | Planner/review | Remaining balance; prevent over-allocation without changing entries | Browser verified |
| FR-008 | Planner | Omitted total accepted and no remainder shown | Browser verified |
| FR-009 | Review and saved plan | Confirm updates, monthly period identified | Browser verified |
| FR-010 | Weekly pacing | Optional breakdown constrained by monthly limits | Browser verified |
| FR-011 | Progress/detail | Same-currency planned/spent/remaining and overspend | Browser verified |
| FR-012 | Category detail | Full-month daily chart with dates and amounts | Browser verified |
| FR-013 | Category detail/progress | Text health: Within plan, At limit, Over plan, No allocation | Browser verified |
| FR-014 | Progress exceptions | Uncategorized and other-currency amounts separate | Browser verified |
| FR-015 | Assignment | Explicit selection, confirmation, cancel, and failure | Browser verified |
| FR-016 | Progress/chart | Recomputed from synthetic category/day data | UI demonstration; actual saved expense refresh not demonstrable |
| FR-017 | Reflection | Compare values and enter three prompts | Browser verified |
| FR-018 | Reflection | Postpone without changing plan or expenses | Browser verified |
| FR-019 | Dashboard | Gentle no-plan prompt; summary with constrained-category health | Browser verified |
| FR-020 | Dashboard summary-unavailable | Missing data distinct from zero; destination remains usable | Browser verified |
| FR-021 | Local/privacy cue | On-device intent visible | Secure storage, offline persistence, and network behavior not demonstrable |
| FR-022 | Failure previews | Plan/reflection/assignment errors preserve draft and avoid false success | Browser verified |
| FR-023 | All screens | Planned vs actual distinction and non-judgmental copy | Browser verified |
| FR-024 | All screens | Semantic labels, keyboard operation, textual state; production AT/localization not demonstrable | Partially demonstrable |
| Edge: negative amounts/over-allocation | Planner validation preview | Prevent continuing; preserve values | Browser verified |
| Edge: zero category allocation | Planner/category detail | Zero allocation remains valid | Browser verified |
| Edge: expenses predate plan | No-plan then create plan | Sample expenses remain visible after planning | UI concept; real recalculation not demonstrable |
| Edge: unassigned/foreign currency | Progress exceptions | Separate values, no inferred category or conversion | Browser verified |
| Edge: reflection after expense change | Reflection and saved reflection | Current totals and saved answers stay distinct | UI concept; real expense update not demonstrable |
| Edge: save failures | Plan/reflection/assignment failure states | Retain entered values and show recoverable error | Browser verified |
| Edge: calendar boundaries | Weekly/monthly labels | Explicit October dates; actual locale boundaries not simulated | Partially demonstrable |
| Edge: zero spending | Empty preview | Zero spend, full remaining, honest empty chart | Browser verified |
| Edge: zero allocation and spend | Unexpected category detail | No allocation, not false positive health | Browser verified |
| Edge: zero allocation and positive spend | Zero-allocation category sample | Over-plan state | Browser verified |
| Edge: complete daily chart | Category detail | Oct 1–31 represented, including zero days | Browser verified |
| Edge: assignment eligibility/cancel/failure | Category assignment | Only same-month INR expense; cancel/failure leaves unassigned | Browser verified |
| Edge: dashboard prompt | Dashboard no-plan | In-app only; no push-notification implication | Browser verified |
| Edge: learning prompt | Kakeibo home | Dismiss/view once per mockup session; subtle link remains | Browser verified; durable preference not demonstrable |
| Edge: expense period date | Month labels | Uses sample saved-date month; device-local boundary behavior not simulated | UI concept only |
| SC-001 | Planner | Review and save flow present; user-study percentage not measurable here | UI only; study required |
| SC-002 | Progress | Four category summaries on one screen | Browser verified; timed study not performed |
| SC-003 | Reflection/progress | Planned and recorded values presented together | Browser verified; comprehension study not performed |
| SC-004 | Save states | Temporary in-memory saves | Persistence after restart/offline not demonstrable |
| SC-005 | Progress | Matching-category amounts and exception separation | UI illustration; backend accuracy not demonstrable |
| SC-006 | Weekly pacing | Weekly and monthly limit displayed together | Browser verified; comprehension study not performed |
| SC-007 | Planner | Both slider/exact entry and total shown | Browser verified; user-study percentage not measurable here |
| SC-008 | Assignment | Confirmed assignment updates category total/chart in sample state | Browser verified; real database update not demonstrable |
| SC-009 | Dashboard | Prompt and post-save summary states | Browser verified |
| SC-010 | Learning | Find/complete/skip/revisit paths | Browser verified; user-study percentage not measurable here |

## Browser verification

**Browser page:** `6c87d1c7-7bb7-4e38-b365-4c4d54da6df1`, local `file://` page. No page errors were observed.

- **J1 Navigation — verified, 390×844:** Dashboard opened Kakeibo; the first-use prompt opened the three-slide learning flow; planning was reachable from slides. Optional overall amount could be omitted and showed no invented remainder. Exact Wants entry `₹9,500` synchronized to its slider. Review/save worked; an available amount of `₹30,000` blocked an over-allocated plan and announced the shortfall.
- **J2 Feature interactions — verified, 390×844:** A blank weekly amount saved as zero; a Needs weekly total above its `₹16,000` monthly limit was rejected; weekly pacing could be skipped. Assigning an eligible `₹750` expense changed Needs spent from `₹12,300` to `₹13,050` and updated the chart; assignment failure retained selection and cancel left it unassigned. Slider/exact output later matched at `₹8,750.25`.
- **J3 States — verified, 390×844:** No-plan prompt and create-plan action; overspend `-₹2,500` / “Over plan”; unassigned and `€20` separate; empty category with zero spent/full remainder; unavailable dashboard summary; plan/reflection/assignment failures; zero allocation with zero spend (“No allocation”) and with positive spend (“Over plan”); 31 daily chart bars; reflection save/reopen and postpone; learning prompt dismissal/revisit and reset all exercised.
- **J4 Responsive & keyboard — verified:** 320×640, 390×844, and 1440×900. After correction, at 320px content width and scroll width were both 286px; weekly inputs measured 108×44px and no small phone buttons were found. At 390px content had no horizontal overflow; at 1440px the board was 1440px with a 388px phone. Navigation focus moved to `kakeibo-title` and `progress-title`.
- **J5 Comparison — verified at 390×844:** Phase 1/2/dashboard/Kakeibo background was `rgb(233,236,229)`; phone max width 390px, radius 30px, paper `rgb(250,251,247)`; Kakeibo primary button 49px high with 8px radius and ink background; title 29px/500. Keyboard focus outline matched Phase 2/dashboard at 3px `rgb(83,117,78)` with 3px offset. Phase 1 retains its documented 4px offset and 32px title.
- **Text journey flow:** Verified against the updated skill checklist: all six modules identify entry, main path, decisions/branches, optional/skip paths, failure/recovery, exits/back destinations, cross-module links, and implementation status. Journey inventory agrees on profile → dashboard, direct reminder capture, proposed Kakeibo destination, and Kakeibo return paths.
- **Mermaid:** Not used or required under the user-approved workflow change. The previous validator retry was interrupted without a result; no local validator is installed.

## Independent reviews

Two fresh-context read-only reviews returned before browser validation; both were
source-based, not rendered. Reviewer A task:
`d44edf39-7165-49b4-8d60-de0e7aa16685`. Reviewer B task:
`043fd890-4621-4187-8c6c-276fc66d6101`. Scoped rechecks completed after the listed
changes; their outcomes are recorded below.

## Review findings and dispositions

| Reviewer | Finding | Evidence | Disposition | Recheck |
|---|---|---|---|---|
| A | Blank weekly amounts were described as allowed but rejected. | FR-010, US3-AS3; `ui-mockups.html:600-610` | Accepted; blank fields now contribute zero while negative values remain invalid. | Resolved; Reviewer A rechecked lines 600-610 |
| A | Slider/exact value lacked an adjacent announced amount. | FR-006; `ui-mockups.html:318, 490, 869-885` | Accepted; added a formatted live output for each category value. | Resolved; Reviewer A rechecked controls, output, and handlers |
| B | New-screen focus allegedly remained on the activating control. | `ui-mockups.html:543` | Rejected; `showScreen()` focuses the new screen's `h2` after rendering and visibility changes. Browser focus was also observed on Kakeibo and progress headings. | Not applicable |
| B | Expense reference link did not clearly state it was a reference. | `ui-mockups.html:251` | Accepted; link now says “View amount-first capture reference.” This feature mockup does not replace the existing capture screen. | Resolved; Reviewer B rechecked line 251 |
| B | 390px phone frame differed from established radius. | `ui-mockups.html:45, 186` | Accepted during visual comparison; responsive breakpoint now retains the 30px baseline at 390px and only reduces it on narrower devices. | Resolved; Reviewer B rechecked line 45 |
