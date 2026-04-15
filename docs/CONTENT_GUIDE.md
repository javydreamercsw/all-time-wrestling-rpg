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
    "set": "HURT_BUSINESS",
    "manager": "MVP"
  }
]
```

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

## Best Practices

1. **Validation**: Always run `DataInitializerTest` and `DataInitializerIntegrationTest` after modifying JSON files to ensure they are valid and all references (sets, card numbers) are correct.
2. **Formatting**: Run `./mvnw spotless:apply` to keep the JSON files formatted according to the project style.
3. **Modularity**: When adding a new wrestler with a dedicated set of moves, create a new JSON file in `src/main/resources/cards/` for those moves.

