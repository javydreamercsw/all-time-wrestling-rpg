# Prompt: Generate PROMO OutcomeMatrix entries

You are generating content for a wrestling tabletop RPG called "Face to the Mat." The game uses named dice-roll charts to resolve storyline outcomes.

Generate **80 unique PROMO entries** covering the full range of promo segment outcomes: championship callouts, crowd-electrifying speeches, devastating verbal takedowns, social media controversies, media appearances, and promo battles where one wrestler gets the better of the other.

## Placeholders

- `{WRESTLER_1}` = the wrestler cutting the promo or with the mic advantage in this moment
- `{WRESTLER_2}` = the target of the promo, or the wrestler being called out / responded to

Not every entry needs both — a solo promo entry may only reference `{WRESTLER_1}`. Use the placeholder exactly as written.

## Effect guidelines

- `fanDelta`: the primary effect of a promo. Strong babyface promo = +500 to +2000. Effective heel promo that generates heat = -300 to -1000. Flat/backfired promo = small negative or omit.
- `heatDelta`: set when the promo directly escalates rivalry tension. 1–3 for verbal shots, 4–6 for face-to-face confrontations.
- `tvGradeDelta`: +1 if the segment is show-saving quality, -1 if it kills the crowd dead. Use sparingly (20% of entries max).
- `grudgeGradeDelta`: 1–2 only for promos that openly escalate a feud to the next level.

Omit any field that has no effect.

## Tone variety

Aim for a mix across these types (roughly 10 entries each):
1. **Championship callout** — {WRESTLER_1} demands a title shot, issues an open challenge, or declares themselves next
2. **Verbal beatdown** — {WRESTLER_1} dismantles {WRESTLER_2} verbally, leaving them humiliated
3. **Comeback promo** — {WRESTLER_2} gets the better of {WRESTLER_1}, crowd goes wild
4. **Crowd connection** — {WRESTLER_1} turns a serious moment into a crowd-popping speech
5. **Heel promo backfires** — {WRESTLER_1} cuts a great heel promo but the crowd starts cheering {WRESTLER_2}
6. **Social media angle** — a tweet, video post, or livestream revelation creates chaos
7. **Interview reveal** — a backstage or press interview drops a bombshell
8. **Authority challenge** — {WRESTLER_1} goes after management or the system itself
9. **Legacy/respect promo** — past history, career achievements, or veteran status invoked
10. **Segment breakdown** — promo escalates to a physical confrontation before security separates them

## Output format

Output a single valid JSON object in exactly this structure. Use sequential `diceRoll` values starting at 1. Do not include any text outside the JSON.

```json
{
  "name": "Promo",
  "category": "PROMO",
  "description": "Outcomes from promo segments affecting crowd reaction and grades",
  "entries": [
    {
      "diceRoll": 1,
      "templateText": "{WRESTLER_1} grabs the mic and cuts a blistering promo declaring they are done waiting — {WRESTLER_2} gets a title match next week or {WRESTLER_1} walks.",
      "heatDelta": 3,
      "fanDelta": 800
    },
    {
      "diceRoll": 2,
      "templateText": "{WRESTLER_1}'s promo falls flat as the crowd chants for {WRESTLER_2} throughout, completely undercutting the moment.",
      "fanDelta": -600,
      "tvGradeDelta": -1
    }
  ]
}
```

