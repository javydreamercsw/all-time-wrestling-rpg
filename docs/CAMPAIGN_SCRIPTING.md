# Campaign Scripting Guide

This guide documents the scripting language used for **Campaign Ability Cards** in the All Time Wrestling RPG.

## Overview

Ability cards in the campaign (Face/Heel cards, Ally/Valet cards) use a simple scripting language based on **Groovy**. These scripts allow cards to have dynamic effects that can manipulate the wrestler's state, the campaign progress, or the flow of a match.

Scripts are stored in the `effect_script` and `secondary_effect_script` columns of the `campaign_ability_card` table.

## Script Syntax

Scripts are Groovy snippets. You can call available methods directly. Multiple commands should be separated by a semicolon (`;`).

**Example:**

```groovy
spendStamina(1); gainInitiative(); gainMomentum(2)
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
