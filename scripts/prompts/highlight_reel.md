# Prompt: Generate HIGHLIGHT_REEL OutcomeMatrix entries

You are generating content for a wrestling tabletop RPG called "Face to the Mat." The game uses named dice-roll charts to resolve TV segment outcomes. The Highlight Reel chart fires during television segments to determine what in-arena moment happens and how the crowd reacts.

Generate **80 unique HIGHLIGHT_REEL entries** covering the full spectrum of live in-arena moments: surprise returns, fan interactions, title presentations, contract signings gone wrong, in-ring celebrations interrupted, backstage segments shown on the big screen, and unexpected crowd-driven moments.

## Placeholders

- `{WRESTLER_1}` = the wrestler with the spotlight / segment focus
- `{WRESTLER_2}` = the wrestler who responds to or is affected by the moment

Not every entry needs both — a return or solo spotlight entry may only reference `{WRESTLER_1}`. Use the placeholder exactly.

## Effect guidelines

- `fanDelta`: primary effect. Huge crowd moment = +1000 to +3000. Crowd boo = -500 to -1500. Flat segment = omit.
- `heatDelta`: only for entries where the moment directly creates or escalates rivalry tension (1–5).
- `tvGradeDelta`: +1 for moments that make the show memorable, -1 for moments that kill momentum. Use on ~20% of entries.
- `grudgeGradeDelta`: 1–2 only for entries that visibly escalate a feud during the segment.
- `injuryCaused`: `true` only for entries involving a sudden attack during the segment.

Omit any field with no effect.

## Tone variety

Aim for a mix across these types:
1. **Surprise return** — a wrestler returns from absence/injury/hiatus to a massive reaction
2. **Debut** — an unknown or NXT/development talent makes their main roster entrance
3. **Contract signing** — goes smoothly, or dramatically goes off the rails
4. **Title presentation ceremony** — celebration interrupted or not
5. **Fan interaction** — {WRESTLER_1} connects with (or alienates) ringside fans
6. **Big screen reveal** — footage, video message, or exclusive interview airs on the screen
7. **Impromptu challenge** — {WRESTLER_1} calls out {WRESTLER_2} right now, forcing an unscheduled match
8. **Celebrity guest** — a non-wrestler presence creates an unexpected moment
9. **Crowd takes over** — the live crowd hijacks the segment with chants or reactions
10. **Segment disrupted** — outside interference, power outage, brawl spills in from backstage

## Output format

Output a single valid JSON object in exactly this structure. Use sequential `diceRoll` values starting at 1. Do not include any text outside the JSON.

```json
{
  "name": "Highlight Reel",
  "category": "HIGHLIGHT_REEL",
  "description": "TV segment narrative outcomes affecting Grudge and TV Grades",
  "entries": [
    {
      "diceRoll": 1,
      "templateText": "The arena goes dark and {WRESTLER_1}'s music hits — a surprise return that brings the crowd to its feet.",
      "fanDelta": 2500,
      "tvGradeDelta": 1
    },
    {
      "diceRoll": 2,
      "templateText": "{WRESTLER_1} approaches ringside and hands their championship belt to a young fan in the front row, a moment that defines the night.",
      "fanDelta": 1800
    }
  ]
}
```

