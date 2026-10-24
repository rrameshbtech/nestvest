# NestVest Design Research

Reusable visual and interaction guidance for feature mockups. Local repository
sources were reviewed on 2026-10-24; no external design research was needed.

## Approved rules and tokens

- **Calm, local-first, accessible:** Favor purposeful simplicity, honest financial
  presentation, assistive-technology support, legible contrast, localization readiness,
  and operation without color alone or precise gestures. Source: [constitution.md](./constitution.md),
  Principles I and V.
- **Visual language:** System sans-serif; charcoal `#202521`, muted text `#626a62`,
  warm paper `#fafbf7`, thin sage-gray lines `#dce1d8`, deep green `#3d5844`, and pale
  sage `#e7eee2`; use compact labels, restrained accent color, flat surfaces, generous
  whitespace, rounded but unobtrusive controls, and visible green focus outlines.
  Sources: [Phase 1 profile mockup](../../specs/001-local-profile-foundation/ui-mockups.html)
  and [Phase 2 expense mockup](../../specs/002-daily-expense-capture/ui-mockups.html).
- **Interaction continuity:** Keep expense capture amount-first, with optional details,
  history periods, and reminder controls remaining independently accessible.
  Source: [Phase 2 spec](../../specs/002-daily-expense-capture/spec.md) and
  current implementation in [MainActivity.kt](../../app/src/main/java/com/nestvest/MainActivity.kt),
  [ExpenseRoute.kt](../../app/src/main/java/com/nestvest/expense/ui/ExpenseRoute.kt),
  and [ExpenseCaptureScreen.kt](../../app/src/main/java/com/nestvest/expense/ui/ExpenseCaptureScreen.kt).
- **Prototype constraints:** Use only synthetic data; no remote fonts, services, analytics,
  or app persistence. Interactive mock state is in memory and is not a product save.

## Accepted prototype limitations

Reviewers receive this list as a constraint. These are not mockup defects unless
a target spec explicitly requires the mockup to demonstrate them. Record each as a
limitation and an implementation handoff instead.

- **In-memory state:** Saves, preferences, and toggles last only for the page session.
  Persistence is demonstrated by the implementation, not the mockup.
- **Sample currency and language:** Mockups use a labelled sample profile currency (INR)
  and English copy with locale-aware `Intl` number/date formatting. Multi-locale and RTL
  checks belong to implementation unless a spec requires a locale preview.
- **No platform resources:** HTML mockups cannot use Android string resources,
  plurals, or content descriptions; the implementation supplies them.
- **Other features are referenced, not rebuilt:** Unchanged screens from other features,
  such as Phase 1 profile setup, are linked through the journey map and the earlier mockup
  rather than duplicated.
- **Not covered by mockup verification:** Screen reader announcements, 200% text zoom,
  offline/device behavior, and user-research success criteria.

## Rendered comparison map

Compare these in one browser call at 390×844. Values were read from source on
2026-10-24. Phase 1 differs slightly and that difference is inherited.

| Element | Selector | Phase 1 | Phase 2 | Phase 3 |
|---|---|---|---|---|
| Page background | `body` | rgb(233,236,229) | rgb(233,236,229) | rgb(233,236,229) |
| Phone frame | `.phone` | max 390px, radius 30px, paper | max 390px, radius 30px, paper | max 390px, radius 30px, paper |
| Primary button | visible `.primary` | ink, radius 8px, 50px | ink, radius 8px, 49px | ink, radius 8px, 49px |
| In-phone screen title | P1/P2 `h3`, P3 `.screen h2` | 32px / 500 | 29px / 500 | 29px / 500 |
| Focus outline | `button:focus-visible` | 3px `#53754e`, offset 4px | 3px `#53754e`, offset 3px | 3px `#53754e`, offset 3px |

Update a row only when a mockup's source changes; do not re-derive the whole table each run.

## Tool availability

| Tool | Last probed | Result | Next-run action |
|---|---|---|---|
| `mermaid-diagram-validator` | 2026-10-24 | Fails: "Sign in to Mermaid to validate diagrams." Interrupted twice earlier the same day | Skip; mark diagram syntax **unverified**. Re-probe once only if this entry is older than 30 days or sign-in is configured |
| Integrated browser + Playwright | 2026-10-24 | Works with `file://` URLs | Use one page per mockup and navigate within it |

## Decisions

### 2026-10-24 — Phase 3 dashboard visual baseline

- **Decision:** Continue the existing paper/sage/charcoal visual system instead of
  introducing a new theme. Use simple, directly labeled action cards and badges with
  text equivalents.
- **Rationale:** Both existing feature mockups and the constitution use a restrained,
  high-contrast, local-first visual language.
- **Scope:** Phase 3 dashboard mockup only; the rules are reusable by future mockups.
- **Open proposals:** The precise default set of pinned modules and any production
  summary data sources remain product/implementation decisions; this mockup uses only
  the currently implemented expense actions and reminders with synthetic sample data.

## Source status notes

- Phase 1 profile setup and Phase 2 expense UI have implementation source in the
  repository; this records code presence, not release or device-certification status.
- Phase 2 task documentation records remaining manual/device checks; see
  [Phase 2 tasks](../../specs/002-daily-expense-capture/tasks.md).
- Phase 3 dashboard is specified and mockup-only in this workflow; no production
  dashboard implementation was found.
