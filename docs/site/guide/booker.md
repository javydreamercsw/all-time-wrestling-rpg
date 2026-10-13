# Booker

Welcome to the Booker guide. This documentation is automatically generated from the latest game features.

## Show Planning — Stipulation Required Error

When a rivalry with heat ≥ 30 is on the card but has no match rule (stipulation), approval is blocked. The error dialog lists each affected rivalry. Edit the segment and add a rule such as Steel Cage or Last Man Standing before approving.

![Show Planning — Stipulation Required Error](/screenshots/booker-show-planning-stipulation-error.png)

---

## Show Planning — Proposed Card

After the AI proposes segments, the booker reviews the card before approving. Rivalries with heat ≥ 10 that are not on the card will trigger an advisory when the Approve button is clicked.

![Show Planning — Proposed Card](/screenshots/booker-show-planning-proposed-card.png)

---

## Show Planning — Unbooked Rivalries Warning

When rivalries with heat ≥ 10 are not on the proposed card, a confirmation dialog lists each unbooked feud. The booker can go back and add them, or acknowledge and approve anyway — unbooked rivalries carry over to future shows.

![Show Planning — Unbooked Rivalries Warning](/screenshots/booker-show-planning-must-book-warning.png)

---

## QR Code Match Share

The QR share button on a match segment opens a dialog with a scannable QR code. The URL uses the machine's LAN IP address so phones on the same network can navigate directly to the match page.

![QR Code Match Share](/screenshots/booker-show-detail-qr-code-share.png)

---

## Show Calendar

The Show Calendar provides a monthly view of every scheduled show in the promotion. Each show appears as a colour-coded event tile on its booked date. Click any tile to jump directly to that show's detail page. Navigate between months using the prev/next arrows, or click Today to snap back to the current date.

![Show Calendar](/screenshots/show-calendar-overview.png)

---

## Season List

Browse all seasons with their status, show count, and date range. Click 'Details' on any season to open its full detail page, including the awards ceremony results.

![Season List](/screenshots/booker-season-list.png)

---

## Season Detail

Full season breakdown showing key stats (status, show count, duration) and the Awards Ceremony section listing Wrestler of the Year, Most Improved, and Most Decorated winners.

![Season Detail](/screenshots/booker-season-detail.png)

---

## Tournament Bracket Generator

Plan and run tournaments directly from the Booker dashboard. Choose from Single Elimination or Round Robin formats, auto-seed wrestlers by fan count or pick them manually, and book each round across your scheduled shows.

![Tournament Bracket Generator](/screenshots/booker-tournament-list.png)

---

## Tournament Detail — Scheduled

A freshly created tournament shows all entrants seeded and ready. The 'Start Tournament' button generates the first-round bracket. Entrants are ranked by seed — 1 vs 8, 2 vs 7, and so on.

![Tournament Detail — Scheduled](/screenshots/booker-tournament-detail-scheduled.png)

---

## Tournament Detail — In Progress

Live tournament view shows completed round results and the pending next round. The booker can book the upcoming round onto any scheduled show and record match winners as each event is completed.

![Tournament Detail — In Progress](/screenshots/booker-tournament-detail-in-progress.png)

---

## Story Arc Wizard

Three-step wizard for scripting a feud arc: select wrestlers, name the arc and choose its length, then add ordered beats (match type, stipulation, winner control).

![Story Arc Wizard](/screenshots/booker-story-arc-wizard.png)

---

## Rivalry Detail

View rivalry details including heat, storyline notes, and dates. Use the Story Arc button to script a multi-beat feud arc for this rivalry.

![Rivalry Detail](/screenshots/booker-rivalry-detail.png)

---

## Rivalry List

Browse active rivalries between wrestlers — ended feuds are filtered out by default; switch the Status filter to All or Ended to see them. Click any row to open the rivalry detail view where you can manage story arc scripts.

![Rivalry List](/screenshots/booker-rivalry-list.png)

---

## Add Beat to Story Arc

Append new beats to an existing story arc after it has been saved. The Add Beat dialog offers the same match type, stipulation, winner control and notes fields as the creation wizard, plus external participants — an opponent and run-in extras from outside the feud, with an AI-assisted opponent suggestion.

![Add Beat to Story Arc](/screenshots/booker-story-arc-add-beat.png)

---

## Story Arc Details

Step 2 names the arc (pre-filled from the selected wrestlers) and picks its length: Short spans 1 PLE, Medium 2, Long 3. The PLE count is a hard ceiling — once a feud has appeared on that many premium live events, it must culminate.

![Story Arc Details](/screenshots/booker-story-arc-details.png)

---

## Story Arc Beats

Step 3 defines the match sequence. Each beat picks a match type and optional stipulation, and decides who controls the outcome: the booker names the planned winner, the AI picks based on story notes, or the system rolls. Mark a beat as the Culmination / Blowoff to close the feud. Roster wrestlers outside the arc can join a beat as an external opponent or run-in extra.

![Story Arc Beats](/screenshots/booker-story-arc-beats.png)

---

## Edit Story Arc Beat

Pending beats can be retooled at any time before they play out. The edit dialog loads the beat's current match type, stipulation, winner control, planned winner, blowoff flag, notes and external participants — saving re-runs the same validation as creation and keeps any show or PLE reservation attached to the beat.

![Edit Story Arc Beat](/screenshots/booker-story-arc-edit-beat.png)

---

## Story Arc Management

Every saved arc lives on the rivalry detail as a card with a status badge (Active, Completed, Cancelled). The beat grid lists each beat in order — match type, stipulation, external participants, winner control, blowoff marker and status. Pending beats can be edited or removed and renumber automatically; the arc itself can be renamed, extended with more beats, or cancelled before its culmination.

![Story Arc Management](/screenshots/booker-story-arc-card.png)

---

## Show Planning — Tournament Slots on the Card

A one-time tournament attached to this show previews its slots on the proposed card: the payoff (and paced round matches on weekly shows) each appear as a grid row with a Tournament source badge. Seeded brackets preview the real match-ups; unknowable ones show placeholder teams. Delete a row to skip that slot on this show — approving books the slots that remain, exactly once.

![Show Planning — Tournament Slots on the Card](/screenshots/booker-show-planning-tournament-slots.png)

---

## Faction Rivalry List

Track active feuds between factions — the Status filter defaults to Active and can show All or Ended faction rivalries. Add heat to escalate a faction feud toward a war match.

![Faction Rivalry List](/screenshots/booker-faction-rivalry-list.png)

---

## Tournament Creation Wizard

Create a tournament in two steps. Step 1 names the tournament and picks the format (Single Elimination or Round Robin), an optional linked championship, start date, and allowed rules — the seeding step stays locked until these are set. Linking a championship also unlocks the contender-deciding option: the tournament winner becomes the title's #1 contender instead of challenging. Step 2 chooses how entrants are seeded: automatically by fan count (with a live match-up preview of what Create will build), manual wrestler picking, or deferred so a paired show can seed it on approval.

![Tournament Creation Wizard](/screenshots/booker-tournament-creation-wizard.png)

---

## Editing Tournament Seeds

Before the bracket is generated, seeds stay editable. Move an entrant up or down to change its seed — the round-1 pairing is 1 vs last, 2 vs second-to-last, so reordering directly reshapes the match-ups — or swap in a different wrestler while keeping the seed. When the tournament is linked to a championship, the current titleholder is excluded from seeding and from the swap picker: the champion cannot win the belt from themselves. Seeds lock once the tournament starts.

![Editing Tournament Seeds](/screenshots/booker-tournament-seed-editing.png)

---

## Tournament Bracket Generator — Arrange Match-ups

Beyond dragging rows to reorder seeds, the round-1 opponents can be hand-picked: each match pairs its wrestlers at the outer seed positions (1 vs last, 2 vs second-to-last), so Start Tournament generates exactly the arranged match-ups. Odd-sized brackets keep a bye at the middle seed.

![Tournament Bracket Generator — Arrange Match-ups](/screenshots/booker-tournament-arrange-matchups.png)

---

## One-Time Tournament on a Host Show

For a one-off tournament, attach it directly to a show instead of pairing it with a PLE template (that stays reserved for recurring tournaments). Pick a host show and the payoff books there exactly once — rounds pace automatically onto the weekly shows before it. The payoff match type and rule default to One-on-One but can be anything, e.g. a Free-for-All TLC match for a title.

![One-Time Tournament on a Host Show](/screenshots/booker-tournament-host-show.png)

---

## Dashboard with News Ticker

Latest headlines and rumors are visible directly on the booker dashboard.

![Dashboard with News Ticker](/screenshots/booker-dashboard-news-ticker.png)

---

## Show Planning

The Show Planning interface allows you to book matches and segments for an upcoming show. Drag and drop wrestlers to create matches, set stipulations, and define segment types.

![Show Planning](/screenshots/booker-show-planning.png)

---

## Match Narration

Bring your matches to life with AI-generated or manual narration. The Story Director uses match participants, rules, and outcomes to weave a compelling narrative of the action. The new structured transcript format separates objective action from character-driven commentary.

![Match Narration](/screenshots/booker-match-narration.png)

---

## Show History

View a complete history of all booked shows. Click on any show to view its detailed results, ratings, and match history.

![Show History](/screenshots/booker-show-history.png)

---
