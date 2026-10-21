<!--
Sync Impact Report
Version change: 1.1.0 -> 1.2.0
Modified principles:
- I. Local-First Privacy and Data Sovereignty -> clarified secure on-device storage
- V. Calm, Honest, and Legible Experience -> expanded to require accessibility and localization
Added sections: None
Removed sections: None
Follow-up TODOs: None
-->
# NestVest Constitution

## Core Principles

### I. Local-First Privacy and Data Sovereignty
All personal financial data MUST be stored and processed on-device or within a trusted
household-local environment. The application MUST NOT send this data to internet-facing
databases, public cloud services, or third-party analytics. Device-to-device data exchange
MUST be limited to authenticated, trusted devices over peer-to-peer connections or a
household-local network. Any future relay server MUST remain on that local network and
MUST NOT expose family data to the public internet. Sensitive data stored on-device MUST
use platform-appropriate protections at rest, and access to it MUST be restricted to
authorized app and user operations. This boundary protects household privacy and keeps
core app behavior independent of internet services.

### II. Zero-Friction, User-Controlled Capture
Every user interaction MUST minimize friction, touches, and time in the app while still
collecting enough information for meaningful, accurate capture, tracking, and analysis.
Recording a manual expense MUST require no more than an amount and an explicit save
action. Categories, descriptions, vendors, tags, and receipt images MUST remain optional
and MUST NOT block saving. The app MAY infer contextual details, such as location, only
when available and permitted; inferred details MUST be distinguishable from confirmed
details and MUST NOT be presented as certain user-provided facts.

Users MUST be able to save an entry with minimal information and complete or correct it
later without losing data or misrepresenting what was originally recorded. The app MUST
preserve entered values and clearly communicate uncertainty or provenance for inferred
or parsed values. It MAY offer unobtrusive, user-controllable opportunities to resume
incomplete entries, such as a concise end-of-day review of recent expenses with missing
details. Such prompts MUST NOT interrupt the user's current task or make enrichment
mandatory. This allows users to record promptly and improve the quality of their financial
records when they have time.

### III. Conflict-Resilient Household Data
Data identities and schemas MUST support merges across multiple household devices.
Records MUST use stable, device-independent unique identifiers or equivalent functional
keys; simple auto-incrementing identifiers MUST NOT be used as cross-device identity.
Synchronization MUST define deterministic duplicate detection and conflict resolution,
and changes to shared schemas or merge behavior MUST include tests for concurrent or
out-of-order updates. This prevents sync from silently duplicating or losing family records.

### IV. Kakeibo-Led Financial Wellbeing
Budgeting and reflection experiences MUST support mindful spending rather than treating
the product as corporate accounting software. Planning and review MUST use the Kakeibo
categories Needs, Wants, Culture, and Unexpected (also presented as Survival, Optional,
Culture, and Extra where the interface uses that vocabulary). Financial summaries MUST
present recorded information honestly and distinguish user-entered facts from estimates
or forecasts. This keeps the product oriented toward intentional household decisions.

### V. Calm, Honest, and Legible Experience
Interfaces MUST prioritize clarity and purposeful simplicity over decoration. Financial
values, labels, and actions MUST be legible and understandable; controls used for frequent
capture MUST be easy to reach and operate. Visual emphasis MUST NOT distort the meaning
of financial data. The design MUST use restrained typography, high-contrast foundations,
and purposeful accent colors, avoiding unnecessary shadows, gradients, and visual noise.
All user-facing content MUST support localization, including text, dates, numbers, and
currency presentation; user-visible strings MUST NOT be designed around a single language
or region. Interfaces MUST meet applicable accessibility standards, including support for
assistive technologies, readable contrast and text, and operation without relying solely
on color or precise gestures. This reduces friction and supports confident decisions for
users across languages, regions, and abilities.

## Additional Constraints

Financial data ingestion, including SMS, PDF, and CSV parsing, MUST occur locally on the
device. Sensitive local data MUST use platform-appropriate storage protections at rest,
and sync MUST authenticate participating trusted devices and protect data in transit. The
product MUST remain useful without cloud registration or internet connectivity.
Notifications and automated suggestions MUST be local and non-intrusive. Any proposed
external data service or public-network path for household data requires a constitution
amendment before implementation.

## Development Workflow and Quality Gates

Feature specifications, plans, implementation tasks, and code reviews MUST demonstrate
compliance with this constitution. Tests MUST verify that an expense can be saved with
only an amount, that local-only data paths do not require internet services, and that
syncable records merge without identity collisions or silent data loss. Features that
parse or forecast financial data MUST test representative inputs and clearly distinguish
parsed or estimated values from confirmed records. User-facing features MUST be checked
for localization readiness and accessibility, including representative locales and
assistive-technology operation. Reviewers MUST check privacy, security, data integrity,
accessibility, localization, and user-control requirements for each affected feature. A
conflict with a principle MUST be resolved by changing the proposal or amending this
constitution before implementation; exceptions MUST NOT be introduced informally.

## Governance

This constitution is the governing source for product and engineering decisions in the
NestVest project. All contributors MUST follow its principles and constraints. Amendments
MUST be proposed with their rationale, affected principles, migration or compatibility
impact, and any required updates to specifications and tests. The project maintainers
MUST review and approve amendments before affected work proceeds.

The constitution MUST use semantic versioning. A MAJOR increment is required when a
principle is removed or redefined incompatibly; a MINOR increment is required when a
principle or section is added or materially expanded; a PATCH increment is used for
clarifications and non-semantic wording changes. The last-amended date MUST be updated
with every approved change.

Compliance MUST be reviewed when a feature is specified, during implementation review,
and before release. Reviews MUST check applicable tests and confirm that no cloud,
analytics, sync, data-entry, accessibility, or localization behavior violates the
constitution. If a conflict is found, the work MUST be revised or the constitution amended
through the process above.

**Version**: 1.2.0 | **Ratified**: 2026-10-21 | **Last Amended**: 2026-10-21
