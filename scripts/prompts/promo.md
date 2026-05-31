# Prompt: Generate PROMO OutcomeMatrix entries

You are generating content for a wrestling tabletop RPG called "Face to the Mat." The game uses named dice-roll charts to resolve storyline outcomes.

Generate **80 unique PROMO entries** covering the full range of promo segment outcomes: championship callouts, crowd-electrifying speeches, devastating verbal takedowns, social media controversies, media appearances, and promo battles where one wrestler gets the better of the other.

## Placeholders

- `FAVORED` = the wrestler cutting the promo or with the mic advantage in this moment
- `UNDERDOG` = the target of the promo, or the wrestler being called out / responded to

Not every entry needs both — a solo promo entry may only reference `FAVORED`. Use the placeholder exactly as written.

## Effect guidelines

- `fanDelta`: the primary effect of a promo. Strong babyface promo = +500 to +2000. Effective heel promo that generates heat = -300 to -1000. Flat/backfired promo = small negative or omit.
- `heatDelta`: set when the promo directly escalates rivalry tension. 1–3 for verbal shots, 4–6 for face-to-face confrontations.
- `tvGradeDelta`: +1 if the segment is show-saving quality, -1 if it kills the crowd dead. Use sparingly (20% of entries max).
- `grudgeGradeDelta`: 1–2 only for promos that openly escalate a feud to the next level.

Omit any field that has no effect.

## Tone variety

Aim for a mix across these types (roughly 10 entries each):
1. **Championship callout** — FAVORED demands a title shot, issues an open challenge, or declares themselves next
2. **Verbal beatdown** — FAVORED dismantles UNDERDOG verbally, leaving them humiliated
3. **Comeback promo** — UNDERDOG gets the better of FAVORED, crowd goes wild
4. **Crowd connection** — FAVORED turns a serious moment into a crowd-popping speech
5. **Heel promo backfires** — FAVORED cuts a great heel promo but the crowd starts cheering UNDERDOG
6. **Social media angle** — a tweet, video post, or livestream revelation creates chaos
7. **Interview reveal** — a backstage or press interview drops a bombshell
8. **Authority challenge** — FAVORED goes after management or the system itself
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
      "templateText": "FAVORED grabs the mic and cuts a blistering promo declaring they are done waiting — UNDERDOG gets a title match next week or FAVORED walks.",
      "heatDelta": 3,
      "fanDelta": 800
    },
    {
      "diceRoll": 2,
      "templateText": "FAVORED's promo falls flat as the crowd chants for UNDERDOG throughout, completely undercutting the moment.",
      "fanDelta": -600,
      "tvGradeDelta": -1
    }
  ]
}
```

