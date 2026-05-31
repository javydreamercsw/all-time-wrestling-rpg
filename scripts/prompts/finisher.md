# Prompt: Generate FINISHER OutcomeMatrix entries

You are generating content for a wrestling tabletop RPG called "Face to the Mat." The game uses named dice-roll charts to resolve finishing-move sequences. The Finisher chart fires when a wrestler attempts their finisher — it determines whether the move lands, is countered, backfires, or triggers a dramatic chain of events that swings the match.

Generate **60 unique FINISHER entries** covering the full range of finishing-move outcomes: clean finisher lands, finisher countered into a submission, kick-out at two-and-a-half, finisher stolen, move botched, opponent plays possum, referee in the way, outside distraction causes a miss, and rare double-finisher moments.

## Placeholders

- `{WRESTLER_1}` = the wrestler going for the finisher
- `{WRESTLER_2}` = the recipient of the finisher attempt

Both should appear in almost every entry. Use the placeholder exactly.

## Effect guidelines

- `fanDelta`: crowd's emotional peak. Clean finisher lands on a crowd favorite = +1500 to +3000. Dramatic kick-out = +1000 to +2000. Counter that swings momentum = +500 to +1500 for {WRESTLER_2} (use negative value since `fanDelta` tracks {WRESTLER_1}'s fans: a counter that helps {WRESTLER_2} hurts {WRESTLER_1}'s fan count). Botch or flat execution = small negative or omit.
- `heatDelta`: 2–5 when the sequence directly escalates rivalry tension (e.g., {WRESTLER_1} hits the finisher and then taunts, or {WRESTLER_2} counters and immediately attacks).
- `injuryCaused`: `true` only for entries describing a landing or impact severe enough to cause injury (e.g., finisher onto exposed concrete, missed move into ring post).
- `tvGradeDelta`: +1 for a genuinely crowd-popping dramatic sequence. Use on no more than 15% of entries.

Omit any field with no effect.

## Tone variety

Aim for a mix across these types:
1. **Clean finish** — finisher lands flush, match effectively over (10 entries)
2. **Kick-out drama** — {WRESTLER_2} kicks out at 2.99, crowd erupts (10 entries)
3. **Counter into submission** — {WRESTLER_2} reverses the finisher into a submission hold (8 entries)
4. **Finisher stolen** — {WRESTLER_2} hits {WRESTLER_1} with their own finishing move (8 entries)
5. **Miss / botch** — {WRESTLER_1} whiffs and crashes, momentum swings (8 entries)
6. **Distraction causes miss** — interference or ringside distraction breaks up the attempt (8 entries)
7. **Desperation counter** — {WRESTLER_2} barely survives and hits a desperation move (8 entries)
8. **Double-down** — both wrestlers are down after a collision of finishers (8 entries) — these entries may omit both placeholders or use both symmetrically

## Output format

Output a single valid JSON object in exactly this structure. Use sequential `diceRoll` values starting at 1. Do not include any text outside the JSON.

```json
{
  "name": "Finisher",
  "category": "FINISHER",
  "description": "Determines outcomes when a wrestler goes for a finishing move",
  "entries": [
    {
      "diceRoll": 1,
      "templateText": "{WRESTLER_1} connects with their finisher dead center — {WRESTLER_2} crumbles and the referee counts three.",
      "fanDelta": 1200,
      "tvGradeDelta": 1
    },
    {
      "diceRoll": 2,
      "templateText": "{WRESTLER_2} slips out of the finisher attempt and rolls {WRESTLER_1} up in a small package — two-count, and both wrestlers scramble to their feet.",
      "fanDelta": -800
    }
  ]
}
```

