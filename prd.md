# NestVest: Product Specification Document

## 1. Executive Summary

**NestVest** is a local-first, privacy-obsessed mobile application designed for holistic family financial wellbeing. It acts as a secure, shared household ledger and knowledge base. Built around the Japanese _Kakeibo_ philosophy of mindful spending, NestVest automates expense and investment tracking without ever sending user data to the public internet. It ensures generational financial continuity by safely storing assets, liabilities, and SOS protocols locally on family devices.

## 2. Core Principles & App Rules (Strict Constraints for SpecKit)

To ensure the integrity of the application, the following rules must govern all architecture and code generation:

- **Rule 1: Absolute Zero Cloud (Air-Gapped Architecture).** No internet-facing databases (e.g., Firebase, AWS) or third-party analytics are permitted. All data processing must happen strictly on-device using local databases.
- **Rule 2: Zero-Friction Data Entry.** The app must function with the absolute minimum amount of data provided by the user. Additional context is strictly voluntary. For example, to manually log an expense while standing at a checkout counter, the user should only need to enter the "Amount" and hit save. The app must not block saving by demanding categories, tags, or descriptions.
- **Rule 3: P2P-Optimized Database Design.** The database schema must be designed natively for conflict-free Peer-to-Peer (P2P) synchronization.
  - Avoid simple auto-incrementing surrogate keys, which cause merge conflicts across devices.
  - Use **functional unique keys** (e.g., a composite key of `DeviceID + Timestamp + Type`, or a deterministic hash of the transaction details) to naturally identify duplicates and ensure seamless, frictionless merging when syncing with family members.
- **Rule 4: Kakeibo as the North Star.** Budgeting flows must not feel like corporate accounting. They must prompt reflection and categorize spending based on Kakeibo principles (Needs, Wants, Culture, Unexpected).
- **Rule 5: Secure Local Sync.** Inter-device communication is strictly peer-to-peer (Bluetooth/Local Wi-Fi) or via a trusted household Intranet.
- **Rule 6: Dieter Rams UX Philosophy.**
  - _Less, but better:_ UI must be unobtrusive, stripping away unnecessary visual noise.
  - _Honest:_ Financial data must be clear and not manipulated.
  - _Aesthetic & Understandable:_ Use minimalist typography, high-contrast monochrome bases with muted, purposeful accent colors.

## 3. Incremental Feature Breakdown & Phased Rollout

To support Agile and Spec-Driven Development, features are broken down into sequential build phases.

### Phase 1: Foundation & The Primary User

- **Goal:** Establish the local environment and primary identity without requiring cloud registration.
- **Features:**
  - **Local Profile Creation:** Capture basic information (Name, preferred currency, simple avatar/color) to establish the "Primary User."
  - **Future-Proof Identity:** This profile is architected to operate solo initially, but is built with the underlying data structure to be linked to a "Family Circle" in later phases.
  - **DB Initialization:** Set up the local database with P2P-ready functional unique keys.

### Phase 2: Ultra-Fast Daily Expense Capture

- **Goal:** Capture day-to-day spending (grocery store runs, movie tickets, cash tips) immediately and with zero friction.
- **Features:**
  - **Minimalist Input Pad:** A large, easy-to-tap numpad accessible immediately upon opening the app. Entering an amount and pressing "Save" is the only required action.
  - **Voluntary Enrichment:** After saving, users are given an optional, non-blocking prompt to add context (Vendor, Kakeibo Category, Receipt Photo).
  - **Non-Intrusive Nudges:** Gentle, local notifications (e.g., a silent end-of-day prompt: _"Any unrecorded cash spends today?"_) to encourage habit-building without being annoying.

### Phase 3: Dashboard

- **Goal:** Show app wide dashboard to help user to navigate across easily
- **Features:**
  - **Minimal & Fluid:** A minimal, friction less dashboard provides options to choose module/actions
  - **Customizable:** Keep dashboard customizable. So that user can keep frequently used options on top and easily accessible. The rest of the module/actions will be shown from subtle more option or after a scroll up
  - **subtle & multi functional tiles:** The module/action tiles shows badge or subtle summary (only if it adds value) based on the module or action.

### Phase 4: The Kakeibo Engine (Planning & Reflection)

- **Goal:** Introduce mindful financial planning over raw accounting.
- **Features:**
  - **Interactive Guidelines:** Pictorial, interactive walk-through of Kakeibo principles for setting up the month.
  - **Monthly/Weekly Planning:** Prompt users to set intentional goals ("How much do you want to save?", "What will you improve?").
  - **Visual Buckets:** Show remaining funds mapped to Kakeibo categories (Survival, Optional, Culture, Extra).
  - **Reflection Flow:** At the end of the cycle, prompt a mindful review rather than generating a cold, sterile "expense report."

### Phase 5: Automated Data Ingestion & Cashflow Tracking

- **Goal:** Automate the heavy lifting and forecast liquidity.
- **Features:**
  - **Local SMS & Statement Parsing:** On-device regex/NLP to parse transaction alerts from banks and credit cards, and local parsing of uploaded PDFs/CSVs.
  - **Predictive Cashflow Dashboard:**
    - Auto-capture recurring monthly inflows.
    - Schedule and predict yearly liabilities (School fees, EB bills, insurance, property tax).
    - Visualize upcoming inflows vs. outflows.

### Phase 6: Wealth & Portfolio Management

- **Goal:** A consolidated, multi-generational view of all investments.
- **Features:**
  - **Bond & NCD Inventory:** Track held bonds, alert on upcoming interest, and reconcile expected interest with actual bank SMS/statements. Alert on maturity.
  - **Consolidated Equities & Mutual Funds:** Parse stock broker SMS alerts. Aggregate multi-broker data.
  - **Sub-Portfolios (Kids' Tags):** Allow specific holdings to be tagged (e.g., "Kid's Piggy Bank") to show children their specific portfolio, which remains a technical subset of the parent's actual account.

### Phase 7: Family Knowledge Base, SOS & Secure Sync

- **Goal:** Shared knowledge, generational continuity, and frictionless offline data exchange.
- **Features:**
  - **Cryptographic Trust Circle:** Securely add family members via local QR code scanning or certificate exchange.
  - **Proximity Auto-Detect & Sync:** Background data exchange of expenses and assets when trusted devices are on the same Wi-Fi or via Bluetooth (utilizing the conflict-free DB design).
  - **Assets & Liabilities Vault:** A shared ledger of everything the family owns and owes.
  - **SOS Helper:** Dedicated profiles for Term Insurance, Health Insurance, etc. Includes actionable metadata: "Who to contact," "What to do next," and "Location of Important Documents."
  - _(Future Scope)_: Support for a central local server (e.g., Raspberry Pi on home Wi-Fi) to act as an always-on data relay.

## 4. UX/UI Technical Guidelines for SpecKit

- **Theme:** Minimalist. Deep blacks/dark grays, stark whites, and precise geometry.
- **Typography:** Sans-serif, highly legible (e.g., Inter, Roboto, or system default). Utilize font weight to establish hierarchy rather than multiple colors.
- **Components:**
  - Use bottom sheets for quick actions (manual expense).
  - Use progress rings/bars (thin, non-intrusive) for Kakeibo budget tracking.
  - Avoid shadows, gradients, and 3D effects. Rely on flat design and negative space (Whitespace) to group elements logically.
