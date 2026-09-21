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

The **template booking path** (tournament rows on a show template) inserts one extra tier between the fixed rule and the pool — the template row's own rule — because an existing type+rule pairing keeps its pairing's stipulation (ATW-etws):

1. **Round's `fixedRule`** (booker-set, or the spec final rule stamped at the bracket final)
2. **The template row's rule** (type+rule AUTO_ATTACH pairing)
3. **Random pick from `allowedRules` pool**
4. **Normal** (standard match)

### Creating a Deadly Combat Tournament

A Deadly Combat tournament (as referenced in OMZ's backstory) ships in the seeded catalog (`tournaments.json`) with the pool of high-intensity, No DQ-eligible rules — Submission, Last Man Standing, and Barbwire Exploding Deathmatch — already assigned to `allowedRules` and a `defaultEntrantCount` of 8. Each round then draws randomly from this pool, preserving the unpredictable, brutal nature of the original tournament. The campaign's "The Tournament" chapter declares `tournamentCode: deadly_combat`, so the Deadly Combat win counter and its scripted achievement only progress in campaigns whose tournament chapter maps to that catalog tournament.

## One-Time Tournaments on a Host Show

A tournament can be attached to a **particular show** (its *host show*) instead of being paired with a PLE template — the natural shape for one-off events like the Abu Dhabi Rumble. PLE-template attachment stays reserved for **recurring** tournaments.

### Booking Behavior

- **Payoff:** the tournament's payoff (its final, or a champion-vs-winner showcase) books on the host show exactly once, then the host-show link is consumed so it can never fire twice. When the linked championship is vacant, the final itself is the title match at the host show.
- **Rounds:** the non-final rounds pace automatically onto the non-PLE shows scheduled before the host show — no weekly template row needed. The system divides the remaining matches across the slots remaining and books that many per show (never more than what remains).
- **Payoff match type and rule:** configurable on the tournament (defaulting to One-on-One). This is what enables payoffs the PLE-template path cannot express — for example, a 6-man Free-for-All TLC match for a title at a one-off show.

### Planning-Card Integration

Tournament slots appear on the planning card ahead of the AI's proposals, the same way scripted arc beats do:

- Each slot is one grid row (a multi-match round preview expands into one row per match), stamped **Tournament** in the Source column — delete a row to skip that slot on this show.
- Seeded brackets preview the **real match-ups** (open bracket matches, champion showcases, and seeded round-1 finals); unseeded or unknowable finals preview placeholder teams, since participants resolve from the bracket at approval.
- The AI prompt never lists these slots: they are removed from the AI's budget ("N total; X pre-determined match slot(s) are booked automatically") and their committed wrestlers are filtered out of the rendered roster, so the model can neither re-propose the slots nor double-book the wrestlers. Placeholder rows claim no budget — the bracket booking is additive. When every slot is pre-determined, the AI call is skipped entirely and the card is built purely from the deterministic passes.
- If the bracket can't finish before the payoff show, approving the card shows a warning — the payoff would degrade to a regular round match; approve again later to catch up, or book rounds manually from the tournament view.

### Template Attachment vs. Host Show

These two binding mechanisms are mutually exclusive by design:

- **Host show** (`Tournament.payoffShow`) — one-time tournaments. The show-attached booking path owns the tournament; any template pairing on it stays idle.
- **PLE template pairing** (`ShowTemplateSegmentAssignment`) — recurring tournaments. The PLE template books the payoff every time that template runs; weekly templates pace the rounds.

## Tournament Spec Rows on Templates

A PLE template's assignment row can carry a full **tournament spec** instead of referencing an existing tournament (ATW-etws): a name, a format, an entrant count, a final rule, an allowed-rules pool, and an optional linked championship. The booking path creates **one persistent tournament per row** on first use — afterwards the row behaves exactly like a tournament-linked row, so approval/rollback cycles reuse the same instance.

- **Identity:** the spec lives on the template row (`specName`, `specFormatId`, `specEntrantCount`, `specFinalRule`, `specTitle`, and the `show_template_assignment_rule` join table for the pool). A row is either a tournament reference or a spec — never both.
- **Entrant-count hierarchy:** the row's spec count → the tournament's `defaultEntrantCount` (catalog presets, e.g. Deadly Combat's 8) → the format max (legacy full-bracket behavior). Spec and preset counts are strict — if the eligible roster can't cover them, the booking falls back to AI participants rather than quietly shrinking the promised bracket; the legacy format-max tier keeps its historical lenient behavior for small rosters.
- **Stipulations:** round rules follow the template-path priority above (fixed rule → row rule → pool); the spec's final rule is stamped onto the bracket final when the row defines one.
- **Consumption:** after the payoff the pairing is consumed — the spec fields clear along with the tournament reference, so a consumed row can never mint a second instance.
- **Seeded catalog:** `tournaments.json` seeds predefined tournaments at startup (skipped when tournaments already exist, upserted by stable `code` otherwise; re-syncing never touches lifecycle state). Template rows can also reference catalog tournaments directly by picking them in the Tournament combo.

## Multi-Entrant Tournaments (Qualifier Groups → Multi-Man Final)

The **Qualifier Groups** format runs N multi-wrestler qualifier matches feeding one M-entrant final — the classic shape for a Free-for-All tournament (e.g. 12 entrants in four 3-man qualifiers → a 4-man Free-for-All TLC title match at the payoff show).

- **Bracket:** entrants split into balanced groups (aiming for 3-wrestler groups, adjusted for divisibility). Each wrestler plays exactly one qualifier; only group winners advance to the single final round.
- **Booking:** multi-entrant matches book through the multi-team segment resolution — every entrant lands in their own team slot (the same layout the Edit Segment dialog shows). Two-entrant matches (including 2-qualifier finals) keep the classic path.
- **Results:** recording a winner eliminates every other entrant in that match — a 6-man final crowns one winner and retires the other five.
- **Match modeling:** the classic `entrant1`/`entrant2` columns remain the two-entrant case; matches with 3+ entrants carry ordered `tournament_match_participant` rows (the authoritative list, with the classic columns mirroring the first two slots).
- **PLE template assignments** may now pick any segment type (not just event-only formats) — a Free-for-All qualifier round needs a Free-for-All assignment, which the old restriction blocked.

