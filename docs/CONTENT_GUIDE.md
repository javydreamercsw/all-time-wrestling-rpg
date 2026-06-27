# Content Management Guide

This guide explains how to manage and add new content (wrestlers, cards, and sets) to the All Time Wrestling RPG.

## Data Initialization

The application uses JSON files located in `src/main/resources/` to initialize the database on startup. The `DataInitializer` class is responsible for scanning these files and syncing them with the database.

## Card Sets

Card sets are defined in `src/main/resources/sets.json`.

**Structure:**

```json
[
  {
    "name": "All Time Wrestling",
    "set_code": "ATW"
  },
  ...
]
```

To add a new set, add an entry to this file. The `set_code` is used to link cards to the set.

## Title Abilities

Titles can have scripted abilities that influence the AI narration of a match. These are defined in `src/main/resources/championships.json` using the `effect_script` field.

**Example:**

```json
{
  "name": "ATW World",
  "tier": "MAIN_EVENTER",
  "championshipType": "SINGLE",
  "effect_script": "gainInitiative()"
}
```

### Available Title Methods

Title scripts use a subset of the logic described in the [Campaign Scripting Guide](CAMPAIGN_SCRIPTING.md), specifically:

| Method                      | Description                                                               |
|:----------------------------|:--------------------------------------------------------------------------|
| `gainInitiative()`          | Explicitly tells the AI that the champion starts with the initiative.     |
| `gainHitPoints(int amount)` | Increases the champion's starting HP for the match.                       |
| `modifyRoll(int modifier)`  | Informs the AI that the champion has a one-time bonus to a critical roll. |

## Cards

Cards are stored in the `src/main/resources/cards/` directory. Each card set has its own JSON file named after its `set_code` (e.g., `ATW.json`, `BBL.json`).

The `DataInitializer` automatically scans all `*.json` files in this directory.

**Structure of a card file:**

```json
[
  {
    "name": "Shoulder Tackle",
    "type": "Strike",
    "damage": 1,
    "stamina": 1,
    "target": 2,
    "momentum": 1,
    "signature": false,
    "finisher": false,
    "taunt": false,
    "recover": false,
    "pin": false,
    "number": 60,
    "set": "ATW"
  },
  ...
]
```

### Card Attributes

- `name`: The display name of the card.
- `type`: `Strike`, `Throw`, `Grapple`, `Aerial`, `Submission`.
- `damage`: Base damage dealt to the opponent.
- `stamina`: Stamina cost to play the card.
- `target`: The target difficulty for the move.
- `momentum`: Momentum gained by the player.
- `signature`: Boolean, if true, this is a signature move.
- `finisher`: Boolean, if true, this is a finisher move.
- `taunt`: Boolean, if true, the move acts as a taunt.
- `recover`: Boolean, if true, the move helps the wrestler recover.
- `pin`: Boolean, if true, the move triggers a pin attempt.
- `number`: The unique number of the card within its set.
- `set`: The `set_code` of the set this card belongs to.

## Wrestlers

Wrestlers are defined in `src/main/resources/wrestlers.json` (and optionally `wrestlers-extra.json`).

**Structure:**

```json
[
  {
    "name": "Bobby Lashley",
    "alignment": "HEEL",
    "health": 10,
    "stamina": 10,
    "resilience": 3,
    "charisma": 2,
    "brawl": 3,
    "heritageTag": "Colorado, USA",
    "description": "...",
    "gender": "MALE",
    "expansion_code": "HURT_BUSINESS",
    "manager": "MVP"
  }
]
```

The `expansion_code` field controls which expansion must be enabled for this wrestler to appear. Omit it (or use `"BASE_GAME"`) for wrestlers that are always available. Valid codes match the entries in `src/main/resources/expansions.json`.

### Heritage Tags

The `heritageTag` field supports **comma-delimited lists**. If a wrestler has multiple tags (e.g., `"Texas, USA"`), they will receive the **10% Home Territory bonus** when competing in a location that matches *any* of those tags.

## Decks

Wrestler decks are defined in `src/main/resources/decks.json`.

**Structure:**

```json
[
  {
    "wrestler": "Bobby Lashley",
    "cards": [
      {
        "number": 1,
        "set": "BBL",
        "amount": 1
      },
      ...
    ]
  }
]
```

Each wrestler entry links cards by their `number` and `set_code`.

## Segment Types

Segment types define the kind of action in a show (One on One, Tag Team, Promo, etc.) and are loaded from `src/main/resources/segment_types.json`.

**Structure:**

```json
[
  {
    "name": "One on One",
    "description": "Traditional singles wrestling match between two competitors",
    "playerAmount": 2,
    "expansion_code": "BASE_GAME",
    "guide": { ... }
  }
]
```

The optional `guide` field holds in-game play instructions rendered in the "How to Play" dialog during a match. See [Play Guide Format](#play-guide-format) below.

## Segment Rules

Segment rules define stipulations or special conditions applied to a segment (No DQ, Cage Match, etc.) and are loaded from `src/main/resources/segment_rules.json`.

**Structure:**

```json
[
  {
    "name": "No DQ",
    "description": "No Disqualification: ...",
    "requiresHighHeat": false,
    "noDq": true,
    "bumpAddition": "ALL",
    "expansion_code": "BASE_GAME",
    "rules": { ... }
  }
]
```

- `requiresHighHeat`: if `true`, the rule only appears as an option when the rivalry has high heat.
- `noDq`: marks the match as no-disqualification.
- `bumpAddition`: one of `NONE`, `PHYSICAL`, `ALL` — controls which bump cards are added to the draw pile.

The optional `rules` field holds in-game play instructions. See [Play Guide Format](#play-guide-format) below.

## Play Guide Format

Both segment types (`guide`) and segment rules (`rules`) share the same play guide structure. It is rendered in the **How to Play** dialog during a match — type sections appear first as "Base Rules", followed by any applied rule's sections.

The guide has two top-level variant keys:

| Key | When shown |
|-----|-----------|
| `solo` | Solo (player vs NPC) play |
| `multiplayer` | Head-to-head multiplayer play |

Each variant is an object with any combination of the following optional text fields. Blank or absent fields are silently skipped in the rendered output:

| Field | Purpose |
|-------|---------|
| `overview` | One-paragraph summary of this match type/stipulation |
| `setup` | Board/card setup instructions before the match begins |
| `attacking` | How to play cards and resolve attacks |
| `defending` | How to respond to the opponent's attacks |
| `winCondition` | How to win (pinfall, KO, etc.) |
| `npcRecovery` | How the NPC/Automa recovers health or stamina |
| `topOfCageStruggle` | Cage-specific rule for the top-of-cage struggle phase |
| `npcWinConditions` | Conditions under which the NPC wins |
| `concepts` | Multiplayer-specific core concepts |
| `gameplayChanges` | How multiplayer differs from the base rules |
| `modeSpecificAbilities` | Special abilities or card substitutions for this mode |
| `gameEndConditions` | Conditions that end the match in multiplayer |

**Minimal example (solo only):**

```json
"guide": {
  "solo": {
    "overview": "A standard singles match where you face one NPC opponent.",
    "setup": "Set health and stamina cubes on your board and draw 5 starting cards.",
    "winCondition": "Pin the opponent by playing a card with a PIN icon and winning the kick-out roll."
  }
}
```

**Full example (solo + multiplayer):**

```json
"rules": {
  "solo": {
    "overview": "No DQ: weapons are legal and cannot cause disqualification.",
    "setup": "Place six weapon cards face-up beside the board.",
    "attacking": "During your Offensive Ability Window you may trigger a weapon card instead of a standard ability.",
    "defending": "Standard blocking applies. Roll a defense die to attempt a Weapon Counter.",
    "winCondition": "Standard pinfall or submission."
  },
  "multiplayer": {
    "concepts": "No-DQ rules apply to all participants; weapon cards are always in play.",
    "gameplayChanges": "Players may trigger weapons after a successful attack roll.",
    "gameEndConditions": "Standard pinfall or submission; Last Man Standing variant ends on a failed 10-count."
  }
}
```

## Best Practices

1. **Validation**: Always run `DataInitializerTest` and `DataInitializerIntegrationTest` after modifying JSON files to ensure they are valid and all references (sets, card numbers) are correct.
2. **Formatting**: Run `./mvnw spotless:apply` to keep the JSON files formatted according to the project style.
3. **Modularity**: When adding a new wrestler with a dedicated set of moves, create a new JSON file in `src/main/resources/cards/` for those moves.

