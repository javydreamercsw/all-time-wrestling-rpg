# Prompt: Generate FEUD_ANGLE OutcomeMatrix entries

You are generating content for a wrestling tabletop RPG called "Face to the Mat." The game uses named dice-roll charts to resolve storyline outcomes. Each chart row is a narrative template with optional mechanical effect values.

Generate **100 unique FEUD_ANGLE entries** covering the full dramatic range of wrestling storylines: betrayals, alliances forming and breaking, contract disputes, personal vendettas, manager interference, career threats, family drama, and backstage confrontations.

## Placeholders

- `{WRESTLER_1}` = the dominant/heel/higher-momentum wrestler in this moment
- `{WRESTLER_2}` = the face/lower-momentum wrestler being targeted or challenged

Use both in every entry unless the event is clearly about one person only (e.g., a character decision entry may only reference {WRESTLER_1}). Always use the placeholder exactly — no "the {WRESTLER_1}" or "a {WRESTLER_2}."

## Effect guidelines

- `heatDelta`: rivalry heat between the two wrestlers. Low confrontation = 1–2. Heated argument = 3–4. Physical attack or major betrayal = 5–8.
- `fanDelta`: fan count change for {WRESTLER_1}. Heel acts lose fans (negative). Face moments gain fans (positive). Neutral/ambiguous = omit.
- `grudgeGradeDelta`: how much the grudge between them escalates. Minor incident = 1. Major escalation = 2–3. Use sparingly.
- `injuryCaused`: only `true` for entries that explicitly involve a physical attack severe enough to risk injury.

Omit any field that has no effect — do not include `0` values.

## Tone variety

Aim for a mix across these types (roughly 10 entries each):
1. **Betrayal** — a partner turns, an alliance ends, trust is broken
2. **Alliance forming** — unexpected teams, rivals unite against a common enemy
3. **Personal vendetta** — family insults, career shots, disrespect, history invoked
4. **Escalation** — physical confrontation, attack, ambush
5. **De-escalation / near-miss** — conflict avoided, mediated, or temporarily cooled
6. **Contract / business angle** — management, money, title opportunities as leverage
7. **Character moment** — a single wrestler's decision that reveals alignment shift
8. **Crowd-driven** — fan reaction forces a moment, chants, crowd interference
9. **Authority figure** — GM/commissioner involvement changes the angle
10. **Media / external** — press, social media, interview reveals a new angle

## Output format

Output a single valid JSON object in exactly this structure. Use sequential `diceRoll` values starting at 1. Do not include any text outside the JSON.

```json
{
  "name": "Feud Angle",
  "category": "FEUD_ANGLE",
  "description": "Storyline developments that drive ongoing rivalries and generate dramatic twists",
  "entries": [
    {
      "diceRoll": 1,
      "templateText": "{WRESTLER_1} publicly accuses {WRESTLER_2} of stealing their spotlight, demanding a match to settle it once and for all.",
      "heatDelta": 3,
      "fanDelta": -200
    },
    {
      "diceRoll": 2,
      "templateText": "{WRESTLER_2} reveals footage of {WRESTLER_1} cheating in their last encounter, sending the locker room into chaos.",
      "heatDelta": 4,
      "grudgeGradeDelta": 1
    }
  ]
}
```

