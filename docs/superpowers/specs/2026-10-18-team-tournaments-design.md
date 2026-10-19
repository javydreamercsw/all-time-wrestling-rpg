# Team-Based Tournaments — Design

Date: 2026-10-18
Issue: ATW-01g2
Branch: `feature/ATW-01g2-team-tournaments` (off `release/2.X.X`)

## Intent and why

Tournaments today are individual-wrestler only: `TournamentEntry` requires a single
`wrestler_id`, and all three bracket formats (single elimination, round robin, qualifier
groups) seed and pair individual entries. The first tournament type this feature enables is
the classic tag team / trios tournament that crowns **inaugural team champions** (Dusty Cup
style) for a `ChampionshipType.TEAM` title. #1-contender tournaments for teams come later,
on the same foundation.

The pieces underneath are already team-ready:

- `Title.championshipType` is `SINGLE | TEAM`.
- `TitleReign.champions` is a `Set<Wrestler>`; display names already join as "A & B".
- `Segment` participants carry a `teamNumber` (side 1 vs side 2), so tag/trios *matches*
  book fine today.
- `WellKnownSegmentType` has `TAG_TEAM` (no trios type yet).
- `teams.json` + `TeamSync` (a `DataSyncContributor`) already seed predefined tag teams by
  wrestler name at startup.

The gap is confined to the team entity shape, the tournament entrant model, and the
wizard/booking path.

## Goals

1. **Team size concept** — an explicit `TeamSize` enum (`DUO` count 2, `TRIO` count 3) on
   `Team`, not derived from member count. Which match types a team can enter follows from
   it. Extensible to more sizes later.
2. **Team as ordered member list** — `Team` restructures from `wrestler1`/`wrestler2`
   columns to an ordered `team_member` child table (slot 0..n-1, same slot pattern as
   `TournamentMatchParticipant`). Duos keep working: `getPartner()`, `getMemberNames()`,
   faction helpers, and every existing duo call site keep their behavior.
3. **Predefined team registry** — `teams.json` gains an ordered `members` array (names) and
   a `teamSize` field (`DUO` default for back-compat; `wrestler1`/`wrestler2` stay accepted
   as deprecated aliases). `TeamSync` reads the new shape, stays idempotent, and seeds
   trios as readily as duos.
4. **Team entrants in tournaments** — `TournamentEntry` gains a nullable `team_id` next to
   `wrestler_id`; exactly one is set (DB check constraint). A new `Tournament.teamSize`
   (null = singles) gates entrant type and must match the linked title's team size when a
   TEAM title is linked.
5. **Formats work unchanged** — the three formats pair `TournamentEntry` objects and don't
   care what's inside. Entrant display names resolve to team names for team entries.
6. **Inaugural champions** — the payoff for a team tournament books as Tag Team (size 2) or
   Trios (size 3); the winning team's members flow into `TitleReign.champions`.
7. **Both entrant sources in the wizard** — pick from the predefined registry (filtered by
   team size, gender, universe, active status) or assemble a one-off team in the wizard;
   the one-off has an optional "save to registry" checkbox (default off).
8. **Seeding & validation** — existing seed logic applies at team level; the tournament's
   gender restriction requires *every* member to satisfy it; a wrestler may appear in only
   one entry per tournament (team or individual).
9. **Team fans & rankings rework** — teams get their own fans counter (`Team.fans`);
   team title rankings sort by team fans instead of summing member fans. Teams accrue
   fans from **every** team segment appearance (win or lose); `TitleReign` gains a
   nullable team reference so champion-team matching is exact.
10. **One-off vs registry teams** — every team is a persisted `Team` row with a
    `TeamOrigin` flag (`REGISTRY | AD_HOC`). Wizard-assembled one-offs persist as AD_HOC;
    "save to registry" flips them to REGISTRY; the tournament registry picker offers only
    REGISTRY teams. Auto-named one-offs get uniquified names (unique `team.name` holds).

## Non-goals (v1)

- Contender-deciding (`contenderDeciding`) and briefcase (`briefcaseDeciding`) tournaments
  stay singles-only.
- Teams never enter singles tournaments as individuals; no mixed singles/team brackets.
- No Freebird-rule handling — if a team member is replaced mid-reign, that's out of scope.
- No trios *title* seeding in v1 unless a trios tournament needs one to link (the model
  supports any `TeamSize` on a TEAM title).
- **No catalog championship additions** — the tag team title is currently custom content
  (user-created via the normal title form, `expansionCode = CUSTOM`); an official version
  is planned for ~April 2027. The feature and its tests only need to *support* TEAM
  titles existing (creation path, tournament linking, payoff flow) — `championships.json`
  and `ChampionshipSync` are not touched. The docs screenshot test creates its TEAM title
  as custom content as part of its setup.
- Faction-based auto-formation of teams (a faction with 3+ members is not silently a trio).
- Individual wrestlers' fan gains are untouched — team fans are a separate counter that
  accrues only at the team level from team segment appearances.

## Constraints

- **Flyway**: new versioned migrations in both `db/migration/h2/` (next after **V163**) and
  `db/migration/mysql/` (next after **V132**). Released scripts are frozen.
- **Data sync**: `teams.json` shape changes ride the existing `TeamSync`
  `DataSyncContributor` (Tier 3 — idempotent, every startup), not a one-time
  `DataMigration`, since backfill of existing teams happens in the SQL migration and the
  sync only reconciles catalog rows.
- **ArchUnit / facades**: wizard UI changes go through the existing facades
  (`ShowFacade`/`ShowContextFacade`/`WrestlerFacade`/`ViewContext`); team service additions
  land behind the appropriate facade, never as raw service params in view constructors.
- **Spotless** + affected unit tests before every commit; docs screenshot test for the new
  wizard flow (rule of thumb: every significant user-facing feature gets one).
- **Trios segment type**: add `TRIOS("trios", "Trios")` to `WellKnownSegmentType` and seed
  a matching `SegmentType` via the segment-type sync, so the payoff segment type resolves
  from the catalog like every other type.
- **Ranking service rework**: `RankingService.getRankedContenders` for TEAM titles currently
  filters by `wrestler1/wrestler2`, removes the champion by member-pair match, sorts by
  member-fan sum (`RankingService.java:192`), and reports that sum in `RankedTeamDTO.fans`
  (`:387`). All of that moves to team-scoped data: filter/sort by `Team.fans` and member
  predicates via the member list; champion removal compares team IDs through the new
  reign→team reference. The member-fan sum survives only as the one-time backfill value.
- **Team origin flag**: `team` gains an `origin` column (`REGISTRY` default for existing
  rows and catalog seeds; `AD_HOC` for wizard-assembled one-offs). One-offs are real,
  persisted teams — they show in team views with their origin; "save to registry" is a
  transition, not a re-create.

## The parts already decided

These were settled with the product owner in conversation:

- **Both entrant sources**: predefined registry *and* one-off assembly, both first-class.
  One-off teams stay one-offs unless saved (checkbox default off).
- **Seeding**: same seed algorithm as individuals, applied at team level (seed position is
  per-entry regardless of entrant type), still overridable in the wizard.
- **Gender**: all members of a team entry must satisfy the tournament's gender restriction.
- **Inaugural champions first**; contender-deciding team tournaments are a later layer.
- **Team size is an enum** (`DUO`, `TRIO`), not a raw int.

## Builder's territory (left to the builder)

- Exact SQL of the migration set: create `team_member`, backfill from
  `wrestler1`/`wrestler2` (ordered by column position), drop the columns, add
  `team_size` to `team`, `team_id` + check constraint to `tournament_entry`, `team_size`
  to `tournament`, `team_size` to `title` (backfill TEAM titles to 2). Split across
  sensible versions; H2 and MySQL dialects as needed.
- Whether `Team` keeps convenience accessors beyond `getPartner()` (e.g.
  `getMembers()` ordered list, `hasMember()`) — keep every current call site compiling
  with unchanged behavior; refactor internals freely.
- **Team fans accrual**: where segment adjudication grants team fans — every team segment
  appearance earns fans (win or lose; winners may earn more — split between the segment
  adjudication path and any fan-event listeners per existing patterns). Amount/rates are
  the builder's to align with existing wrestler fan curves, documented in code.
- **Backfill value**: the migration seeds `Team.fans` as the sum of member fans at
  migration time, preserving current ranking order on upgrade. This same member-sum metric
  is the one-time migration input only — the *live* ordering metric is `Team.fans`.
- **Auto-seed ordering**: eligible teams order by `Team.fans` descending (mirrors the
  individual path's fans sort); the backfill means existing teams start in today's order.
- **Qualifier group size**: entry-level — a group holds N *teams* (a 3-team Free-for-All
  is 6 wrestlers). Validation math (`entrants >= 2 × groupSize`) and wizard copy count
  entries, not wrestlers; javadoc on `Tournament.qualifierGroupSize` updates to match.
- How the booking path materializes a team-vs-team `Segment` from a
  `TournamentMatch`: participants of side 1 = entry team members with `teamNumber` 1,
  side 2 likewise (existing `syncParticipants(Map<Integer, List<Wrestler>>)` overload is
  the natural fit). Where that wiring lives is the builder's call.
- `TournamentFormat.getRoundSegmentTypeCode()` semantics under `teamSize`: rounds book as
  Tag Team / Trios for team tournaments (each format overrides or the booking path
  resolves from tournament team size — builder's choice, documented in code).
- Wizard UX details (tab vs radio for registry/one-off, how member pickers exclude
  wrestlers already entered).
- REST/DTO surface for teams (`TeamResponseDTO` gains members + teamSize).
- Bracket projection (`projectBracket`) display for team entries (entrantName = team
  name; no format changes expected).

## Risks / watch-outs

- `TeamRepository` and NPC segment-team resolution query on `wristler1/wrestler2` columns;
  all such queries must move to the member table. Sweep with grep + ArchUnit tests, and
  run the full team/tournament test suites.
- `TournamentMatch.entrant1/entrant2` columns are `TournamentEntry` FKs — unchanged by this
  design; nothing in the match table moves.
- Catalog back-compat: `teams.json` entries without `teamSize` must keep seeding as DUO.
- Unique `team.name` vs auto-named one-offs: name generation must uniquify (e.g. "A & B",
  "A & B (2)") rather than collide.
- No TEAM championship is seeded — fresh installs need the tester/booker to create the tag
  title as custom content before running a team tournament; the docs test does this in
  setup. When the official title arrives (~April 2027) it replaces the custom one in the
  catalog; nothing in this design blocks that swap.
- `TitleReign` keeps its `Set<Wrestler>` champions *and* gains a nullable team reference
  (`team_id`) for TEAM-title reigns — lineage display still uses member names; the team
  reference is for exact matching (contender filtering, tournament payoff). The migration
  backfills it for existing TEAM reigns by matching the champion set against member sets.
- The inaugural-champions payoff likely needs **no new mechanism**: adjudication's
  title-change path already grants the title to all winning participants, and the NPC
  team-resolution path already marks every winning team member as a winner — verified by
  the builder check. Wiring the tournament payoff through the team entry (members as the
  winning side with `teamNumber`s, reign team reference set) is the remaining work.

