# Track Specification: Advanced Campaign Chapters

## Overview

This track introduces three distinct advanced campaign chapters designed for experienced players who have established themselves in All Time Wrestling (ATW). These chapters ("Open Challenge", "Faction Warfare", "Authority Feud") provide tailored narrative arcs for veteran players and champions, leveraging existing NPCs for dynamic storytelling.

## Functional Requirements

### Chapter 1: The Fighting Champion (Open Challenge)

1. **Trigger:** Player holds a major championship.
2. **Mechanic:**
   - Introduces the "Open Challenge" segment type.
   - AI selects existing roster members as surprise opponents.
   - High-risk/high-reward structure for fan gain/loss.
3. **Outcome:**
   - **Success:** Transition to "Legendary Reign" status.
   - **Failure:** Loss of title triggers "The Chase" or "Redemption" arcs.

### Chapter 2: Gang Warfare (Faction Warfare)

1. **Trigger:** Player has high heat with a faction leader or is recruited by an existing faction.
2. **Mechanic:**
   - Focus on Tag Team, 6-Man Tag, and Survivor Series style matches.
   - Uses existing Factions defined in `factions.json`.
   - Backstage segments focus on recruitment, betrayal, and loyalty.
3. **Outcome:**
   - **Victory:** Player's faction becomes dominant; potential to become faction leader.
   - **Defeat:** Faction dissolves or player is exiled.

### Chapter 3: Corporate Power Trip (Authority Feud)

1. **Trigger:** Player reaches high tier status or chooses "Anti-Authority" dialogue options.
2. **Mechanic:**
   - Existing GM/Authority NPCs (from `npcs.json`) actively work against the player.
   - Scenarios: Forced handicap matches, unfair referees, sudden rule changes.
3. **Outcome:**
   - **Win:** "Takeover" event where the authority figure is ousted or humbled.
   - **Loss:** Player is "fired" (story reset) or demoted to the bottom of the card.

## Non-Functional Requirements

- **Asset Reuse:** Must use existing `npcs.json`, `wrestlers.json`, and `factions.json` data. No new hardcoded characters.
- **Save Compatibility:** Chapters must be seamlessly playable on existing saves that meet criteria.

## Acceptance Criteria

- [ ] "The Fighting Champion" chapter triggers only for champions and generates valid opponents.
- [ ] "Gang Warfare" chapter correctly identifies and utilizes existing factions.
- [ ] "Corporate Power Trip" chapter correctly assigns roles to existing Authority NPCs.
- [ ] All three chapters have clear branching entry and exit points.

## Out of Scope

- Creation of new NPC assets or Faction data structures (logic must work with what exists).

