---
name: "speckit-mockup"
description: "Create or update interactive UI mockups from a Spec Kit spec.md and optional design guidance. Use for feature mockups, screen flows, visual workflows, user journey maps, design research, and visual or behavioral consistency with existing features."
argument-hint: "<path/to/spec.md> [additional mockup guidance]"
compatibility: "Requires a Spec Kit project, browser tools for rendered verification, and subagents for independent reviews."
---

# Spec Kit UI Mockups

Purpose: a verified visual and behavioral reference for one spec that reuses the
established design and fits the cross-feature journey. Not production UI. Run only
when asked to create or update a mockup.

## Input

```text
$ARGUMENTS
```

- Target = explicit spec path or feature directory containing `spec.md`. Else run
  `.specify/scripts/bash/check-prerequisites.sh --json --paths-only` from the repo
  root and use `FEATURE_DIR`/`FEATURE_SPEC`; confirm the spec exists. If the script
  fails or several candidates fit, list them and ask. Never guess the newest feature
  or create a branch.
- Guidance = the rest of the user message and attachments. Attachments are
  references, not the target, unless stated.

## Terms and files

| Term | Default path | Holds |
|---|---|---|
| **design memory** | `.specify/memory/design-research.md` | Approved rules/tokens, dated decisions, accepted prototype limitations, rendered comparison map, tool availability |
| **journey map** | `.specify/memory/user-journeys.md` | Cross-feature journey inventory, module/sub-module text flow, per-feature status |
| **mockup** | `<FEATURE_DIR>/ui-mockups.html` | Self-contained interactive screens and states |
| **ledger** | `<FEATURE_DIR>/mockup-review.md` | Stage status, context digest, coverage table, browser results, reviews, open questions |

Reuse existing equivalents instead of creating a second source of truth. Keep an
existing mockup's format and location unless the user approves a migration.
Templates for the ledger, reviewer briefs, browser harness, and report are in
[templates/](./templates/); load each only at the stage that needs it.

## Hard rules

1. MUST create or change only the mockup, its ledger, design memory, and the journey
   map. MUST NOT edit specs, plans, tasks, constitution, feature selection, app code,
   or other features' mockups. Record gaps as ledger open questions.
2. MUST preserve existing user edits and findings; ask before replacing conflicting work.
3. Authority order: constitution → target spec → approved design memory and
   implemented UI → user guidance. MUST ask (structured question tool) before a
   material visual/behavioral choice that conflicts with a higher source or fills an
   unresolved spec clarification. Record approved exceptions with scope and date.
4. MUST reuse established tokens, components, and interaction patterns. No new theme.
5. Mockup data is synthetic and in memory. No network, remote fonts, CDNs, analytics,
   or persistence without user approval. Never imply a mock save changes real data.
6. Do not invent business rules or make optional steps mandatory.
7. Current vs proposed behavior differing is a recorded delta, not a contradiction.
   A contradiction requires two sources disagreeing about the same state.
8. No claim without evidence: rendering, interaction, visual consistency, and diagram
   validity need tool output; source inspection alone never proves them. If a tool
   is unavailable or fails, mark the item **unverified** / **not verified**.
9. Exactly one reviewer per role per workflow. Record its task ID in the ledger at
   launch. On resume, use recorded IDs; never relaunch a reviewer that may still be running.
10. Never send private specs, code, screenshots, or data to external services.
11. Never report complete while any stage is `pending`, `in progress`, or `blocked`,
    or while required behavior or a material design conflict is unresolved.
    A blocked stage does not stop independent stages; finish them and report the
    resume point.
12. If a needed source is inaccessible, report it; do not reconstruct its contents.

Statuses: `pending | in progress | complete | blocked` (blocked needs a reason).
The ledger tracks stages 1–6; stage 7 is the final response, sent only when the
ledger permits it under rule 11.

## Run mode

| Situation | Scope |
|---|---|
| No ledger | New mockup: every stage. Create the ledger from [templates/mockup-review.md](./templates/mockup-review.md). |
| Ledger with incomplete stages | Resume at the first incomplete stage. Do not redo complete stages. |
| Ledger complete; user requests changes | Update: rerun only affected coverage rows and journeys; send responsible reviewer a scoped recheck. Full re-review only if screens are added, navigation changes, or shared design rules change. |

Legacy ledgers: map older stage names to the current ones and keep their history.
Confirm an existing ledger belongs to the resolved spec before resuming from it.
If a recorded reviewer task no longer exists, record that in the ledger, then
launch exactly one replacement for that role.
Write ledger updates once per stage, and before any user question, browser batch,
or reviewer launch. Give the user one short update per stage transition or block.

## Stages

### 1. Context

Skip if: the ledger's context digest exists and no listed source has a newer
modification/commit date. Re-read only changed sources.

- Read in one batch: applicable repository instructions, target spec (all of it),
  constitution, design memory, journey map, neighboring feature mockups (source),
  and the target's plan/research/data-model/contracts/checklists if present. A plan
  or tasks file is not required.
- Read app source only for entry points and screens the target changes or links to.
- Feature status (`implemented | mockup-only | planned | unknown`): take it from the
  journey map; re-check only features whose files changed. A spec, task list, or
  mockup alone is not proof of implementation.
- Exit: ledger **context digest** = sources with dates, current→proposed flow delta,
  open spec questions.

### 2. Flow and design decisions

- Ask only if one of these is true: unresolved spec clarification affecting UI;
  same-state contradiction; inaccessible entry point; change to existing
  implemented behavior whose intended transition is unclear. Otherwise record the
  delta and continue.
- Journey map: one Markdown inventory table plus a text-based end-to-end flow
  organized by module and sub-module. Do not require Mermaid diagrams or Mermaid
  validation. Include actor, trigger, entry, screens/actions, decisions, exits/back,
  validation failures, optional/skip paths, empty/error/recovery states, links, and
  per-feature status. Trace at least one complete journey through existing and new
  features. Mark the target's additions as proposed, and unresolved proposals as
  pending.
- Validate the text flow with a completeness check: each module names its entry,
  main path, decision branches, optional/skip paths, failures and recovery, exits/back
  destinations, cross-module links, and implementation status; check that links
  between modules agree with the inventory table and approved behavior. Record any
  missing path as an open question rather than inferring product behavior.
- Design memory: change only what changed. Dated decisions with rationale and scope;
  mark superseded entries. Separate verified source facts from recommendations.
  Research externally only for a specific unresolved question; cite sources with
  access dates; never fabricate research. Feature-specific rationale goes in the ledger.
- Add an **accepted prototype limitation** only with user approval or when an
  existing prototype constraint already implies it. Never use one to excuse
  behavior the spec requires the mockup to show.
- Exit: decisions recorded; journey map updated.

### 3. Build

- Fill the ledger coverage table: every UI-relevant FR, acceptance scenario, edge
  case, and measurable UI criterion, with original IDs. Non-demonstrable items get
  a reason, not a pass.
- Smallest set of screens/states covering the table. On updates, keep unchanged
  screens intact.
- Self-contained HTML (inline CSS/JS, local assets, no build step), labelled as a
  mockup of the named feature. Feature-only assets live beside it via relative links.
- Demonstrate: navigation, back/cancel, validation, save feedback, optional/skip,
  every specified empty/error/unavailable state, reset. No inert meaningful controls;
  no alerts in place of flows. Label simulated capabilities.
- Preserve specified calculations, units, terminology, and data relationships; use
  boundary examples.
- Accessibility: semantic controls, labels, keyboard access, visible focus, targets
  ≥44×44 px, non-color-only feedback, focus moved to the new screen heading after
  transitions; text expansion; localization/RTL where specified.
- Testability: stable unique `id`/`data-*` hooks; each preview state loadable via
  `?state=<name>` and a visible control; one render function per screen called on
  load, navigation, and every dependent state change.
- Exit: coverage table filled; mockup exists.

### 4. Pre-check (source, before browser)

Fix any failure before stage 5:

- Initial load uses the same render path as navigation.
- Every data/preview-state change re-renders every screen showing that data;
  preview states change the sample data, not only labels.
- Absent data, meaningful zero, and unavailable are distinct where the spec distinguishes them.
- In-app controls ≥44×44 px.
- Extracted inline script passes `node --check`.

### 5. Browser verification

Use [templates/browser-harness.js](./templates/browser-harness.js) as the prefix of
every script. One page per mockup; navigate instead of opening duplicates.

| Call | Covers |
|---|---|
| J1 Navigation | Entry, primary flows, validation, save feedback, back/cancel, calculations with boundary values |
| J2 Feature interactions | Feature-specific editing/customization/settings |
| J3 States | Every preview, empty, error, unavailable, and reset state |
| J4 Responsive & keyboard | Target mobile and wide viewports, overflow, Tab order, focus after transitions, target sizes |
| J5 Comparison | Computed styles for every row of the design memory's rendered comparison map, all mockups, same viewport |

- Locators scoped to a container and unique. Re-query after re-render.
- Each assertion records its own result; dependent checks become `blocked`, not skipped.
- At most one targeted retry per failed call. After a fix, rerun only affected journeys.
- Text journey flow: run the module/sub-module completeness check from Stage 2 and
  record its result in the ledger. No diagram renderer or syntax-validation tool is
  required.
- Record viewport sizes and observed values per check in the ledger. No full
  accessibility audit claims. Avoid screenshot/log files unless useful as evidence.
- No browser tools: complete source checks; mark rendering, interaction, and
  comparison **not verified**.

### 6. Independent reviews

Launch two read-only subagents in parallel as soon as stage 4 passes, using
[templates/reviewer-a.md](./templates/reviewer-a.md) and
[templates/reviewer-b.md](./templates/reviewer-b.md). Fill in exact paths and the
accepted limitations; never include the ledger, your rationale, or the other
reviewer's output. Record task IDs immediately, then run stage 5 while they work.
Reviewers may render the mockup if they have browser tools; source-only review is
acceptable because rendered evidence comes from stage 5, but they must state it.

- Do not block on reviewers. After one bounded wait, mark a missing review
  `blocked — pending` with its task ID and return a partial report.
- A response counts only if every finding has severity, file:line evidence,
  introduced/inherited, and a correction, plus an explicit no-findings statement
  if none and stated limitations. Otherwise send one follow-up to the same
  reviewer; if still insufficient, mark `blocked`.
- Assess each finding against sources: `accepted` (fix) | `rejected` (cite why) |
  `limitation` | `spec gap` (ledger open question). Fix only in-scope issues
  introduced by this mockup; do not fix unrelated inherited issues.
- After fixes: rerun affected browser journeys and send the reviewer who raised
  the finding a scoped recheck ([templates/recheck.md](./templates/recheck.md)).
- No subagents available: mark both reviews `blocked`. Self-review does not substitute.
- Exit: both reviews `complete` with dispositions and recheck outcomes in the ledger.

### 7. Report

Use [templates/completion-report.md](./templates/completion-report.md). Classify
every outcome as **verified**, **partially verified**, or **blocked**.
