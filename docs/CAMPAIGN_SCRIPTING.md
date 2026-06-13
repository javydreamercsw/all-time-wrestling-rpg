# Campaign Scripting Guide

This guide documents the scripting language used for **Campaign Ability Cards** in the All Time Wrestling RPG.

## Overview

Ability cards in the campaign (Face/Heel cards, Ally/Valet cards) use a simple scripting language based on **Groovy**. These scripts allow cards to have dynamic effects that can manipulate the wrestler's state, the campaign progress, or the flow of a match.

Scripts are stored in the `effect_script` and `secondary_effect_script` columns of the `campaign_ability_card` table.

> **Note:** This same scripting engine is also used for **Title Abilities**. Titles can use a subset of these methods to influence AI narration. See the [Content Management Guide](CONTENT_GUIDE.md) for details.

## Script Syntax

Scripts are Groovy snippets. You can call available methods directly. Multiple commands should be separated by a semicolon (`;`).

**Example:**

```groovy
spendStamina(1);
gainInitiative();
gainMomentum(2)
```

## Available Script Methods

The following methods are provided by the `CampaignEffectContext` and can be used in any ability card script.

### Resource Management

Methods for managing wrestler resources (Stamina, HP, Momentum).

| Method                      | Description                                                     |
|:----------------------------|:----------------------------------------------------------------|
| `spendStamina(int amount)`  | Reduces the wrestler's current stamina by the specified amount. |
| `gainStamina(int amount)`   | Increases the wrestler's current stamina.                       |
| `gainHitPoints(int amount)` | Heals the wrestler by the specified amount.                     |
| `damage(int amount)`        | Deals direct damage to the opponent.                            |
| `gainMomentum(int amount)`  | Increases the wrestler's momentum.                              |
| `drawCard(int amount)`      | Draws the specified number of cards from the attack deck.       |

### Match Flow Control

Methods for manipulating the rules or state of an ongoing match.

| Method             | Description                                |
|:-------------------|:-------------------------------------------|
| `gainInitiative()` | Grants the initiative to the player.       |
| `negateAttack()`   | Cancels the opponent's current attack.     |
| `pin()`            | Immediately triggers a pinfall attempt.    |
| `breakPin()`       | Automatically recovers from a pin attempt. |

### Modifiers & Attributes

Methods for temporary buffs or attribute modifications.

| Method                                  | Description                                                         |
|:----------------------------------------|:--------------------------------------------------------------------|
| `modifyRoll(int modifier)`              | Adds a bonus (or penalty) to the player's next dice roll.           |
| `modifyOpponentRoll(int modifier)`      | Adds a penalty (or bonus) to the opponent's next dice roll.         |
| `modifyBackstageDice(int amount)`       | Adds bonus dice to the next Backstage Action check.                 |
| `modifyAttribute(String attr, int val)` | Modifies a specific wrestler attribute (e.g., 'charisma', 'brawl'). |

## Card Configuration (JSON)

When adding cards to `campaign_ability_cards.json`, use the following structure:

```json
{
  "name": "Feigned Retreat",
  "description": "Spend 1 Stamina to gain Initiative and 2 Momentum.",
  "alignmentType": "HEEL",
  "level": 1,
  "oneTimeUse": true,
  "timing": "DEFENSE",
  "trackRequirement": 1,
  "effectScript": "spendStamina(1); gainInitiative(); gainMomentum(2)"
}
```

### Ally & Valet Cards

Cards with both a passive ability and a one-time use ability use the `secondary` fields:

```json
{
  "name": "Ally",
  "alignmentType": "HEEL",
  "level": 1,
  "oneTimeUse": false,
  "timing": "BACKSTAGE",
  "effectScript": "modifyBackstageDice(1)",
  "secondaryEffectScript": "damage(2)",
  "secondaryOneTimeUse": true,
  "secondaryTiming": "OFFENSE"
}
```

## Implementation Details

The scripting engine is powered by:
- **`CampaignEffectContext.java`**: Defines the Java methods exposed to scripts.
- **`CampaignScriptService.java`**: Evaluates the Groovy snippets using `GroovyShell`.
- **`CampaignService.java`**: Triggers the execution when `useAbilityCard()` is called.

## Campaign Chapters

Campaigns are organized into chapters defined in `campaign_chapters.json`. Each chapter defines its own narrative tone, rules, and progression criteria.

### Chapter Configuration (JSON)

Full example structure:

```json
{
  "id": "beginning",
  "title": "The Beginning",
  "shortDescription": "Your journey in All Time Wrestling starts here.",
  "introText": "You stand backstage...",
  "aiSystemPrompt": "You are the Campaign Director for Chapter: The Beginning...",
  "difficulty": "ENTRY",
  "mode": "STATIC_ONLY",
  "requiredExpansions": [],
  "expansionBoundary": false,
  "entryPoints": [
    { "name": "New Career", "criteria": [ { "maxVictoryPoints": 0 } ] }
  ],
  "exitPoints": [
    { "name": "Tournament Invite", "criteria": [ { "minMatchesPlayed": 3, "minVictoryPoints": 5 } ] },
    { "name": "Sent to Development", "criteria": [ { "minMatchesPlayed": 5, "maxVictoryPoints": 4 } ] }
  ],
  "rules": {
    "victoryPointsWin": 2,
    "victoryPointsLoss": -1
  },
  "staticEncounters": [ ... ]
}
```

### Chapter-Level Fields

| Field                | Type            | Default   | Description                                                                                   |
|:---------------------|:----------------|:----------|:----------------------------------------------------------------------------------------------|
| `id`                 | string          | —         | Unique identifier used in routing and completion tracking.                                    |
| `mode`               | enum            | `AI_ONLY` | Controls how encounters are generated. See **Chapter Modes** below.                           |
| `requiredExpansions` | list of strings | `[]`      | Expansion codes (from `expansions.json`) that must ALL be enabled for this chapter to appear. |
| `expansionBoundary`  | boolean         | `false`   | Silences the "no static successor" validator warning for intentional AI handoff endings.      |
| `difficulty`         | enum            | —         | `ENTRY`, `EASY`, `MEDIUM`, `HARD`, `LEGENDARY`.                                               |
| `tournament`         | boolean         | `false`   | Marks a chapter as a tournament arc.                                                          |
| `tagTeam`            | boolean         | `false`   | Marks a chapter as a tag team arc.                                                            |

### Chapter Modes

| Mode               | Behaviour                                                                                       |
|:-------------------|:------------------------------------------------------------------------------------------------|
| `AI_ONLY`          | All encounters are AI-generated. No static fallback. Shows "Story Director Offline" without AI. |
| `AI_WITH_FALLBACK` | Uses AI when available; falls back to `staticEncounters` when no AI provider is configured.     |
| `STATIC_ONLY`      | Always uses `staticEncounters`. AI is never called. Safe for offline play.                      |

### Entry & Exit Points

Each chapter can have multiple entry and exit points.
- **Entry Points:** Define when a chapter becomes available to the player.
- **Exit Points:** Define when a chapter is considered complete and what narrative path follows.

Logical evaluation:
- Within a **Point**, all criteria in a single criteria object must be met (**AND**).
- Between multiple **Points**, any one point being active triggers the transition (**OR**).

### Progression Criteria

The following fields can be used in a `criteria` object:

| Field                         | Description                                              |
|:------------------------------|:---------------------------------------------------------|
| `minVictoryPoints`            | Minimum VP required.                                     |
| `maxVictoryPoints`            | Maximum VP (useful for a "lost" / development path).     |
| `minMatchesPlayed`            | Minimum matches played in the current chapter.           |
| `minWins`                     | Minimum wins in the current chapter.                     |
| `tournamentWinner`            | Boolean check for tournament victory.                    |
| `failedToQualify`             | Boolean check for tournament qualification failure.      |
| `wonFinale`                   | Boolean check for chapter finale outcome.                |
| `isChampion`                  | Boolean check if the wrestler currently holds any title. |
| `hasFaction`                  | Boolean check for active faction membership.             |
| `requiredAlignmentType`       | `FACE`, `HEEL`, or `NEUTRAL`.                            |
| `minAlignmentLevel`           | Minimum level on the alignment track (0–5).              |
| `requiredCompletedChapterIds` | List of IDs of chapters that must be finished.           |
| `customEvaluationScript`      | (Experimental) Groovy script for complex logic.          |

---

## Static Encounters (Gamebook Mode)

`staticEncounters` is an array of pre-authored narrative cards used in `STATIC_ONLY` and `AI_WITH_FALLBACK` chapters. Each card has an ID, narrative text, and a list of choices the player can make.

### Sequential vs. Gamebook Routing

By default, cards are shown in **sequential order** — card 0, then card 1, etc. For a true gamebook experience ("win sends you to card 47, loss to card 23"), choices carry explicit routing fields.

**Routing priority** when loading the next card:
1. **Pending match-outcome routing** — if the previous choice had `onWinNextEncounterId` / `onLossNextEncounterId`, the win/loss delta is resolved and the target card is loaded.
2. **`currentEncounterId` in state** — if a non-match choice set `nextEncounterId`, that card is loaded directly.
3. **Sequential fallback** — if no routing is set, the next card in list order is shown (expansion-gated cards are skipped in this mode).

### Encounter Card Structure

```json
{
  "id": "card_intro",
  "title": "Your First Match",
  "narrativeText": "The crowd doesn't know your name yet...",
  "requiredExpansion": null,
  "choices": [
    {
      "id": "go_to_match",
      "label": "Step Into the Ring",
      "text": "It's time to prove yourself.",
      "nextPhase": "MATCH",
      "onWinNextEncounterId": "card_victory",
      "onLossNextEncounterId": "card_defeat",
      "requiredExpansion": null,
      "vpReward": 0,
      "alignmentShift": 0
    }
  ]
}
```

### Static Encounter Fields

| Field               | Type   | Description                                                                                                  |
|:--------------------|:-------|:-------------------------------------------------------------------------------------------------------------|
| `id`                | string | **Unique within the chapter.** Used as routing target. Required.                                             |
| `title`             | string | Short name displayed as the card heading.                                                                    |
| `narrativeText`     | string | The story text shown to the player.                                                                          |
| `requiredExpansion` | string | Expansion code required to show this card. In sequential mode, gated cards are skipped. Null = always shown. |
| `choices`           | array  | List of choices the player can take.                                                                         |

### Choice Fields

| Field                   | Type    | Description                                                                                                  |
|:------------------------|:--------|:-------------------------------------------------------------------------------------------------------------|
| `id`                    | string  | Unique identifier for this choice within the card.                                                           |
| `label`                 | string  | Short button label shown in the UI.                                                                          |
| `text`                  | string  | Full description shown as the tooltip.                                                                       |
| `nextPhase`             | enum    | `MATCH`, `POST_MATCH`, or `BACKSTAGE`. Controls what happens after this choice.                              |
| `nextEncounterId`       | string  | For non-match choices: jump directly to this card ID. Null = advance sequentially.                           |
| `onWinNextEncounterId`  | string  | After a **MATCH win**: jump to this card ID.                                                                 |
| `onLossNextEncounterId` | string  | After a **MATCH loss**: jump to this card ID.                                                                |
| `requiredExpansion`     | string  | Choice is hidden if this expansion is not enabled. Use with an ungated fallback choice. Null = always shown. |
| `vpReward`              | int     | VP awarded immediately (only applied when `nextPhase` is `MATCH`).                                           |
| `alignmentShift`        | int     | Positive = towards Babyface; negative = towards Heel.                                                        |
| `momentumBonus`         | int     | Bonus momentum for the next match.                                                                           |
| `unlockPromo`           | boolean | Unlocks the Promo action in campaign state.                                                                  |
| `unlockAttack`          | boolean | Unlocks the Attack action in campaign state.                                                                 |
| `featureFlags`          | object  | Arbitrary key/value pairs merged into `featureData` (e.g. `{"tournamentWinner": true}`).                     |
| `statusCardKeys`        | array   | Status card keys to assign to the wrestler.                                                                  |
| `outcomeText`           | string  | Immediate narrative shown after the player selects this choice.                                              |

### Expansion-Gated Choices (Conditional Branching)

To create a "if you own expansion X go to card Y, otherwise end here" branch:

```json
{
  "id": "branch_card",
  "title": "What's Next?",
  "narrativeText": "A stranger approaches...",
  "choices": [
    {
      "id": "c_eddie_route",
      "label": "Follow the Legend",
      "text": "An Eddie Guerrero storyline awaits.",
      "nextEncounterId": "card_eddie_intro",
      "requiredExpansion": "EDDIE"
    },
    {
      "id": "c_base_end",
      "label": "Head Backstage",
      "text": "Your chapter here is complete.",
      "nextPhase": "BACKSTAGE"
    }
  ]
}
```

When the EDDIE expansion is **not** enabled, only "Head Backstage" appears. When it **is** enabled, both choices are shown.

> **Rule:** Every encounter card that is **not itself gated** by `requiredExpansion` must have at least one choice with no `requiredExpansion`. This ensures a base-game player can always proceed. The `CampaignChapterSimulationTest` enforces this at build time.

### Expansion-Gated Encounter Cards

An entire card can be gated. In sequential mode, gated cards are **skipped** when the expansion is disabled — the count advances past them transparently.

```json
{
  "id": "card_extreme_event",
  "title": "Extreme Challenge",
  "narrativeText": "Only for the hardcore...",
  "requiredExpansion": "EXTREME",
  "choices": [ ... ]
}
```

---

## Expansion Gating

Expansions are defined in `expansions.json` and enabled per-universe by the admin. Campaign content can be gated at three levels:

| Level     | Field                          | Effect                                                                      |
|:----------|:-------------------------------|:----------------------------------------------------------------------------|
| Chapter   | `requiredExpansions: ["CODE"]` | Chapter never appears in `findAvailableChapters` if any code is disabled.   |
| Encounter | `requiredExpansion: "CODE"`    | Card is skipped in sequential mode; must still be explicit routing target.  |
| Choice    | `requiredExpansion: "CODE"`    | Choice is hidden from the player. Use a fallback choice without this field. |

Valid expansion codes (from `expansions.json`): `BASE_GAME`, `EDDIE`, `EXTREME`, `MATT_CARDONA`, `RUMBLE`, `TRAILBLAZERS`, `ATW_VS_WOW`, `GAIL_KIM`, `HURT_BUSINESS`.

---

## Chapter Simulation Validator

`CampaignChapterSimulationTest` runs automatically on every `mvn test` and catches authoring errors before anyone reaches them in the game. It runs **eight checks**:

| # | Severity | What it checks                                                                                                         |
|:--|:---------|:-----------------------------------------------------------------------------------------------------------------------|
| 1 | FAIL     | Every exit point is reachable under some achievable game state.                                                        |
| 2 | FAIL     | Static encounter IDs are unique within each chapter.                                                                   |
| 3 | FAIL     | Routing targets (`nextEncounterId` / `onWinNextEncounterId` / `onLossNextEncounterId`) exist in the same chapter.      |
| 4 | FAIL     | Static chapters have enough MATCH-phase choices to satisfy `minMatchesPlayed` exit criteria.                           |
| 5 | FAIL     | Expansion codes in `requiredExpansions` / `requiredExpansion` match a known code in `expansions.json`.                 |
| 6 | FAIL     | Every ungated encounter has at least one choice with no `requiredExpansion`.                                           |
| 7 | WARN     | Every chapter exit state has a static successor chapter (AI handoff / expansion boundary logs a warning, never fails). |
| 8 | OUTPUT   | Writes `target/campaign-graph.dot` — a Graphviz visualization of all chapter connections.                              |

To render the graph:

```bash
mvn test -Dtest=CampaignChapterSimulationTest
dot -Tsvg target/campaign-graph.dot -o target/campaign-graph.svg
# or
dot -Tpng target/campaign-graph.dot -o target/campaign-graph.png
```

---

### AI Narrative Integration

Chapters influence the AI Director via:
- `aiSystemPrompt`: Tailors the AI's persona and goals for the chapter.
- **Phase Context:** The system uses the `CampaignPhase` (BACKSTAGE, MATCH, POST_MATCH) to determine which prompt instructions to send to the AI.
- **Match Result Context:** During the `POST_MATCH` phase, the AI receives details about the last match (won/lost, opponent, type) to generate immediate reactions.

## Status Card Logic

Status Cards use strictly **Groovy** syntax for their trigger conditions.

### Available Variables

The following variables are available in the condition evaluation context:
- `momentum`: The wrestler's momentum at the end of the match.
- `loss`: A boolean indicating if the wrestler lost the match (`true`) or won (`false`).

### Common Condition Examples

- `momentum >= 5`: Flips up or down based on reaching high momentum.
- `loss == true`: Discards or flips based on match failure.
- `false`: A static condition that never triggers (useful for statuses that can only be removed by specific narrative prompts).

### Reward Scripting

Campaign chapters and milestones can grant statuses using their internal keys.

**Example `status_cards.json`:**

```json
{
  "key": "status_draw",
  "level1Name": "Draw",
  "level2Name": "Main Eventer",
  "flipUpCondition": "momentum >= 5"
}
```

**Referencing in Campaign Data:**

```json
{
  "statusCardRewards": ["status_draw"]
}
```

