# Prompt: Generate MATCH_FLOW OutcomeMatrix entries

You are generating content for a wrestling tabletop RPG called "Face to the Mat." The game uses named dice-roll charts to resolve in-match events. The Match Flow chart fires during a match to determine momentum shifts, interference, environmental hazards, and in-ring incidents that change the tide.

Generate **80 unique MATCH_FLOW entries** covering the full range of in-match events: momentum swings, referee issues, outside interference, environmental spots, near-falls, submission attempts, illegal tactics, crowd surges, and injury scares.

## Placeholders

- `FAVORED` = the wrestler currently in control / with momentum
- `UNDERDOG` = the wrestler currently on the receiving end

Both should appear in most entries since match flow is inherently about the interaction between two wrestlers. Use placeholders exactly.

## Effect guidelines

- `heatDelta`: how much this moment escalates rivalry tension. Cheap shot = 1–2. Blatant cheat or low blow = 3–4. Extended beatdown or foreign object = 5–6.
- `fanDelta`: crowd's emotional reaction to FAVORED. Face comeback = positive. Heel cheating = negative. Omit for neutral moments.
- `injuryCaused`: `true` only for entries that explicitly describe an injury-causing spot (landing awkwardly, posted, thrown into steps, etc.).
- `tvGradeDelta`: +1 for a match-saving dramatic spot, -1 for a botch or flat moment. Use on ~15% of entries.

Omit any field with no effect.

## Tone variety

Aim for a mix across these types:
1. **Momentum shift** — UNDERDOG suddenly fires back and the crowd erupts
2. **Heel cheating** — FAVORED uses the ropes, a distraction, or a foreign object
3. **Near fall drama** — a convincing two-count that had everyone fooled
4. **Referee issue** — ref bump, slow count, missed DQ, or disputed call
5. **Outside interference** — a third party gets involved at ringside
6. **Environmental spot** — steel steps, barricade, announce table, ring post
7. **Submission drama** — UNDERDOG catches FAVORED in a submission, crowd on edge
8. **Injury scare** — a wrestler appears hurt, match pauses, tension builds
9. **Crowd surge** — live crowd energy directly affects the wrestlers
10. **Desperation spot** — UNDERDOG pulls out a move or counter out of nowhere

## Output format

Output a single valid JSON object in exactly this structure. Use sequential `diceRoll` values starting at 1. Do not include any text outside the JSON.

```json
{
  "name": "Match Flow",
  "category": "MATCH_FLOW",
  "description": "Controls the ebb and flow of match momentum",
  "entries": [
    {
      "diceRoll": 1,
      "templateText": "UNDERDOG absorbs a brutal shot from FAVORED and fires back with three unanswered strikes — the crowd comes alive.",
      "fanDelta": 600
    },
    {
      "diceRoll": 2,
      "templateText": "FAVORED pulls the referee between themselves and UNDERDOG, using the distraction to land a low blow.",
      "heatDelta": 3,
      "fanDelta": -400
    }
  ]
}
```

