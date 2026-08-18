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

## Expansion Packs

The All Time Wrestling RPG features a modular content system where wrestlers, staff, and pre-defined teams are grouped into **Expansion Packs**.

### How it Works

- **Content Grouping:** Every wrestler and staff member belongs to a specific set (e.g., "Base Game", "Extreme Pack", "Eddie Guerrero Pack").
- **Member-Driven Availability:**
  - Tag Teams and Factions do not have a fixed set. Instead, their availability is derived from their members.
  - A Team or Faction is only available if **all** of its members belong to expansion packs that are currently enabled.
- **Hard Disable:** Disabling an expansion pack completely hides all associated wrestlers from the roster, rankings, and match selection. Any teams or factions they belong to will also be hidden.

### Management

Administrators can manage these packs through the **Admin -> Expansion Management** tab. Toggling an expansion pack immediately updates the available content across the entire application.

## Faction Synergy

Factions are more than just groups; they provide mechanical benefits when members work together.

### Affinity

- Factions gain **Affinity** through shared matches and victories.
- High Affinity unlocks synergy buffs that can influence match outcomes and narrative progression.

## Fan Growth & Tiers

Gaining fans is the primary way to advance through tiers and unlock title eligibility. However, as a wrestler becomes more famous, maintaining that growth becomes increasingly difficult.

### Fan Gain Efficiency (The "Fan Tax")

The number of fans gained from matches and events is subject to diminishing returns based on the wrestler's current tier:

| Tier               | Gain Efficiency | Description                                                                     |
|:-------------------|:----------------|:--------------------------------------------------------------------------------|
| **Rookie / Riser** | 100%            | Full gains. Every new fan counts.                                               |
| **Contender**      | 97%             | Minor friction in growth.                                                       |
| **Midcarder**      | 95%             | Growth begins to stabilize.                                                     |
| **Main Eventer**   | 93%             | Significant effort required to reach the top.                                   |
| **Icon**           | 90%             | The "Legend Tax." Only the most impactful moments grow an already massive base. |

### Rounding & Deductions

- **Rounding:** After efficiency is applied, positive fan gains are rounded to the nearest **1,000**.
- **Deductions:** When a wrestler *loses* fans or *spends* them (e.g., for injury treatment), no tax or rounding is applied. Losses are always exact.

## Status Cards

Status Cards represent a wrestler's mental or social state, providing modifiers that impact matches and backstage actions. These are primarily introduced in the "ATW vs. WOW" expansion.

### Double-Sided Mechanics

Every Status Card is **double-sided**, representing **Level I** and **Level II**.

- **Positive Statuses:** Level II provides enhanced benefits. Players aim to "upgrade" these cards.
- **Negative Statuses:** Level II is more punishing. Players aim to "downgrade" these cards back to Level I or discard them entirely.

### Gaining and Flipping

- **Initial Gain:** When instructed to draw a status you don't possess, you gain it at **Level I**.
- **Flipping Up:** If you are prompted to draw a status you already have at Level I, the card **flips to Level II**.
- **Conditions:** Each card has specific **Trigger Conditions** evaluated at the end of every match:
  - **Flip Up Condition:** Triggers the upgrade from Level I to Level II.
  - **Flip Down Condition:** Triggers the downgrade from Level II back to Level I.
  - **Discard Condition:** Removes the status card entirely.

### Mechanical Modifiers

Status Cards can influence various wrestler attributes:
- **Momentum:** Starting momentum for the next match.
- **Hand Size:** The number of attack cards drawn at the start of a match.
- **Victory Points:** Bonuses or penalties to VP gains.
- **Custom Scripts:** Unique effects like the "Respected" card's defensive bonus.

Wrestlers can hold an unlimited number of **different** Status Cards, but never more than one of the same type.

## Legacy Score, Prestige & Achievements

While the mechanics above track a single wrestler's in-ring performance, your **Legacy Score** tracks your career as a manager — persisting across every wrestler you've ever booked and every season you've played.

### How it Works

Legacy Score is recalculated automatically whenever your roster changes, combining three factors:
- **Fans:** 1 point per 1,000 total fans across all wrestlers you manage.
- **Titles:** 50 points per championship currently held by any of your wrestlers.
- **Prestige:** The sum of XP from every Achievement you've unlocked.

**Prestige XP** is permanent — once an Achievement is unlocked, its XP keeps contributing to your Legacy Score for as long as your account exists, even if the wrestler who earned it retires or is released.

### Achievements

Achievements are milestone rewards that unlock automatically as you play — winning matches, building your roster, capturing titles, and completing Challenges are all monitored in the background, with no manual claiming required. Each spans one of several categories:

| Category          | Examples                                                                                       |
|:------------------|:-----------------------------------------------------------------------------------------------|
| **Collection**    | Roster-building milestones (managing your first wrestler, reaching 10 or 50 wrestlers).        |
| **Fans**          | Total fan-count thresholds across your roster.                                                 |
| **Championship**  | Title reign milestones, including holding every active title at once.                          |
| **Match Type**    | Participating in or winning a specific match stipulation (Cage, Ladder, Battle Royal, etc.).   |
| **Booking**       | Show-quality and match-quality milestones as a booker.                                         |
| **Special Event** | One-off feats like winning an Abu Dhabi Rumble.                                                |
| **Challenge**     | Weekly and season Challenge completions, including cumulative and difficulty-based milestones. |

#### Unlock Conditions

Each achievement carries a Groovy boolean expression evaluated automatically against the current game state after every relevant event — no manual claiming is needed. Common variables available in unlock expressions:

| Variable             | Type    | Description                                          |
|:---------------------|:--------|:-----------------------------------------------------|
| `wrestlers`          | List    | All wrestlers currently managed by the player        |
| `currentTitlesHeld`  | int     | Number of championships currently held               |
| `isWinner`           | boolean | Whether the segment outcome was a win                |
| `isMainEvent`        | boolean | Whether the segment was the show's main event        |
| `isPremiumLiveEvent` | boolean | Whether the event is flagged as a Premium Live Event |

When no unlock condition is present, the achievement is triggered explicitly by service code at the exact game moment (e.g. completing a Challenge, winning a specific tournament).

#### Wrestler-Specific Achievements

The **Special Event** category includes wrestler-centric milestones that track feats only possible with a particular superstar — such as winning a championship while managing a specific Icon-tier wrestler, or completing a defined number of consecutive victories with the same competitor. These achievements are evaluated after every show segment and unlock silently in the background, the same as all other achievement types.

### Management

- **View Progress:** Your Legacy Score, Prestige, and unlocked Achievement badges are visible on the **Player Dashboard**, under the Career Legacy and Achievements tabs.
- **Compare Rankings:** The **Hall of Fame** dashboard ranks every player across the ecosystem, primarily by Legacy Score.

## Tournament Segment Rules

Tournaments support two modes for determining the match stipulation played in each round.

### Pool Mode

The `Tournament.allowedRules` list holds a set of eligible `SegmentRule` entries. When a round starts without a fixed rule, the system randomly selects one from the pool. This creates open-draw tournaments where the stipulation is unknown until each round is scheduled.

### Fixed-Rule Mode

A specific `TournamentRound` can carry a `fixedRule` that overrides the pool for that round, regardless of what the pool contains. This enables structured brackets — for example, Quarterfinals under Submission rules, Semi-finals in a Steel Cage, and a Normal final.

### Resolution Priority

The system resolves a round's stipulation in this order:

1. **Round's `fixedRule`** — if set, always used.
2. **Random pick from `allowedRules` pool** — if the pool is non-empty.
3. **Normal** (standard match) — fallback when neither applies.

### Creating a Deadly Combat Tournament

A Deadly Combat tournament (as referenced in OMZ's backstory) is configured by assigning a pool of high-intensity, No DQ-eligible rules — such as Submission, Last Man Standing, and Barbwire Exploding Deathmatch — to `allowedRules`. Each round then draws randomly from this pool, preserving the unpredictable, brutal nature of the original tournament.

