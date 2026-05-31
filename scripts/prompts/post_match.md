# Prompt: Generate POST_MATCH OutcomeMatrix entries

You are generating content for a wrestling tabletop RPG called "Face to the Mat." The game uses named dice-roll charts to resolve what happens after a match ends. The Post-Match chart fires immediately after the bell to determine the aftermath: celebrations, beatdowns, unexpected challenges, title implications, and crowd reactions that define what the match meant.

Generate **80 unique POST_MATCH entries** covering the full range of post-match scenarios: victory celebrations, heel beatdowns of the loser, surprise challengers, handshakes and sign-of-respect moments, authority figure appearances, media scrums, title match implications, and shocking turns.

## Placeholders

- `{WRESTLER_1}` = the wrestler who won the match (or the aggressor in non-finish scenarios)
- `{WRESTLER_2}` = the wrestler who lost (or the target)

Most entries should reference both. Use the placeholder exactly.

## Effect guidelines

- `heatDelta`: escalation after the bell. Handshake = 0 (omit). Cheap attack on fallen opponent = 3–5. Extended post-match beatdown = 6–8.
- `fanDelta`: {WRESTLER_1}'s fan change. Babyface celebration = positive. Heel post-match attack = negative. Surprise alignment shift = large value either direction.
- `grudgeGradeDelta`: 1–3 for entries that explicitly advance a feud in the aftermath.
- `injuryCaused`: `true` only for entries where a post-match attack is severe enough to cause injury.
- `tvGradeDelta`: +1 for a show-closing memorable moment, -1 for a flat aftermath. Use on ~15% of entries.

Omit any field with no effect.

## Tone variety

Aim for a mix across these types:
1. **Respectful handshake** — both wrestlers acknowledge each other, crowd appreciates it
2. **Heel beatdown** — {WRESTLER_1} attacks {WRESTLER_2} after the bell, sends a message
3. **Surprise challenger** — a third wrestler's music hits and stakes a claim
4. **Title implications** — {WRESTLER_1}'s win earns a future title shot announcement
5. **Alignment tease** — {WRESTLER_1}'s post-match behavior hints at a turn
6. **Manager/faction reaction** — outside parties celebrate, console, or attack
7. **Emotional aftermath** — {WRESTLER_2}'s reaction (tears, rage, stunned silence) becomes the story
8. **Rematch demand** — {WRESTLER_2} immediately demands a rematch on the mic
9. **Authority consequence** — GM/commissioner appears with a match decision consequence
10. **Crowd hijack** — the live crowd's reaction to the result overshadows everything else

## Output format

Output a single valid JSON object in exactly this structure. Use sequential `diceRoll` values starting at 1. Do not include any text outside the JSON.

```json
{
  "name": "Post-Match Scenario",
  "category": "POST_MATCH",
  "description": "Events that occur after a match concludes",
  "entries": [
    {
      "diceRoll": 1,
      "templateText": "{WRESTLER_1} extends a hand to {WRESTLER_2} after the bell — {WRESTLER_2} hesitates, then accepts. The crowd gives a standing ovation.",
      "fanDelta": 900,
      "tvGradeDelta": 1
    },
    {
      "diceRoll": 2,
      "templateText": "{WRESTLER_1} grabs a chair and drives it into {WRESTLER_2}'s spine three times after the bell, standing over them as the crowd rains down boos.",
      "heatDelta": 6,
      "fanDelta": -800,
      "injuryCaused": true
    }
  ]
}
```

