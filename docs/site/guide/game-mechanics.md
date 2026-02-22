# Game Mechanics

Welcome to the Game Mechanics guide. This documentation is automatically generated from the latest game features.

## Cards

The heart of the ATW RPG battle system. Each card represents a move (Strike, Grapple, Aerial, Throw) with specific health and stamina costs and damage effects.

![Cards](/screenshots/mechanic-cards.png)

---

## Decks

Manage wrestler-specific decks. Strategy involves balancing high-damage finishers with efficient setup moves and stamina-recovering taunts.

![Decks](/screenshots/mechanic-decks.png)

---

## Faction Synergy

Factions in All Time Wrestling are more than just groups; they are living units that grow stronger the more their members work together.

### Affinity & Progression
Every faction has an **Affinity** score (0-100%) that represents the chemistry between its members. Affinity increases when:
*   Members participate in the same match or segment.
*   The faction wins a match together (extra bonus).
*   Members appear in major events like Main Events or Premium Live Events (multipliers apply).

### Synergy Levels
As a faction's Affinity grows, it unlocks powerful mechanical bonuses that are applied during matches:

*   **Level 1 (20% Affinity) - Stamina Synergy:** Members recover +5 Stamina when tagged out in tag team matches.
*   **Level 2 (40% Affinity) - Finisher Synergy:** Tag team maneuvers and finishers deal +10% extra damage.
*   **Level 3 (60% Affinity) - Resilience Synergy:** Members receive a bonus to their Resilience when a stablemate is at ringside or in the same match.
*   **Level 4 (80% Affinity) - Momentum Synergy:** "Heat" and momentum carry over more effectively between members during tags.
*   **MAX (100% Affinity) - Legendary Chemistry:** All synergy bonuses are active, making the faction a dominant force.

High-affinity factions also receive a significant boost to their win probability in automated match resolutions.

---

## Match Interference

Matches in ATW RPG are dynamic and often influenced by characters at ringside. Managers and faction members can actively interfere to swing the momentum.

### Ringside Actions
If a wrestler has a manager or a stablemate at ringside, they can perform several actions:
*   **Referee Distraction:** Pause the opponent's momentum or buy time for an illegal recovery.
*   **Weapon Slide:** Provide a foreign object, significantly boosting attack damage for a short duration.
*   **Trip/Ankle Pull:** Directly reduce the opponent's stamina or interrupt an aerial move.
*   **Cheap Shot:** A direct strike from the outside that deals damage and can stun the opponent.

### Referee Awareness & Risk
Every interference attempt is a gamble. The match tracks a **Referee Awareness** meter (0-100%):
*   **Detection:** Each action increases the meter based on its "Risk" level.
*   **Suspicion:** As the meter grows, the chance of success for future interferences decreases.
*   **Ejection (80%):** If the referee becomes too suspicious, the interferer is ejected from ringside.
*   **Disqualification (100%):** Blatant or repeated interference will result in an immediate DQ loss for the beneficiary.

### Strategic Influence
*   **Faction Affinity:** High-affinity factions are better at timing their interference, which reduces the amount of "Awareness" generated per action.
*   **Alignment:** Heel managers are more aggressive and proficient at high-impact interference, while Face managers typically only interfere to counter an opponent's dirty tactics.
*   **AI Retaliation:** In interactive matches, the AI will intelligently decide when to counter-interfere based on the referee's state and their own alignment.

---

## AI Commentary & Narration

The **Story Director** brings every match to life using advanced AI to generate a detailed, structured transcript of the action.

### Structured Transcript
Match narration is presented as a dialogue between the officiating **Narrator** and the **Commentary Team**:

*   **Narrator:** Provides objective, vivid descriptions of the wrestling moves, high spots, and key moments in the match.
*   **Commentators:** Offer character-driven analysis based on their unique personas.
    *   **Face Commentators:** Typically emphasize sportsmanship, technical skill, and the hero's journey.
    *   **Heel Commentators:** May justify rule-breaking, mock the underdogs, and offer controversial perspectives.

### Commentary Teams
Different shows can feature different commentary teams, each with their own chemistry and dynamic. The default **All-Time Broadcast Team** features:

*   **Dara Hoshiko:** An optimistic, analytical voice focusing on history and technique.
*   **Lord Bastian Von Crowe:** A theatrical, snarky color commentator who delights in the villainy of the squared circle.

<feature-showcase id="ai-features-dynamic-commentator-personas" />
