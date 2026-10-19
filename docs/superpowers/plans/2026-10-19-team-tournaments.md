# Team-Based Tournaments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Teams (duos/trios) become first-class tournament entrants, with a team registry, team fans/rankings, and inaugural team champions.

**Architecture:** Restructure `Team` from `wrestler1`/`wrestler2` columns to an ordered `team_member` child table carrying `TeamSize`/`TeamOrigin`/`fans`; add a nullable `team_id` to `TournamentEntry` plus `Tournament.teamSize`; keep the three bracket formats untouched (they pair `TournamentEntry` objects); resolve booking segment types from tournament team size and let adjudication's existing title-change path grant the belt to all winning members, with the payoff setting the new `TitleReign.team` reference.

**Tech Stack:** Java 21, Spring Boot 4, JPA/Hibernate, Flyway (H2 + MySQL), Vaadin 25, JUnit 5 + Mockito.

**Spec:** `docs/superpowers/specs/2026-10-18-team-tournaments-design.md`

## Global Constraints

- Flyway: new migrations at **V164** (`db/migration/h2/`) and **V133** (`db/migration/mysql/`) or later; never edit released scripts (`ReleasedMigrationChecksumTest` enforces).
- Run `mvn spotless:apply` before every commit (CI fails otherwise; `spotless:check` runs at compile).
- Never run two `mvn` commands concurrently against the same `target/`.
- E2E runs headless with unit tests skipped: `mvn -Pe2e verify -Dheadless=true -Dsurefire.skip=true -DrerunFailingTestsCount=0`.
- ArchUnit (`ArchitectureTest`): views never receive raw services in constructors — add services to the facade the view already uses.
- Google Java Format via Spotless; Lombok `@Getter/@Setter/@Builder` standard.
- Single-unit tests: `mvn test -Dtest=ClassName`. Verify each test fails before implementing (TDD) and passes after.
- Every commit message ends with `Co-Authored-By: Claude Code <noreply@anthropic.com>`.
- Enum persistence is `@Enumerated(EnumType.STRING)` everywhere.

## Review Focus

1. **teams.json without `teamSize`** must still seed as DUO (old files keep working) — test in Task 2.
2. **Entry with both or neither of wrestler/team set** must be rejected by service validation even though the DB check constraint is the backstop — tests in Task 5.
3. **A wrestler already entered (singly or via a team) entering again in the same tournament** must be rejected — test in Task 5.
4. **Team fans accrue on every team segment appearance, win or lose** (not wins only) — test in Task 6.
5. **One-off team auto-names must uniquify** ("A & B", "A & B (2)") against the unique `team.name` constraint — test in Task 2.

---

## File Structure

| File | Responsibility |
|---|---|
| `db/migration/h2/V164__Team_Members_And_Team_Tournaments.sql` (+ mysql V133) | Schema: `team_member`, team columns, entry `team_id`, tournament/title `team_size`, reign `team_id`, backfills |
| `management/domain/team/TeamMember.java` (new) | Ordered member slot entity |
| `management/domain/team/TeamMemberRepository.java` (new) | Repo for member slots |
| `management/domain/team/TeamSize.java` (new) | `DUO(2)`, `TRIO(3)` |
| `management/domain/team/TeamOrigin.java` (new) | `REGISTRY`, `AD_HOC` |
| `management/domain/team/Team.java` | Members list + new fields + derived duo accessors |
| `management/domain/team/TeamRepository.java` | Queries move from columns to member join |
| `management/service/team/TeamService.java` | List-based create, uniquify, save-to-registry |
| `management/sync/TeamSync.java` + `management/dto/TeamImportDTO.java` + `resources/teams.json` | Catalog: members array + teamSize, back-compat |
| `management/service/ranking/RankingService.java` | TEAM branch: fans sort, member predicates, reign-team champion removal |
| `management/domain/title/TitleReign.java`, `Title.java` | `team` reference; team-aware award overload |
| `management/domain/show/segment/type/WellKnownSegmentType.java` | `TRIOS` |
| `management/domain/tournament/Tournament.java`, `TournamentEntry.java` | `teamSize` on tournament; `team_id` on entry |
| `management/service/tournament/TournamentService.java` | Team entry add/seed/eligibility/validation |
| `management/service/tournament/TournamentTemplateBookingService.java` | Team-vs-team segment sides; payoff reign team |
| `management/service/match/SegmentAdjudicationService.java` | Team fans accrual |
| `management/ui/view/tournament/TournamentListView.java` (+ facade) | Wizard entrant-type, registry picker, one-off assembly |
| `src/test/java/...` matching test classes | Per-task tests listed in each task |

### Task 1: Schema migration + Team member restructure

**Files:**
- Create: `src/main/resources/db/migration/h2/V164__Team_Members_And_Team_Tournaments.sql`
- Create: `src/main/resources/db/migration/mysql/V133__Team_Members_And_Team_Tournaments.sql`
- Create: `src/main/java/com/github/javydreamercsw/management/domain/team/TeamSize.java`, `TeamOrigin.java`, `TeamMember.java`, `TeamMemberRepository.java`
- Modify: `src/main/java/com/github/javydreamercsw/management/domain/team/Team.java`, `TeamRepository.java`
- Modify: every file that sets `wrestler1`/`wrestler2` or queries those columns (sweep in Step 5)
- Test: `src/test/java/com/github/javydreamercsw/management/domain/team/TeamTest.java`

**Interfaces:**
- Produces: `TeamSize { DUO(2), TRIO(3) }` with `int getMemberCount()`; `TeamOrigin { REGISTRY, AD_HOC }`; `TeamMember` (`id`, `team` ManyToOne, `wrestler` ManyToOne, `slot` int, unique `(team_id, slot)` and `(team_id, wrestler_id)`); `Team.getMembers(): List<TeamMember>` (slot-ordered), `Team.getTeamSize(): TeamSize`, `Team.getOrigin(): TeamOrigin`, `Team.getFans(): long`; deprecated derived `getWrestler1()/getWrestler2()` returning member slot 0/1.

- [ ] **Step 1: Write the failing domain test** in `TeamTest`: `teamKeepsDuoBehaviorAfterMemberListMigration` — build a `Team`, call `setMembers(List.of(w1, w2))`, assert `getWrestler1() == w1`, `getWrestler2() == w2`, `getPartner(w1) == w2`, `hasMember(w2)`, `getMemberNames()` equals `"A & B"`, `getTeamSize() == TeamSize.DUO`; and `trioTeamExposesOrderedMembers` — members `[a, b, c]`, slot order preserved, `getTeamSize() == TeamSize.TRIO`.
- [ ] **Step 2: Run** `mvn test -Dtest=TeamTest` — expect FAIL (no `TeamSize`/`setMembers`).
- [ ] **Step 3: Implement** `TeamSize`, `TeamOrigin`, `TeamMember`, `TeamMemberRepository`; rework `Team`: replace the two `wrestler1`/`wrestler2` columns with `@OneToMany(mappedBy="team", cascade=ALL, orphanRemoval=true) @OrderBy("slot ASC") private List<TeamMember> members`; add `team_size`, `origin` (default `REGISTRY` in `@PrePersist`), `fans` (default 0) columns; derive deprecated `getWrestler1/2()` from slots 0/1; `setMembers(List<Wrestler>)` rebuilds slots 0..n-1 and infers `TeamSize` from list size when `teamSize` unset. Remove the setters for wrestler1/2.
- [ ] **Step 4: Write the migration** (both dialects, same operations): create `team_member` (verify the exact PK column names in `Wrestler`/`Team` mappings before writing: team PK is `team_id`; wrestler PK is defined in `Wrestler.java`); backfill `INSERT INTO team_member (team_id, wrestler_id, slot) SELECT team_id, wrestler1_id, 0 FROM team` then slot 1 from `wrestler2_id`; `ALTER TABLE team ADD team_size VARCHAR(16) NOT NULL DEFAULT 'DUO'`, `ADD origin VARCHAR(16) NOT NULL DEFAULT 'REGISTRY'`, `ADD fans BIGINT NOT NULL DEFAULT 0`; backfill `UPDATE team t SET fans = COALESCE((SELECT SUM(ws.fans) FROM wrestler_state ws JOIN team_member tm ON tm.team_id = t.team_id AND tm.wrestler_id = ws.wrestler_id WHERE ws.universe_id = t.universe_id), 0)` (verify `wrestler_state`'s universe column name from `WrestlerState.java`); `ALTER TABLE tournament_entry ADD team_id BIGINT` + FK + `CHECK ((wrestler_id IS NOT NULL AND team_id IS NULL) OR (wrestler_id IS NULL AND team_id IS NOT NULL))`; `ALTER TABLE tournament ADD team_size VARCHAR16 NULL`; `ALTER TABLE title ADD team_size VARCHAR(16) NULL` + `UPDATE title SET team_size='DUO' WHERE championship_type='TEAM'`; `ALTER TABLE title_reign ADD team_id BIGINT` + FK; finally drop `team.wrestler1_id`/`wrestler2_id` columns.
- [ ] **Step 5: Sweep call sites to compile:** rewrite the four `TeamRepository` JPQL queries (`findByWrestler`, `findByWrestlerAndStatus`, `findByBothWrestlers`, `findActiveTeamByBothWrestlers`) as joins over `members` (e.g. `SELECT t FROM Team t JOIN t.members m WHERE m.wrestler = :wrestler ...`), and change the two `findAll` fetch queries to `LEFT JOIN FETCH t.members`; update `TeamService.createTeam` internals to `setMembers`; grep `setWrestler1\|setWrestler2\|getWrestler1()\|getWrestler2()` under `src/main/java` and update *setters* to `setMembers` (getters may remain via the deprecated derived accessors). `mvn compile` must pass.
- [ ] **Step 6: Run** `mvn test -Dtest=TeamTest` (PASS) then `mvn test -Dtest=ReleasedMigrationChecksumTest` (PASS — released files untouched).
- [ ] **Step 7:** `mvn spotless:apply`, commit `feat: restructure Team to ordered member list with team size/origin/fans (ATW-01g2)`.

### Task 2: TeamService list-based API, one-off naming, catalog sync

**Files:**
- Modify: `management/service/team/TeamService.java`, `management/sync/TeamSync.java`, `management/dto/TeamImportDTO.java`, `src/main/resources/teams.json`, `management/dto/TeamResponseDTO.java`
- Test: `src/test/java/com/github/javydreamercsw/management/service/team/TeamServiceTest.java`

**Interfaces:**
- Consumes: `Team.setMembers`, `TeamSize`, `TeamOrigin` (Task 1).
- Produces: `TeamService.createTeam(String name, String description, List<Long> memberIds, TeamSize teamSize, Long factionId, Long managerId, TeamOrigin origin): Optional<Team>`; old 6-arg `createTeam(...)` delegates with `List.of(id1, id2)`, `TeamSize.DUO`, `REGISTRY`; `TeamService.saveToRegistry(Team team): Team` (sets `AD_HOC`→`REGISTRY`); `TeamService.generateUniqueName(List<Wrestler> members): String` ("A & B", then "A & B (2)", "A & B (3)", … via `existsByName`); `TeamImportDTO` gains `List<String> members` and `String teamSize` (old `wrestler1`/`wrestler2` fields retained).

- [ ] **Step 1: Failing tests** in `TeamServiceTest`: `createsTrioTeamFromMemberList` (3 ids → members size 3, `teamSize == TRIO`); `rejectsTeamWhenMemberCountDoesNotMatchTeamSize`; `uniquifiesGeneratedNameWhenTaken` (existing "A & B" → generates "A & B (2)"); `saveToRegistryFlipsOrigin`. Mock `TeamRepository`/`WrestlerRepository` as the existing tests do.
- [ ] **Step 2: Run** `mvn test -Dtest=TeamServiceTest` — FAIL.
- [ ] **Step 3: Implement** the signatures above; validation mirrors the existing `createTeam` checks (null/missing wrestlers, duplicate member, name taken, existing active team with same member set — the same-set check now compares member sets of any size).
- [ ] **Step 4: Catalog sync:** rework `teams.json` entries to `{"name", "description", "members": ["...", "..."], "teamSize": "DUO"}` (keep `manager` where present); `TeamImportDTO` gains `members`/`teamSize`; `TeamSync.sync()` resolves `members` when present, else falls back to `wrestler1`/`wrestler2`, and defaults `teamSize` to `DUO` when absent; passes `REGISTRY` origin.
- [ ] **Step 5:** `TeamResponseDTO` gains `List<String> memberNames` + `TeamSize teamSize`; update the mapping site(s) `grep -rn "TeamResponseDTO" src/main/java` finds.
- [ ] **Step 6: Run** `mvn test -Dtest=TeamServiceTest` PASS; add a catalog test: `src/test/java/com/github/javydreamercsw/management/sync/TeamSyncTest.java` method `catalogWithoutTeamSizeSeedsDuo` (Jackson parse a JSON string without `teamSize` → DTO defaults DUO) — run `mvn test -Dtest=TeamSyncTest` PASS.
- [ ] **Step 7:** `mvn spotless:apply`, commit `feat: team registry API with trios support and catalog members array (ATW-01g2)`.

### Task 3: Rankings rework + reign team reference

**Files:**
- Modify: `management/domain/title/TitleReign.java`, `management/domain/title/Title.java`, `management/service/ranking/RankingService.java`
- Test: `src/test/java/com/github/javydreamercsw/management/service/ranking/RankingServiceTest.java`

**Interfaces:**
- Consumes: `Team.getFans()`, `Team.getMembers()`, member predicates (Tasks 1–2).
- Produces: `TitleReign.getTeam(): @Nullable Team` (+ `setTeam`); `Title.awardTitleTo(Team team, List<Wrestler> newChampions, Instant awardDate, @Nullable Segment wonAtSegment)` — sets `reign.setTeam(team)` then delegates to the existing member-based overload; `RankedTeamDTO.fans` now reports `Team.getFans()`.

- [ ] **Step 1: Failing tests** in `RankingServiceTest`: `teamRankingsSortByTeamFansNotMemberSum` (team X fans 500/members 100+100, team Y fans 300/members 1000+1000 → X ranks above Y); `teamContendersFilteredByAllMemberGender` (one member wrong gender → excluded); `championTeamRemovedByIdViaReignReference` (reign with `team` set → that exact team absent, a *different* team with the same members present); `legacyReignWithoutTeamFallsBackToMemberSetMatch` (reign `team == null`, champions == member set → removed).
- [ ] **Step 2: Run** `mvn test -Dtest=RankingServiceTest` — FAIL.
- [ ] **Step 3: Implement:** `TitleReign` gains `@ManyToOne(fetch=LAZY) @JoinColumn(name="team_id") @Nullable private Team team;`; `Title` gains the team overload; rewrite the TEAM branch of `getRankedContenders` (currently lines 152–196): filter `team.isActive() && team.getMembers().stream().allMatch(w -> Boolean.TRUE.equals(w.getActive()) && w.getGender() == gender)`; champion removal by `reign.getTeam() != null ? team.getId().equals(reign.getTeam().getId()) : sameMemberSet(team, reignChampions)`; sort `Comparator.comparingLong(Team::getFans).reversed()`; `toRankedTeamDTO` reports `team.getFans()` and member names from the member list.
- [ ] **Step 4: Run** `mvn test -Dtest=RankingServiceTest` PASS.
- [ ] **Step 5:** `mvn spotless:apply`, commit `feat: team rankings sort by team fans with exact champion matching (ATW-01g2)`.

### Task 4: Trios segment type

**Files:**
- Modify: `management/domain/show/segment/type/WellKnownSegmentType.java`
- Modify: segment-type seeding the same way `TAG_TEAM` is seeded (grep `TAG_TEAM` under `src/main/java` to find the `SegmentTypeSync`/seeding site)
- Test: existing segment-type catalog test class (grep `WellKnownSegmentType` under `src/test/java`)

**Interfaces:**
- Produces: `WellKnownSegmentType.TRIOS("trios", "Trios")`; a seeded `SegmentType` row with code `trios` resolved by `SegmentTypeService` like every other type.

- [ ] **Step 1: Failing test:** assert `WellKnownSegmentType.TRIOS.getCode()` equals `"trios"` and the catalog parse covers it (extend the existing enum/catalog test).
- [ ] **Step 2: Implement** the enum constant; ensure the segment-type seeding path that guarantees `TAG_TEAM` exists also guarantees `trios` (same idempotent pattern).
- [ ] **Step 3: Run** the test — PASS; `mvn test -Dtest=ReleasedMigrationChecksumTest` still PASS.
- [ ] **Step 4:** `mvn spotless:apply`, commit `feat: add TRIOS segment type (ATW-01g2)`.

### Task 5: Team entrants in tournaments

**Files:**
- Modify: `management/domain/tournament/Tournament.java`, `TournamentEntry.java`, `management/service/tournament/TournamentService.java`
- Test: `src/test/java/com/github/javydreamercsw/management/service/tournament/TournamentServiceTest.java`

**Interfaces:**
- Consumes: `Team` entity (Task 1), `TeamSize` (Task 1), migration `team_id`/check constraint (Task 1).
- Produces: `Tournament.getTeamSize(): @Nullable TeamSize` (+setter); `TournamentEntry.getTeam(): @Nullable Team` (+setter), `TournamentEntry.getWrestler()` now nullable; `TournamentService.addEntry(Tournament tournament, Team team, int seed): TournamentEntry`; `TournamentService.seedAuto` auto-detects team tournaments (`tournament.getTeamSize() != null`) and seeds from eligible REGISTRY teams sorted by `Team.getFans()` desc; `countEligibleEntrants(Title, Gender)` counts teams for team tournaments; `createTournament` overload accepting `TeamSize teamSize`.

- [ ] **Step 1: Failing tests** in `TournamentServiceTest`: `addsTeamEntryToTeamTournament` (entry persisted with team + seed); `rejectsTeamEntryInSinglesTournament` (`teamSize == null` → `IllegalArgumentException`); `rejectsTeamWhenSizeMismatchesTournament` (TRIO team into DUO tournament); `rejectsTeamWhenAnyMemberFailsGender`; `rejectsWrestlerAlreadyEnteredViaTeam` (wrestler in existing team entry then added again, either shape); `seedsTeamTournamentByTeamFansDescending`; `rejectsContenderDecidingTeamTournament` (create with `teamSize != null` + `contenderDeciding` → rejected; same for `briefcaseDeciding`); `teamSizeMustMatchLinkedTeamTitle` (create with teamSize 3 + TEAM title teamSize 2 → rejected).
- [ ] **Step 2: Run** `mvn test -Dtest=TournamentServiceTest` — FAIL.
- [ ] **Step 3: Implement:** entity fields; `addEntry(Tournament, Team, int seed)` validates: tournament `teamSize != null`, `team.getTeamSize() == tournament.getTeamSize()`, every member satisfies the tournament gender, no member of the team appears in any existing entry (`entry.getTeam() != null ? member overlap : entry.getWrestler() == member`); seedAuto/countEligibleEntrants branch on `getTeamSize()` (teams: `origin == REGISTRY`, active, member predicates — reuse the Task 3 filter shape, ordered by fans desc); createTournament overload validates title/teamSize consistency and the contender/briefcase exclusion; update `Tournament.qualifierGroupSize` javadoc to "entries (teams or wrestlers) per qualifier group" — validation math `entrants >= 2 * groupSize` unchanged (it already counts entries).
- [ ] **Step 4: Add central name resolution and sweep call sites (NPE guard):** add to `TournamentEntry`: `@Nullable String entrantName()` returning `team != null ? team.getName() : (wrestler != null ? wrestler.getName() : null)` and `@Nullable Long entrantId()` likewise. Sweep every direct `entry.getWrestler().getName()`/`.getWrestler().getId()` on tournament entries to these helpers — known sites: `SingleEliminationFormat.java:237,258,267`, `QualifierGroupsFormat.java:192,208,224,239,244`, `RoundRobinFormat` (grep `getWrestler()` there), `TournamentService.java:173,192-195,1042-1044,1283` (the 1283 site is briefcase grant — team tournaments can't be briefcase, so leave it but add a guard comment), plus any hits from `grep -rn "getWrestler()\.getName\(\)\|getWrestler()\.getId()" src/main/java/com/github/javydreamercsw/management/service/tournament/ src/main/java/com/github/javydreamercsw/management/ui/view/tournament/`. Every call site that lazily initializes names (e.g. `TournamentService.java:173`) must touch `entry.getTeam().getName()` for team entries too.
- [ ] **Step 5: Run** `mvn test -Dtest=TournamentServiceTest` PASS.
- [ ] **Step 6:** `mvn spotless:apply`, commit `feat: team entrants in tournaments with size/gender/uniqueness validation (ATW-01g2)`.

### Task 6: Booking, payoff, and team fans

**Files:**
- Modify: `management/service/tournament/TournamentTemplateBookingService.java`, `management/service/match/SegmentAdjudicationService.java`
- Test: `src/test/java/com/github/javydreamercsw/management/service/tournament/TournamentTemplateBookingServiceTest.java`, `src/test/java/com/github/javydreamercsw/management/service/match/SegmentAdjudicationServiceTest.java`

**Interfaces:**
- Consumes: `TournamentEntry.getTeam()`, `Tournament.getTeamSize()` (Task 5); `Title.awardTitleTo(Team, ...)` (Task 3); `SegmentTeam` (`service/segment`) and `Segment.syncParticipants(Map<Integer, List<Wrestler>>)` (existing).
- Produces: team tournament matches book as segments with sides = team members (`teamNumber` 1/2) and segment type resolved from `tournament.getTeamSize()` (`DUO`→`tag_team`, `TRIO`→`trios`); team fans accrue per appearance; tournament payoff sets `reign.setTeam(winningTeam)`.

- [ ] **Step 1: Failing booking test** in `TournamentTemplateBookingServiceTest`: `teamTournamentBooksSegmentWithTeamSides` — a DUO-team tournament match materializes a `Segment` whose participants carry `teamNumber` 1/2 with both members per side, and the resolved segment type code is `tag_team`; `trioTournamentBooksTriosSegmentType` → `trios`.
- [ ] **Step 2: Run** `mvn test -Dtest=TournamentTemplateBookingServiceTest` — FAIL.
- [ ] **Step 3: Implement** in the booking path: when the matched entries are team entries, build sides from `entry.getTeam().getMembers()` (slot order) via `syncParticipants(Map<Integer, List<Wrestler>>)`; resolve the segment type from `tournament.getTeamSize()` through `SegmentTypeService` (single place — the booking path, not each format; document with a javadoc on the resolution method).
- [ ] **Step 4: Failing adjudication test** in `SegmentAdjudicationServiceTest`: `teamSegmentAwardsFansToBothTeamsOnAppearance` — a segment with two team sides: both teams gain fans when it adjudicates; winners' teams gain strictly more than losers' teams (dice seeded/averaged or asserted with bounds); `singlesSegmentDoesNotAwardTeamFans`.
- [ ] **Step 5: Run** — FAIL.
- [ ] **Step 6: Implement** in `SegmentAdjudicationService` after the existing winner/loser fan block (around line 1075): detect team sides from participants' `teamNumber` (any side with ≥2 participants carrying the same teamNumber); for each team, one fans adjustment — winners use the winner curve shape, losers the loser curve shape (reuse the dice/quality/difficulty variables already in scope; exact multipliers chosen to sit under the individual awards — document the rate in a comment); persist via `TeamRepository`.
- [ ] **Step 7: Failing payoff test** in `TournamentTemplateBookingServiceTest`: `teamTournamentPayoffSetsReignTeamReference` — after the payoff of a team tournament linked to a TEAM title completes with a team entry winning, the title's current reign has `getTeam() == winningTeam` and `champions` contains exactly the team members.
- [ ] **Step 8: Run** — FAIL, then implement: adjudication's existing title-change path already grants to all winning members (verified in design); the payoff completion (where the tournament final's winner is recorded) resolves the winning entry's team and sets `team` on the reign adjudication just created (`title.getCurrentReign()`); if no reign exists (edge), fall back to calling `awardTitleTo(team, members, now, segment)`.
- [ ] **Step 9: Run** both test classes PASS.
- [ ] **Step 10:** `mvn spotless:apply`, commit `feat: book team tournament matches with team sides, team fans accrual, payoff reign team (ATW-01g2)`.

### Task 7: Wizard UI + docs screenshot test

**Files:**
- Modify: `management/ui/view/tournament/TournamentListView.java` (creation wizard section), the facade it consumes (grep its constructor for the facade; add `TeamService` there)
- Create: `src/test/java/com/github/javydreamercsw/management/ui/view/tournament/TournamentTeamWizardDocsE2ETest.java`
- Test: unit-level wizard test if a separate dialog class is extracted (builder's choice; ArchUnit must pass either way)

**Interfaces:**
- Consumes: `TeamService.createTeam(...)` with origin, `saveToRegistry`, `TournamentService.createTournament(... teamSize)`, `addEntry(Tournament, Team, seed)`, `seedAuto` (Tasks 2/5).
- Produces: wizard entrant-type choice (Singles / Tag Team / Trios); team mode shows a registry grid (`origin == REGISTRY`, active, matching `TeamSize`, every member matching tournament gender, current universe) and a one-off assembly panel (member multi-select excluding wrestlers already entered, auto-named via `generateUniqueName`, "Save to team registry" checkbox default off); docs screenshots captured.

- [ ] **Step 1: Extend the wizard** in `TournamentListView` per the Interfaces block; entrant-type radio visible in the create dialog; on team types, replace the wrestler picker with registry grid + one-off panel; wire entries through `TournamentService`.
- [ ] **Step 2: Run ArchUnit:** `mvn test -Dtest=ArchitectureTest` PASS (facade rule holds).
- [ ] **Step 3: Write the docs test** extending `AbstractDocsE2ETest`: setup creates a TEAM title (custom content, via the title service directly), seeds via existing data initializer, navigates the wizard: choose Tag Team, add a registry team, capture `documentFeature("Game Mechanics", "Team Tournaments", "...", "team-tournament-wizard")`; also capture the one-off assembly panel.
- [ ] **Step 4: Run** `mvn -Pe2e verify -Dtest=TournamentTeamWizardDocsE2ETest -Dheadless=true -Dsurefire.skip=true -DrerunFailingTestsCount=0` PASS.
- [ ] **Step 5:** `mvn spotless:apply`, commit `feat: tournament wizard team entrants with registry and one-off assembly (ATW-01g2)`.

### Task 8: Full verification

- [ ] **Step 1:** `mvn test` — full unit suite PASS.
- [ ] **Step 2:** `mvn -Pe2e verify -Dheadless=true -Dsurefire.skip=true -DrerunFailingTestsCount=0` — E2E suite PASS.
- [ ] **Step 3:** `graphify update .` (keep the knowledge graph current).
- [ ] **Step 4:** `git pull --rebase; git push` — verify `git status` shows "up to date with origin".
- [ ] **Step 5:** Close ATW-01g2 (`bd close ATW-01g2`) and note any follow-ups (contender-deciding team tournaments, official tag title catalog swap ~April 2027) as new beads issues.
