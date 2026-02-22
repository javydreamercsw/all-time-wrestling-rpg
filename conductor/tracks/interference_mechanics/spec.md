# Match Interference & Manager Mechanics - Specification

## Overview
Introduce active match interference mechanics for managers and faction members. This system allows characters at ringside to influence match outcomes through strategic actions, adding depth to the gameplay and reinforcing the mechanical value of alliances.

## Core Features

### 1. Interference Actions
- **Referee Distraction:** Temporarily pauses the opponent's momentum gain or allows for illegal moves.
- **Weapon Slide:** Provide a foreign object to a teammate, increasing attack damage for the next few turns.
- **Trip/Ankle Pull:** Reduce opponent's health or stamina directly.
- **Cheap Shot:** A direct strike from ringside, dealing damage and potentially stunning the opponent.

### 2. Detection & Risk System
- **Detection Meter:** Every interference action increases a "Referee Awareness" meter.
- **Ejection:** If awareness reaches 100%, the interceding character is ejected from ringside.
- **Disqualification (DQ):** Blatant interference (e.g., using a weapon seen by the ref) results in an immediate DQ for the beneficiary.
- **Alignment Bias:** Heel managers are more proficient at high-impact, high-risk interference. Face managers typically only interfere to "level the playing field."

### 3. Faction Synergy Integration
- **Affinity Bonus:** High faction affinity reduces the "Awareness" cost of interference actions, representing better timing and coordination.
- **Multiple Interferers:** Larger factions can attempt multiple interferences per match, though with compounding risk.

### 4. NPC AI Behavior
- **Heel Logic:** NPCs will proactively use interference to win, especially if losing.
- **Face Logic:** NPCs will only use "Protective Interference" if the opponent's manager interferes first.

## Technical Goals
- Create an `InterferenceService` to manage actions, risks, and outcomes.
- Update `MatchView` to provide UI buttons for interference actions when a manager or faction member is present.
- Extend the match state to track "Referee Awareness."
- Hook into the `NPCSegmentResolutionService` to allow interference to weight automated match outcomes.
