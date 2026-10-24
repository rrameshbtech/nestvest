# Research: Phase 3 — Dashboard Navigation

## Dashboard navigation and lifecycle

**Decision**: Use the existing profile route as the setup gate, then make the dashboard
the ordinary saved-profile starting point. The dashboard routes to existing expense
capture, expense history, and reminder settings rather than implementing parallel flows.
Preserve the existing reminder notification request to open expense capture.

**Rationale**: `MainActivity` currently switches from `ProfileRoute` directly to
`ExpenseRoute`; `ExpenseViewModel` already owns the capture, history, detail, and
reminder destinations. The Phase 3 mockup calls for a dashboard after profile setup and
explicitly says Add expense opens the amount-first flow. Reusing these destinations
avoids duplicating proven behavior and keeps the notification action functional.

**Alternatives considered**:
- Rebuild expense and reminder screens inside the dashboard: rejected because it
  duplicates existing flows and risks changing Phase 2 behavior.
- Make dashboard access require a new profile step: rejected because saved profile
  state already gates the app.

## Dashboard preference persistence

**Decision**: Persist the user's pinned stable option identifiers in a local singleton
Room preference record and update it only after the user saves customization.

**Rationale**: The feature requires preferences to survive app restarts and operate
  offline. The app already uses Room for its local profile and expense data. The
  mockup's saved choices action and failure state establish that edits are drafts and a
  failed save must not replace the previously saved preference.

**Alternatives considered**:
- Keep choices only in UI state: rejected because they would not survive app restarts.
- Add a remote preference service: rejected by the local-first privacy constraint.

## Expense summaries

**Decision**: Derive the optional current-day count and total from the existing local
expense collection, using the selected profile currency and the device's local day.
Represent loading, available-with-records, no-records, and unavailable states
distinctly. Do not persist a duplicate aggregate.

**Rationale**: Expense records already store exact minor-unit amounts and currency, and
the UI uses locale-aware date/currency formatting. The dashboard summary is small and
must not misstate missing information as zero or block navigation when unavailable.

**Alternatives considered**:
- Maintain a separately persisted dashboard total: rejected because it duplicates
  derived financial data and creates consistency risks.
- Show summary values from the HTML mockup: rejected because all mockup activity is
  synthetic and session-only.

## UI and accessibility conventions

**Decision**: Follow the Phase 3 mockup's quiet app bar, “Pinned for you” and “More”
groups, Customize flow, concise tile summaries, and inline status feedback. Use
existing Compose/Material 3, localized string resources, accessible state descriptions,
and the project's established test approach.

**Rationale**: These patterns directly satisfy the Phase 3 interaction model while
aligning with the app's existing Compose UI and its constitution's accessibility,
localization, and visual-simplicity requirements.

**Alternatives considered**:
- Add a new UI toolkit or navigation library: rejected because existing dependencies
  already support the required screens and routing.
- Convey summary or pin state using color/icon alone: rejected because status must be
  accessible without color perception.
