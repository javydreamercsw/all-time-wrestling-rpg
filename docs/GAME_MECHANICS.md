# Game Mechanics

This document describes the core game mechanics of All Time Wrestling RPG.

## Persistent Wear & Tear

Matches take a toll on a wrestler's physical well-being. This is tracked via the **Physical Condition** metric.

### How it Works

- Every wrestler starts with **100% Physical Condition**.
- Each match played reduces this condition based on several factors:
  - **Base Loss:** 1-3% per match.
  - **Intensity Multiplier:** Matches with "Extreme", "No DQ", or "Cage" rules double the condition loss.
  - **Main Event Penalty:** Main events add an additional 1% loss.
- **Promo segments** do not cause any wear and tear.

### Mechanical Impact

- **Health Penalty:** For every 5% lost from 100%, the wrestler suffers a **-1 Health penalty** to their starting health in matches.
- **Cap:** The health penalty from wear and tear is capped at **-5 HP**.
- **Retirement Risk:**
  - If condition falls below **20%**, there is a **10% chance** of forced retirement after each match.
  - If condition falls below **10%**, the risk increases to a **50% chance** of forced retirement.

### Management

- **View Condition:** Physical condition is visible on the Campaign Dashboard, Wrestler Summary Cards, and the full Wrestler Profile.
- **Resetting:** Administrators and Bookers can reset a wrestler's condition to 100% via the Wrestler Profile. A global reset for all wrestlers is available in the **Admin Tools** dashboard.
- **Toggle:** The entire feature can be enabled or disabled globally in **Game Settings**.

## Faction Synergy

Factions are more than just groups; they provide mechanical benefits when members work together.

### Affinity

- Factions gain **Affinity** through shared matches and victories.
- High Affinity unlocks synergy buffs that can influence match outcomes and narrative progression.

