# General Manager (GM) Mode Refinement - Specification

## Overview

This track evolves the existing Booker role into a strategic General Manager experience. It adds layers of financial management, roster logistics, and performance tracking to provide a more challenging and rewarding simulation.

## 1. Core Concepts

### 1.1. Brand Finances

Each League or Season will track a **Budget**.
- **Revenue:** Ticket sales (Arena Capacity x Rating multiplier), Sponsorships, PPV buys.
- **Expenses:** Roster salaries, Production costs (Pyro, special effects), Venue rental.

### 1.2. Contract Logic

Wrestlers are no longer just "active" or "inactive". They have **Contracts**.
- `salary`: Amount paid per show. This is **dynamically tied to the wrestler's Fan Count** (e.g., a base rate plus a multiplier per 1,000 fans).
- `durationWeeks`: The length of the contract in weeks.
- `expiryDate`: Calculated based on the number of shows or weeks from the signing date.
- `morale`: A value (0-100) affected by booking frequency, win/loss record, and title reigns.

### 1.3. GM Mode Lifecycle & Draft

- **Initial Draft:** Every GM Mode session begins with a mandatory **Roster Draft**. GMs take turns selecting from the available pool of wrestlers to build their initial brand.
- **Drafted Contracts:** Wrestlers selected during the Initial Draft are signed to **Full Season Contracts** (lasting the entire duration of the GM Mode/Season).
- **Free Agency:** After the draft, any unselected wrestlers enter the "Free Agent" pool and can be signed to shorter contracts (e.g., 4-week or 12-week deals) for a premium fee.

### 1.3. Stamina System

A new management-level **Stamina** attribute (1-100) tracks short-term fatigue.
- **Depletion:** Stamina decreases after every match, with intensity multipliers for gimmick matches.
- **Recovery:** Stamina recovers slowly over time or through "Off-Days" (skipping a show).
- **Injury Risk:** If Stamina drops **below 40**, the probability of sustaining an injury increases significantly during segments.

### 1.3. Show Ratings & TV War

- **Segment Stars:** Detailed 0-5 star rating based on wrestler stats, chemistry, and segment type.
- **TV Rating:** An aggregate score for the show that influences revenue and brand growth.

## 2. Gameplay Impact

### 2.1. Resource Scarcity

Bookers must now balance booking their top stars (who have higher appearance fees) with building new talent to stay within budget.

### 2.2. Roster Unrest

Wrestlers with low morale may refuse to perform, demand more money, or threaten to leave for a rival brand (if implemented).

### 2.3. Strategic Booking

Specific segments (e.g., high-risk matches) cost more in production but have higher rating potential.

## 3. Related Immersion Enhancements

### 3.1. Arena Atmosphere

- **Crowd Noise Levels:** Implement a visual "Noise Meter" during match narration, influenced by arena capacity and segment quality.
- **Location-Specific Rules:** Locations can have "Mandatory Rules" (e.g., Mexico City requiring a 2-out-of-3 falls rule for main events).

### 3.2. Backstage Incidents

- **Locker Room Morale:** A global stat for the brand. High morale increases training efficacy; low morale triggers random "Drama Events" like walkouts.
- **Press Conferences:** New segment types that boost show "Hype" (revenue multiplier) but carry a high financial cost.

## 4. Technical Requirements

### 3.1. Persistence

- Update `Season` and `League` to include financial fields.
- New entity `WrestlerContract` linked to `Wrestler` and `League`.
- New entity `FinancialTransaction` to track income/expenses for historical reporting.

### 3.2. UI

- **GM Dashboard:** A new view summarizing financial health, roster morale, and recent show performance.
- **Financial Reports:** Monthly breakdown of profit/loss.
- **Contract Management:** View to renew or release wrestlers.

## 4. Testing Plan

- **Unit Tests:** Revenue and expense calculation logic.
- **Integration Tests:** Contract expiration checks at show completion.
- **E2E Tests:** Verification of the GM Dashboard UI.

