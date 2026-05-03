# Specification: Status Cards Mechanic

## 1. Overview
The Status Cards mechanic introduces a system to represent a wrestler's mental or social state (e.g., "Draw", "Lost Confidence"), providing modifiers that impact matches and backstage actions, originally introduced in the Women's Edition ("ATW vs. WOW") expansion.

## 2. Core Mechanics

### 2.1 Double-Sided Progression (Level I and II)
*   Status cards have two levels.
*   **Positive Statuses:** Level II provides enhanced benefits (Upgrade).
*   **Negative Statuses:** Level II is more punishing (Downgrade).

### 2.2 Gaining and Flipping Statuses
*   When instructed to draw a status card:
    *   If the wrestler does not have it, they gain it at Level I.
    *   If they have it at Level I, the card flips to Level II.
    *   If they have it at Level II, the instruction is generally ignored (unless specified otherwise).

### 2.3 Trigger Conditions
*   Each status has specific trigger conditions (e.g., win/loss, ending momentum) evaluated at the end of a match to determine if the card flips up, flips down, or is discarded.

### 2.4 Stacking Constraints
*   Wrestlers can hold multiple different Status Cards simultaneously.
*   **However, a wrestler cannot hold multiple copies of the exact same Status Card type at the same time.** If they receive a duplicate, it triggers the flipping mechanic (Level I -> Level II) or is ignored (if already Level II).

## 3. Specific Status Examples
*   **Draw / Main Eventer:** Positive. L1: +4 momentum. L2: +4 momentum & Main Event status.
*   **Lost Confidence / Humiliated:** Negative. L1: -2 starting attack cards. L2: Starts at -7 momentum.
*   **Superstar / GOAT:** Positive social status.
*   **Respected:** One-time use per match to increase opponent's target number by 1 during defense.

## 4. Technical Implementation

### 4.1 Backend Storage (Entity-Based)
*   Similar to the Alignment abilities, new entities will be created:
    *   `StatusCard`: Defines the status, its levels, modifiers, and flip conditions.
    *   `WrestlerStatus`: Maps a `Wrestler` to a `StatusCard` and tracks the current level (I or II).
    *   `WrestlerStatusHistory`: Logs all changes (gains, flips, losses) for auditing and campaign tracking.

### 4.2 UI Integration
*   **Profile Icons:** Active status icons will be displayed on the wrestler's profile view.
*   **Match Setup UI:** Statuses and their modifiers will be visible and interactive during the match setup phase.
*   **Admin Override:** GMs/Admins will have UI controls to manually assign, flip, or remove status cards from wrestlers.

### 4.3 Match Integration
*   **Pre-Match Phase:** AI evaluates active Status Cards to apply modifiers to starting health, stamina, momentum, and hand size.
*   **Post-Match Phase:** AI evaluates trigger conditions on active cards to determine if they flip or discard based on match outcomes and momentum.

### 4.4 Campaign Integration
*   **Chapter Configuration:** The campaign chapter configuration schema must be enhanced to support Status Cards.
*   **Branching Outcomes:** It must support defining which specific story branches or objectives grant (or remove) a particular Status Card.

### 4.5 Settings & Expansion Integration
*   **Global Toggle:** A system setting must be added to globally enable or disable the Status Cards mechanic.
*   **Expansion Dependency:** The Status Cards mechanic is fundamentally tied to the "Women's Expansion" set. If this expansion is disabled, the Status Cards mechanic must be automatically disabled.

### 4.6 Non-Campaign Assignment
*   **Procedural Assignment:** The system must provide a mechanism to assign statuses outside of the campaign context (e.g., based on win/loss streaks, major PPV victories, random backstage events in GM mode).
*   The exact rules for these procedural assignments will be determined during implementation but the framework must support them.