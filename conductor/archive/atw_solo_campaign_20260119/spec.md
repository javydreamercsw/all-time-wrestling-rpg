# Specification: ATW Solo Campaign "All or Nothing"

## Overview

Implement a persistent solo campaign mode for *All Time Wrestling* titled **"All or Nothing" (Season 1)**. This mode features character progression across three chapters, a dynamic Face/Heel alignment system, scriptable backstage actions, and AI-driven narrative branching.

## Functional Requirements

### 1. Persistent Campaign State

- **Database Schema:** Create new relational tables:
  - `Campaign`: Main record for a solo campaign run.
  - `CampaignState`: Tracks current chapter, health, bumps, injuries, skill tokens, and Victory Points (VP).
  - `WrestlerAlignment`: Tracks position on the Face/Heel track (0-10 on each side).
  - `BackstageActionHistory`: Log of actions performed.
- **Health Management:**
  - Persistent "Bumps" and "Injuries".
  - 3 Bumps auto-convert to 1 random Injury Card.
  - Medical Limit: Mandatory removal of injuries if > 2, costing 4 VP each.
  - Penalties: Bumps/Injuries reduce starting health, hand size, or stamina.

### 2. Face/Heel Track System (Groovy Scripted)

- **Mechanics:** Linear track starting at zero.
- **Progression:** Advancing through Backstage Actions (Promo/Attack) or Story Events.
- **Unlocks:** Level-based ability card unlocking (e.g., reaching space 5 unlocks Level 2 and Level 3 cards).
- **Alignment Turn:** Logic to flip the marker to the opposite track while resetting/swapping relevant cards.
- **Scripting Engine:** All track progression rules and ability unlock logic must be implemented via **Groovy scripts** for high flexibility.

### 3. Backstage Actions

- **Actions:** Training (Drive), Recovery (Resilience), Promo (Charisma), and Attack (Brawl - Heels only).
- **Logic:** Dice roll (1d6) per attribute point. Success on 4+.
- **Outcomes:**
  - Training: Earn Skill Tokens (8 tokens = Permanent Ability like Iron Man).
  - Recovery: Remove bumps or injuries.
  - Promo/Attack: Advance alignment track and gain match buffs (Momentum / Opponent Health penalty).

### 4. Chapter Structure

- **Chapter 1 ("Beginnings"):** Easy difficulty, 2 VP Win / -1 VP Loss.
- **Chapter 2 ("The Tournament"):** Medium difficulty, Rival system integration.
- **Chapter 3 ("The Outsider"):** High difficulty, Outsider encounters, 4 VP Win / -2 VP Loss.

### 5. AI Integration

- **Narrative Generation:** AI generates contextual match narrations and backstage promos using the existing AI platform integrations (Gemini/Claude/OpenAI).
- **Story Branching:** AI analyzes alignment and performance to trigger specific "Rival" or "Outsider" story beats.

## UI Requirements

- **Campaign Dashboard:** Hub for current stats and progression.
- **Backstage Action View:** Interactive dice-rolling interface for development actions.
- **Ability Tree UI:** Visual management of skills and unlocked cards.
- **Narrative Viewer:** Immersive component for displaying AI-generated story segments.

## Acceptance Criteria

- [ ] Campaign state survives application restarts and is correctly linked to a user's Wrestler.
- [ ] Face/Heel track progression correctly triggers card unlocks via Groovy scripts.
- [ ] Backstage actions correctly calculate success based on wrestler stats and update state.
- [ ] AI narrations incorporate current campaign context (e.g., injuries, previous promo results).
- [ ] Transition between Chapters 1, 2, and 3 correctly adjusts difficulty and VP rewards.

## Out of Scope

- Multiplayer Campaign modes.
- Real-time 3D animations for backstage actions (stick to 2D/UI-based).
- Character creation within this track (uses existing wrestler entities).

