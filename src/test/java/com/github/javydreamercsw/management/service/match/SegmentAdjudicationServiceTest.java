/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpSource;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.event.ChampionshipChangeEvent;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.achievement.ScriptedAchievementEvaluator;
import com.github.javydreamercsw.management.service.campaign.WrestlerStatusService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.resolution.ResolutionResult;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.ringside.RingsideAiService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.github.javydreamercsw.management.service.wrestler.RetirementService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentAdjudicationServiceTest {

  @Mock private RivalryService rivalryService;
  @Mock private WrestlerService wrestlerService;
  @Mock private FeudResolutionService feudResolutionService;
  @Mock private MultiWrestlerFeudService feudService;
  @Mock private Random random;
  @Mock private Segment segment;
  @Mock private Wrestler winner;
  @Mock private Wrestler loser;
  @Mock private WrestlerState winnerState;
  @Mock private WrestlerState loserState;
  @Mock private SegmentType segmentType;
  @Mock private Show show;
  @Mock private TitleService titleService;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;
  @Mock private LeagueRepository leagueRepository;

  @Mock private LeagueRosterRepository leagueRosterRepository;

  @Mock private LegacyService legacyService;
  @Mock private FactionService factionService;
  @Mock private RingsideActionService ringsideActionService;
  @Mock private RingsideAiService ringsideAiService;
  @Mock private WrestlerRelationshipService relationshipService;
  @Mock private RetirementService retirementService;
  @Mock private GameSettingService gameSettingService;
  @Mock private LocationService locationService;
  @Mock private ArenaService arenaService;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private WrestlerStatusService wrestlerStatusService;
  @Mock private UniverseContextService universeContextService;
  @Mock private TitleOpportunityService titleOpportunityService;
  @Mock private TournamentMatchRepository tournamentMatchRepository;

  private SegmentAdjudicationService segmentAdjudicationService;
  @Mock private Universe universe;

  @BeforeEach
  public void setUp() {
    lenient().when(gameSettingService.isWearAndTearEnabled()).thenReturn(true);
    lenient().when(gameSettingService.isRivalryResolutionOnRegularShowsEnabled()).thenReturn(false);
    lenient().when(gameSettingService.getRivalryResolutionThresholdPle()).thenReturn(30);
    lenient().when(gameSettingService.getRivalryResolutionThresholdRegular()).thenReturn(35);
    lenient().when(gameSettingService.getCurrentGameDate()).thenReturn(LocalDate.now());
    segmentAdjudicationService =
        new SegmentAdjudicationService(
            new SegmentAdjudicationService.Dependencies(
                rivalryService,
                wrestlerService,
                feudResolutionService,
                feudService,
                titleService,
                matchFulfillmentRepository,
                leagueRepository,
                leagueRosterRepository,
                legacyService,
                factionService,
                ringsideActionService,
                ringsideAiService,
                retirementService,
                gameSettingService,
                relationshipService,
                wrestlerStatusService,
                universeContextService,
                random));
    ReflectionTestUtils.setField(segmentAdjudicationService, "eventPublisher", eventPublisher);
    ReflectionTestUtils.setField(
        segmentAdjudicationService, "titleOpportunityService", titleOpportunityService);
    ReflectionTestUtils.setField(
        segmentAdjudicationService, "tournamentMatchRepository", tournamentMatchRepository);
    ReflectionTestUtils.setField(segmentAdjudicationService, "teamRepository", teamRepository);

    when(universe.getId()).thenReturn(1L);
    when(show.getUniverse()).thenReturn(universe);

    when(segment.getWinners()).thenReturn(List.of(winner));
    when(segment.getLosers()).thenReturn(List.of(loser));
    when(segment.getWrestlers()).thenReturn(List.of(winner, loser));
    when(winner.getId()).thenReturn(1L);
    when(loser.getId()).thenReturn(2L);

    when(wrestlerService.getOrCreateState(eq(1L), anyLong())).thenReturn(winnerState);
    when(wrestlerService.getOrCreateState(eq(2L), anyLong())).thenReturn(loserState);
    when(winner.getState(anyLong())).thenReturn(Optional.of(winnerState));
    when(loser.getState(anyLong())).thenReturn(Optional.of(loserState));

    when(segment.getSegmentType()).thenReturn(segmentType);
    when(segmentType.getName()).thenReturn("Test Match");
    when(segment.getShow()).thenReturn(show);
    when(show.isPremiumLiveEvent()).thenReturn(false);
    when(matchFulfillmentRepository.findBySegment(segment)).thenReturn(Optional.empty());
    // Default: no AI-tagged rivalry; tests that need it override this.
    lenient().when(segment.getRivalryId()).thenReturn(null);
  }

  @Test
  void testAffinityGainOnVictory() {
    Faction faction = mock(Faction.class);
    when(faction.getId()).thenReturn(100L);

    Wrestler w1 = mock(Wrestler.class);
    Wrestler w2 = mock(Wrestler.class);
    WrestlerState s1 = mock(WrestlerState.class);
    WrestlerState s2 = mock(WrestlerState.class);

    when(w1.getId()).thenReturn(10L);
    when(w2.getId()).thenReturn(11L);
    when(wrestlerService.getOrCreateState(eq(10L), anyLong())).thenReturn(s1);
    when(wrestlerService.getOrCreateState(eq(11L), anyLong())).thenReturn(s2);
    when(w1.getState(anyLong())).thenReturn(Optional.of(s1));
    when(w2.getState(anyLong())).thenReturn(Optional.of(s2));

    when(s1.getFaction()).thenReturn(faction);
    when(s2.getFaction()).thenReturn(faction);

    when(segment.getWrestlers()).thenReturn(List.of(w1, w2, loser));
    when(segment.getWinners()).thenReturn(List.of(w1, w2));
    when(segment.isMainEvent()).thenReturn(false);

    segmentAdjudicationService.adjudicateMatch(segment);

    // Calculation: (2-1) [participation] + 2 [victory bonus] = 3
    verify(factionService).addAffinity(100L, 3);
  }

  @Test
  void testAdjudicateLeagueMatch() {
    League league = mock(League.class);
    when(leagueRepository.findByUniverse(universe)).thenReturn(Optional.of(league));

    LeagueRoster winnerRoster = mock(LeagueRoster.class);
    LeagueRoster loserRoster = mock(LeagueRoster.class);
    when(winnerRoster.getWrestler()).thenReturn(winner);
    when(loserRoster.getWrestler()).thenReturn(loser);
    when(leagueRosterRepository.findByLeague(league))
        .thenReturn(List.of(winnerRoster, loserRoster));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(winnerRoster).setWins(any(Integer.class));
    verify(loserRoster).setLosses(any(Integer.class));
    verify(leagueRosterRepository).saveAll(any());
  }

  @Test
  void testAdjudicateChampionshipChange() {
    Title title = mock(Title.class);
    when(segment.getIsTitleSegment()).thenReturn(true);
    when(segment.getTitles()).thenReturn(Set.of(title));
    Wrestler oldChamp = mock(Wrestler.class);
    when(title.getCurrentChampions()).thenReturn(List.of(oldChamp));

    // Winner is NOT the old champ
    when(segment.getWinners()).thenReturn(List.of(winner));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleService).awardTitleTo(eq(title), eq(List.of(winner)), eq(segment));
  }

  @Test
  void testWearAndTear() {
    when(winnerState.getPhysicalCondition()).thenReturn(100);
    when(loserState.getPhysicalCondition()).thenReturn(100);
    when(random.nextInt(3)).thenReturn(1); // base loss 1 + 1 = 2%

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(winnerState).setPhysicalCondition(any(Integer.class));
    verify(loserState).setPhysicalCondition(any(Integer.class));
    verify(retirementService).checkRetirement(eq(winner), anyLong());
    verify(retirementService).checkRetirement(eq(loser), anyLong());
  }

  @Test
  void testRivalryHeat() {
    // Plain matches look up an existing rivalry and add heat; they do NOT auto-create new ones.
    Rivalry rivalry = mock(Rivalry.class);
    when(rivalry.getId()).thenReturn(99L);
    when(rivalryService.getRivalryBetweenWrestlers(eq(1L), eq(2L)))
        .thenReturn(Optional.of(rivalry));
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(rivalryService).getRivalryBetweenWrestlers(eq(1L), eq(2L));
    verify(rivalryService).addHeat(eq(99L), eq(1), anyString());
    verify(rivalryService, never())
        .addHeatBetweenWrestlers(eq(1L), eq(2L), anyInt(), anyString(), anyLong());
  }

  @Test
  void testTeammatesDoNotBuildRivalryHeat() {
    Faction faction = mock(Faction.class);
    when(faction.getId()).thenReturn(50L);

    Wrestler w1 = mock(Wrestler.class);
    Wrestler w2 = mock(Wrestler.class);
    WrestlerState s1 = mock(WrestlerState.class);
    WrestlerState s2 = mock(WrestlerState.class);

    when(w1.getId()).thenReturn(10L);
    when(w2.getId()).thenReturn(11L);
    when(wrestlerService.getOrCreateState(eq(10L), anyLong())).thenReturn(s1);
    when(wrestlerService.getOrCreateState(eq(11L), anyLong())).thenReturn(s2);
    when(w1.getState(anyLong())).thenReturn(Optional.of(s1));
    when(w2.getState(anyLong())).thenReturn(Optional.of(s2));
    // Both wrestlers share the same faction
    when(s1.getFaction()).thenReturn(faction);
    when(s2.getFaction()).thenReturn(faction);

    when(segment.getWrestlers()).thenReturn(List.of(w1, w2));
    when(segment.getWinners()).thenReturn(List.of(w1));

    segmentAdjudicationService.adjudicateMatch(segment);

    // Heat must NOT be added between teammates
    verify(rivalryService, never())
        .addHeatBetweenWrestlers(eq(10L), eq(11L), anyInt(), anyString(), anyLong());
    verify(rivalryService, never())
        .addHeatBetweenWrestlers(eq(11L), eq(10L), anyInt(), anyString(), anyLong());
  }

  @Test
  void testOpponentsFromDifferentFactionsDoGetHeat() {
    Faction factionA = mock(Faction.class);
    Faction factionB = mock(Faction.class);
    when(factionA.getId()).thenReturn(1L);
    when(factionB.getId()).thenReturn(2L);

    Wrestler w1 = mock(Wrestler.class);
    Wrestler w2 = mock(Wrestler.class);
    WrestlerState s1 = mock(WrestlerState.class);
    WrestlerState s2 = mock(WrestlerState.class);

    when(w1.getId()).thenReturn(10L);
    when(w2.getId()).thenReturn(11L);
    when(wrestlerService.getOrCreateState(eq(10L), anyLong())).thenReturn(s1);
    when(wrestlerService.getOrCreateState(eq(11L), anyLong())).thenReturn(s2);
    when(w1.getState(anyLong())).thenReturn(Optional.of(s1));
    when(w2.getState(anyLong())).thenReturn(Optional.of(s2));
    when(s1.getFaction()).thenReturn(factionA);
    when(s2.getFaction()).thenReturn(factionB);

    when(segment.getWrestlers()).thenReturn(List.of(w1, w2));
    when(segment.getWinners()).thenReturn(List.of(w1));

    Rivalry rivalry = mock(Rivalry.class);
    when(rivalry.getId()).thenReturn(99L);
    when(rivalryService.getRivalryBetweenWrestlers(eq(10L), eq(11L)))
        .thenReturn(Optional.of(rivalry));
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());

    segmentAdjudicationService.adjudicateMatch(segment);

    // Plain match: heat is added to the existing rivalry, not via auto-create.
    verify(rivalryService).getRivalryBetweenWrestlers(eq(10L), eq(11L));
    verify(rivalryService).addHeat(eq(99L), anyInt(), anyString());
  }

  @Test
  void testPleResolutionTriggeredForNonStandardMatchType() {
    Rivalry rivalry = mock(Rivalry.class);
    when(rivalry.getId()).thenReturn(99L);
    when(show.isPremiumLiveEvent()).thenReturn(true);
    when(show.getId()).thenReturn(1L);
    // Triple Threat — not in the explicit switch cases
    when(segmentType.getName()).thenReturn("Triple Threat");

    Wrestler w1 = mock(Wrestler.class);
    Wrestler w2 = mock(Wrestler.class);
    Wrestler w3 = mock(Wrestler.class);
    when(w1.getId()).thenReturn(10L);
    when(w2.getId()).thenReturn(11L);
    when(w3.getId()).thenReturn(12L);

    WrestlerState s1 = mock(WrestlerState.class);
    WrestlerState s2 = mock(WrestlerState.class);
    WrestlerState s3 = mock(WrestlerState.class);
    when(wrestlerService.getOrCreateState(eq(10L), anyLong())).thenReturn(s1);
    when(wrestlerService.getOrCreateState(eq(11L), anyLong())).thenReturn(s2);
    when(wrestlerService.getOrCreateState(eq(12L), anyLong())).thenReturn(s3);
    when(w1.getState(anyLong())).thenReturn(Optional.of(s1));
    when(w2.getState(anyLong())).thenReturn(Optional.of(s2));
    when(w3.getState(anyLong())).thenReturn(Optional.of(s3));

    when(segment.getWrestlers()).thenReturn(List.of(w1, w2, w3));
    when(segment.getWinners()).thenReturn(List.of(w1));

    when(rivalryService.getRivalryBetweenWrestlers(anyLong(), anyLong()))
        .thenReturn(Optional.of(rivalry));
    // Plain match heat path calls addHeat on the found rivalry
    lenient()
        .when(rivalryService.addHeat(anyLong(), anyInt(), anyString()))
        .thenReturn(Optional.of(rivalry));
    when(rivalryService.attemptResolution(anyLong(), anyInt(), anyInt()))
        .thenReturn(new ResolutionResult<>(true, "resolved", rivalry, 15, 18, 33));
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());

    segmentAdjudicationService.adjudicateMatch(segment);

    // getRivalryBetweenWrestlers is called for both the heat path AND the resolution path for
    // each pair — verify at least once per cross-pair from the PLE winner scan.
    verify(rivalryService, atLeastOnce()).getRivalryBetweenWrestlers(eq(10L), eq(11L));
    verify(rivalryService, atLeastOnce()).getRivalryBetweenWrestlers(eq(10L), eq(12L));
  }

  @Test
  void testPromoRewards() {
    when(segmentType.getName()).thenReturn("Promo");
    when(random.nextInt(any(Integer.class))).thenReturn(5);

    segmentAdjudicationService.processRewards(segment, 1.0);

    // Should call awardFans for participants
    verify(wrestlerService).awardFans(eq(1L), anyLong(), anyLong());
    verify(wrestlerService).awardFans(eq(2L), anyLong(), anyLong());
  }

  // --- Team fans (ATW-01g2) ---

  @Mock private com.github.javydreamercsw.management.domain.team.TeamRepository teamRepository;

  /** A real team with the given members (slot order) and fan count. */
  private com.github.javydreamercsw.management.domain.team.Team teamWithFans(
      Long id, String name, long fans, List<Wrestler> members) {
    com.github.javydreamercsw.management.domain.team.Team team =
        new com.github.javydreamercsw.management.domain.team.Team();
    team.setId(id);
    team.setName(name);
    team.setMembers(new ArrayList<>(members));
    team.setFans(fans);
    return team;
  }

  /**
   * A 2-vs-2 segment with the four given wrestlers: side 1 (winners) vs side 2 (losers). Segment
   * type is a real {@code tag_team} row so the PROMO checks and match-quality paths behave like
   * production. Both teams resolve through {@code teamRepository} member-set matching.
   */
  private Segment teamSegmentFixture(
      com.github.javydreamercsw.management.domain.team.Team winnersTeam,
      com.github.javydreamercsw.management.domain.team.Team losersTeam,
      Wrestler a,
      Wrestler b,
      Wrestler c,
      Wrestler d) {
    SegmentType tagTeamType = new SegmentType();
    tagTeamType.setName("Tag Team");
    tagTeamType.setCode("tag_team");
    Segment real = new Segment();
    real.setShow(show);
    real.setSegmentType(tagTeamType);
    real.addParticipant(a, 1);
    real.addParticipant(b, 1);
    real.addParticipant(c, 2);
    real.addParticipant(d, 2);
    real.setWinners(List.of(a, b));
    return real;
  }

  @Test
  void teamSegmentAwardsFansToBothTeamsOnAppearance() {
    // A 2-vs-2 team segment: BOTH teams gain fans (appearance-based, win or lose) and the
    // winners' team gains strictly more than the losers' team. Dice are seeded so the winners'
    // base (2d6+3) always beats the losers' (1d6-4) — the assertion checks the rates, not luck.
    Wrestler a = mock(Wrestler.class);
    Wrestler b = mock(Wrestler.class);
    Wrestler c = mock(Wrestler.class);
    Wrestler d = mock(Wrestler.class);
    when(a.getId()).thenReturn(10L);
    when(b.getId()).thenReturn(11L);
    when(c.getId()).thenReturn(12L);
    when(d.getId()).thenReturn(13L);
    when(a.getName()).thenReturn("Ace");
    when(b.getName()).thenReturn("Bea");
    when(c.getName()).thenReturn("Cy");
    when(d.getName()).thenReturn("Di");
    when(a.getState(anyLong())).thenReturn(Optional.empty());
    when(b.getState(anyLong())).thenReturn(Optional.empty());
    when(c.getState(anyLong())).thenReturn(Optional.empty());
    when(d.getState(anyLong())).thenReturn(Optional.empty());
    when(wrestlerService.getOrCreateState(anyLong(), anyLong()))
        .thenAnswer(inv -> mock(WrestlerState.class));
    when(wrestlerService.awardFans(anyLong(), anyLong(), anyLong()))
        .thenReturn(Optional.of(mock(WrestlerState.class)));

    var winnersTeam = teamWithFans(100L, "High Flyers", 5_000L, List.of(a, b));
    var losersTeam = teamWithFans(200L, "Face Team", 5_000L, List.of(c, d));
    when(teamRepository.findByWrestler(a)).thenReturn(List.of(winnersTeam));
    when(teamRepository.findByWrestler(b)).thenReturn(List.of(winnersTeam));
    when(teamRepository.findByWrestler(c)).thenReturn(List.of(losersTeam));
    when(teamRepository.findByWrestler(d)).thenReturn(List.of(losersTeam));

    Segment real = teamSegmentFixture(winnersTeam, losersTeam, a, b, c, d);
    when(random.nextInt(any(Integer.class))).thenReturn(3); // low but positive dice everywhere

    segmentAdjudicationService.adjudicateMatch(real);

    // Both teams were persisted exactly once (one adjustment per appearance).
    ArgumentCaptor<com.github.javydreamercsw.management.domain.team.Team> teamCaptor =
        ArgumentCaptor.forClass(com.github.javydreamercsw.management.domain.team.Team.class);
    verify(teamRepository, times(2)).save(teamCaptor.capture());
    var savedTeams = teamCaptor.getAllValues();
    var winnersSaved =
        savedTeams.stream()
            .filter(t -> t.getId().equals(100L))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Winners' team must be saved"));
    var losersSaved =
        savedTeams.stream()
            .filter(t -> t.getId().equals(200L))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Losers' team must be saved"));
    // Both teams gained (the flat appearance grant keeps every appearance positive — spec: teams
    // accrue fans from every team segment appearance), winners strictly more than losers.
    assertThat(winnersSaved.getFans()).isGreaterThan(5_000L);
    assertThat(losersSaved.getFans()).isGreaterThan(5_000L);
    assertThat(winnersSaved.getFans()).isGreaterThan(losersSaved.getFans());
    // Individuals still earn their own fans through the unchanged wrestler path.
    verify(wrestlerService, atLeastOnce()).awardFans(anyLong(), anyLong(), anyLong());
  }

  @Test
  void singlesSegmentDoesNotAwardTeamFans() {
    // A plain 1-vs-1 segment (no ≥2-participant side) never touches the team fan counter.
    segmentAdjudicationService.adjudicateMatch(segment);

    verify(teamRepository, never()).save(any());
  }

  @Test
  void teamPayoffTitleChange_setsReignTeamReference() {
    // Payoff of a team tournament linked to a TEAM title: adjudication's title change grants the
    // belt through the real Title.awardTitleTo (a NEW reign is created), then the winning
    // entry's team is stamped on that new reign. The champions snapshot is taken BEFORE the
    // grant, so a real change (no pre-change reign) carries the winner's team.
    when(segment.getId()).thenReturn(900L);
    when(segment.getIsTitleSegment()).thenReturn(true);
    Title realTitle = new Title();
    realTitle.setId(7L);
    realTitle.setName("Tag Titles"); // vacant — no pre-change reign
    when(segment.getTitles()).thenReturn(Set.of(realTitle));
    // The grant flows through the real domain method: titleService.awardTitleTo delegates to
    // Title.awardTitleTo, creating the reign adjudication is about to stamp.
    doAnswer(
            inv -> {
              Title t = inv.getArgument(0);
              List<Wrestler> champs = inv.getArgument(1);
              t.awardTitleTo(champs, Instant.now(), inv.getArgument(2));
              return null;
            })
        .when(titleService)
        .awardTitleTo(any(Title.class), anyList(), any(Segment.class));

    var winningTeam = teamWithFans(100L, "High Flyers", 0L, List.of(winner));
    var losingTeam = teamWithFans(200L, "Face Team", 0L, List.of(loser));
    TournamentEntry winnerEntry = TournamentEntry.builder().team(winningTeam).seed(1).build();
    TournamentEntry loserEntry = TournamentEntry.builder().team(losingTeam).seed(2).build();
    TournamentMatch match =
        TournamentMatch.builder().entrant1(winnerEntry).entrant2(loserEntry).build();
    TournamentRound round = new TournamentRound();
    round.setTournament(teamTournament(realTitle));
    match.setRound(round);
    when(tournamentMatchRepository.findBySegmentId(900L)).thenReturn(Optional.of(match));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleService).awardTitleTo(eq(realTitle), eq(List.of(winner)), eq(segment));
    // The reign the grant just created carries the winning team reference.
    assertThat(realTitle.getCurrentReign()).isPresent();
    assertThat(realTitle.getCurrentReign().get().getTeam()).isEqualTo(winningTeam);
    assertThat(realTitle.getCurrentReign().get().getChampions()).containsExactly(winner);
  }

  @Test
  void teamPayoffChampionsDefend_reignTeamReferenceStays() {
    // Champion showcase defense with the REAL sequence: a pre-change reign exists and one of
    // its champions is among the winners — the snapshot (taken before the grant) marks a
    // defense and the reigning team reference is never re-stamped with the challenger's team.
    when(segment.getId()).thenReturn(902L);
    when(segment.getIsTitleSegment()).thenReturn(true);
    Title realTitle = new Title();
    realTitle.setId(7L);
    realTitle.setName("Tag Titles");
    var reigningTeam = teamWithFans(300L, "Reigning Team", 0L, List.of(loser));
    realTitle.awardTitleTo(List.of(loser), Instant.now(), null);
    realTitle.getCurrentReign().get().setTeam(reigningTeam);
    when(segment.getTitles()).thenReturn(Set.of(realTitle));
    doAnswer(
            inv -> {
              Title t = inv.getArgument(0);
              List<Wrestler> champs = inv.getArgument(1);
              t.awardTitleTo(champs, Instant.now(), inv.getArgument(2));
              return null;
            })
        .when(titleService)
        .awardTitleTo(any(Title.class), anyList(), any(Segment.class));

    // Champions (loser) defended against the tournament winner (winner) — note the segment
    // winners list still reads [winner] from setUp, so flip it: the champion won here.
    when(segment.getWinners()).thenReturn(List.of(loser));
    when(segment.getIsTitleSegment()).thenReturn(true);

    var winningTeam = teamWithFans(100L, "High Flyers", 0L, List.of(winner));
    TournamentEntry winnerEntry = TournamentEntry.builder().team(winningTeam).seed(1).build();
    TournamentEntry loserEntry =
        TournamentEntry.builder()
            .team(teamWithFans(200L, "Face Team", 0L, List.of(loser)))
            .seed(2)
            .build();
    TournamentMatch match =
        TournamentMatch.builder().entrant1(winnerEntry).entrant2(loserEntry).build();
    TournamentRound round = new TournamentRound();
    round.setTournament(teamTournament(realTitle));
    match.setRound(round);
    when(tournamentMatchRepository.findBySegmentId(902L)).thenReturn(Optional.of(match));

    segmentAdjudicationService.adjudicateMatch(segment);

    // Defense: the reign still belongs to the reigning team — no team change, no re-stamp.
    assertThat(realTitle.getCurrentReign()).isPresent();
    assertThat(realTitle.getCurrentReign().get().getTeam()).isEqualTo(reigningTeam);
    assertThat(realTitle.getCurrentReign().get().getChampions()).containsExactly(loser);
  }

  @Test
  void teamPayoffTitleChange_noReign_fallsBackToTeamAward() {
    // Edge fallback: when no current reign exists after the grant, the team award path creates
    // one carrying the team reference and the full member set as champions.
    when(segment.getId()).thenReturn(901L);
    when(segment.getIsTitleSegment()).thenReturn(true);
    Title realTitle = new Title();
    realTitle.setId(7L);
    realTitle.setName("Tag Titles");
    when(segment.getTitles()).thenReturn(Set.of(realTitle));
    // The titleService grant does NOTHING here (simulating a path that never created a reign),
    // exercising the service's own fallback award.

    // Segment winners are [winner] (setUp); the winning entry's team matches exactly that set.
    var winningTeam = teamWithFans(100L, "High Flyers", 0L, List.of(winner));
    TournamentEntry winnerEntry = TournamentEntry.builder().team(winningTeam).seed(1).build();
    TournamentEntry loserEntry =
        TournamentEntry.builder()
            .team(teamWithFans(200L, "Face Team", 0L, List.of(loser)))
            .seed(2)
            .build();
    TournamentMatch match =
        TournamentMatch.builder().entrant1(winnerEntry).entrant2(loserEntry).build();
    TournamentRound round = new TournamentRound();
    round.setTournament(teamTournament(realTitle));
    match.setRound(round);
    when(tournamentMatchRepository.findBySegmentId(901L)).thenReturn(Optional.of(match));

    segmentAdjudicationService.adjudicateMatch(segment);

    // Fallback awarded the title to the whole team with the team reference on the new reign.
    assertThat(realTitle.getCurrentReign()).isPresent();
    assertThat(realTitle.getCurrentReign().get().getTeam()).isEqualTo(winningTeam);
    assertThat(realTitle.getCurrentReign().get().getChampions())
        .containsExactlyInAnyOrderElementsOf(
            winningTeam.getMembers().stream()
                .map(com.github.javydreamercsw.management.domain.team.TeamMember::getWrestler)
                .toList());
  }

  @Test
  void midBracketTeamMatch_doesNotStampOrAwardTitle() {
    // A mid-bracket team match on a tournament with a linked title is NOT a title segment —
    // the booking path only attaches the championship to the payoff final/showcase — so no
    // reign is stamped, no fallback award fires, even though the bracket already has a WINNER
    // entry (booking records bracket results before any show airs).
    when(segment.getId()).thenReturn(903L);
    when(segment.getIsTitleSegment()).thenReturn(false);
    Title realTitle = new Title();
    realTitle.setId(7L);
    realTitle.setName("Tag Titles");
    realTitle.awardTitleTo(List.of(loser), Instant.now(), null);
    when(segment.getTitles()).thenReturn(Set.of()); // nothing attached — the mid-bracket shape

    var tournamentWinnerTeam = teamWithFans(400L, "Bracket Winners", 0L, List.of(winner));
    TournamentEntry winnerEntry =
        TournamentEntry.builder()
            .team(tournamentWinnerTeam)
            .seed(1)
            .status(TournamentEntryStatus.WINNER) // stamped by markWinner at booking time
            .build();
    TournamentEntry loserEntry =
        TournamentEntry.builder()
            .team(teamWithFans(200L, "Face Team", 0L, List.of(loser)))
            .seed(2)
            .status(TournamentEntryStatus.ELIMINATED)
            .build();
    TournamentMatch match =
        TournamentMatch.builder().entrant1(winnerEntry).entrant2(loserEntry).build();
    TournamentRound round = new TournamentRound();
    round.setTournament(teamTournament(realTitle));
    match.setRound(round);
    when(tournamentMatchRepository.findBySegmentId(903L)).thenReturn(Optional.of(match));

    segmentAdjudicationService.adjudicateMatch(segment);

    // Nothing championship-related happened: the reigning reign is untouched, no award fired.
    assertThat(realTitle.getCurrentReign()).isPresent();
    assertThat(realTitle.getCurrentReign().get().getChampions()).containsExactly(loser);
    assertThat(realTitle.getCurrentReign().get().getTeam()).isNull();
  }

  /** A DUO team tournament linked to the given title. */
  private Tournament teamTournament(Title linkedTitle) {
    Tournament tournament = mock(Tournament.class);
    when(tournament.getTeamSize())
        .thenReturn(com.github.javydreamercsw.management.domain.team.TeamSize.DUO);
    when(tournament.getLinkedTitle()).thenReturn(linkedTitle);
    return tournament;
  }

  @Test
  void adjudicateMatch_aiTaggedRivalry_addsTargetedHeatOnRegularShow() {
    when(segment.getRivalryId()).thenReturn(42L);
    when(show.isPremiumLiveEvent()).thenReturn(false);
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(rivalryService).addHeat(eq(42L), eq(2), anyString());
  }

  @Test
  void adjudicateMatch_aiTaggedRivalry_addsHigherHeatOnPle() {
    when(segment.getRivalryId()).thenReturn(42L);
    when(show.isPremiumLiveEvent()).thenReturn(true);
    when(show.getId()).thenReturn(1L);
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(rivalryService).addHeat(eq(42L), eq(3), anyString());
  }

  @Test
  void adjudicateMatch_aiTaggedRivalry_attemptsResolutionOnPle() {
    Rivalry rivalry = mock(Rivalry.class);
    when(segment.getRivalryId()).thenReturn(42L);
    when(show.isPremiumLiveEvent()).thenReturn(true);
    when(show.getId()).thenReturn(1L);
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());
    when(rivalryService.resolveAtPle(anyLong(), anyInt(), anyInt(), anyLong()))
        .thenReturn(new ResolutionResult<>(true, "resolved", rivalry, 0, 0, 0));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(rivalryService).resolveAtPle(eq(42L), anyInt(), anyInt(), anyLong());
    verify(rivalryService, never()).attemptResolution(anyLong(), anyInt(), anyInt(), anyInt());
    // The generic pair-scan still runs so OTHER rivalries between participants get their
    // qualifying PLE attempt; the tagged rivalry itself is not rolled a second time (the
    // pair lookup returned no rivalry here, so only the tagged attempt happened).
    verify(rivalryService, times(1)).resolveAtPle(anyLong(), anyInt(), anyInt(), anyLong());
  }

  @Test
  void adjudicateMatch_noRivalryId_usesGenericPairScanOnPle() {
    when(segment.getRivalryId()).thenReturn(null);
    when(show.isPremiumLiveEvent()).thenReturn(true);
    when(show.getId()).thenReturn(1L);
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());
    when(rivalryService.getRivalryBetweenWrestlers(anyLong(), anyLong()))
        .thenReturn(Optional.empty());

    segmentAdjudicationService.adjudicateMatch(segment);

    // Generic pair-scan must run for winner vs loser (called from both heat path and resolution
    // path)
    verify(rivalryService, atLeastOnce()).getRivalryBetweenWrestlers(eq(1L), eq(2L));
    // Direct resolution must NOT be called with a specific id
    verify(rivalryService, never()).attemptResolution(anyLong(), anyInt(), anyInt(), anyInt());
  }

  // --- Briefcase grant at adjudication (ATW-8p72) ---

  private void stubTournamentSegment(Tournament tournament, Long segmentId) {
    when(segment.getId()).thenReturn(segmentId);
    when(tournament.isBriefcaseDeciding()).thenReturn(true);
    when(tournament.getStatus()).thenReturn(TournamentStatus.COMPLETE);
    when(tournament.getName()).thenReturn("Time Vault");
    TournamentRound round = new TournamentRound();
    round.setTournament(tournament);
    TournamentMatch match = new TournamentMatch();
    match.setRound(round);
    when(tournamentMatchRepository.findBySegmentId(segmentId)).thenReturn(Optional.of(match));
  }

  @Test
  void adjudicateMatch_briefcaseFinalComplete_grantsToBracketWinner() {
    Tournament tournament = mock(Tournament.class);
    Wrestler bracketWinner = mock(Wrestler.class);
    when(bracketWinner.getName()).thenReturn("Mukundi Shumba");
    TournamentEntry winnerEntry = new TournamentEntry();
    winnerEntry.setWrestler(bracketWinner);
    winnerEntry.setStatus(TournamentEntryStatus.WINNER);
    when(tournament.getEntries()).thenReturn(List.of(winnerEntry));
    stubTournamentSegment(tournament, 777L);

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleOpportunityService).grantFromTournament(tournament, bracketWinner);
  }

  @Test
  void adjudicateMatch_segmentWinnerDiffersFromBracket_grantStillUsesBracket() {
    // The bracket's WINNER entry is authoritative even if the adjudicated segment disagrees
    // (a double-check guard, not a hard block).
    Tournament tournament = mock(Tournament.class);
    Wrestler bracketWinner = mock(Wrestler.class);
    when(bracketWinner.getName()).thenReturn("Bracket Winner");
    TournamentEntry winnerEntry = new TournamentEntry();
    winnerEntry.setWrestler(bracketWinner);
    winnerEntry.setStatus(TournamentEntryStatus.WINNER);
    when(tournament.getEntries()).thenReturn(List.of(winnerEntry));
    stubTournamentSegment(tournament, 778L);
    // segment.getWinners() stubbed to `winner` (≠ bracketWinner) in setUp.

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleOpportunityService).grantFromTournament(tournament, bracketWinner);
  }

  @Test
  void adjudicateMatch_segmentNotInTournament_noGrant() {
    when(segment.getId()).thenReturn(779L);
    when(tournamentMatchRepository.findBySegmentId(779L)).thenReturn(Optional.empty());

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleOpportunityService, never()).grantFromTournament(any(), any());
  }

  @Test
  void adjudicateMatch_tournamentNotBriefcaseDeciding_noGrant() {
    Tournament tournament = mock(Tournament.class);
    when(tournament.isBriefcaseDeciding()).thenReturn(false);
    stubTournamentSegment(tournament, 780L);

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleOpportunityService, never()).grantFromTournament(any(), any());
  }

  @Test
  void adjudicateMatch_briefcaseTournamentStillScheduled_noGrant() {
    Tournament tournament = mock(Tournament.class);
    when(tournament.isBriefcaseDeciding()).thenReturn(true);
    when(tournament.getStatus()).thenReturn(TournamentStatus.SCHEDULED);
    when(tournament.getName()).thenReturn("Time Vault II");
    stubTournamentSegment(tournament, 781L);

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleOpportunityService, never()).grantFromTournament(any(), any());
  }

  @Test
  void adjudicateMatch_briefcaseTournamentWithoutWinnerEntry_noGrantAndWarns() {
    Tournament tournament = mock(Tournament.class);
    when(tournament.getEntries()).thenReturn(List.of());
    stubTournamentSegment(tournament, 782L);

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleOpportunityService, never()).grantFromTournament(any(), any());
  }

  @Test
  void adjudicateMatch_grantFailure_neverFailsAdjudication() {
    // Failure isolation: a grant problem must never bubble into the adjudication flow.
    Tournament tournament = mock(Tournament.class);
    Wrestler bracketWinner = mock(Wrestler.class);
    when(bracketWinner.getName()).thenReturn("Mukundi Shumba");
    TournamentEntry winnerEntry = new TournamentEntry();
    winnerEntry.setWrestler(bracketWinner);
    winnerEntry.setStatus(TournamentEntryStatus.WINNER);
    when(tournament.getEntries()).thenReturn(List.of(winnerEntry));
    stubTournamentSegment(tournament, 783L);
    when(titleOpportunityService.grantFromTournament(any(), any()))
        .thenThrow(new IllegalStateException("one-HELD guard tripped"));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleOpportunityService).grantFromTournament(tournament, bracketWinner);
  }

  // --- Health-based bump tests ---

  private Wrestler buildPlayerWrestler(long id, WrestlerState state, int lowHealth) {
    Wrestler w = mock(Wrestler.class);
    Account account = mock(Account.class);
    when(w.getId()).thenReturn(id);
    when(w.getAccount()).thenReturn(account);
    when(w.getLowHealth()).thenReturn(lowHealth);
    when(w.getStartingHealth()).thenReturn(15);
    when(wrestlerService.getOrCreateState(eq(id), anyLong())).thenReturn(state);
    when(w.getState(anyLong())).thenReturn(Optional.of(state));
    lenient().when(state.getPhysicalCondition()).thenReturn(100);
    return w;
  }

  private SegmentParticipant buildParticipant(Wrestler w, int finalHealth) {
    SegmentParticipant p = mock(SegmentParticipant.class);
    when(p.getWrestler()).thenReturn(w);
    when(p.getFinalHealth()).thenReturn(finalHealth);
    return p;
  }

  @Test
  void testTwoHealthBumpsAtOneHp() {
    WrestlerState playerState = mock(WrestlerState.class);
    lenient().when(playerState.getPhysicalCondition()).thenReturn(100);
    Wrestler player = buildPlayerWrestler(10L, playerState, 4);

    SegmentParticipant participant = buildParticipant(player, 1);
    when(segment.getParticipants()).thenReturn(Set.of(participant));
    when(segment.getWrestlers()).thenReturn(List.of(player));
    when(segment.getWinners()).thenReturn(List.of(player));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(wrestlerService, times(2)).addBump(eq(10L), anyLong(), eq(BumpSource.WEAR_AND_TEAR));
  }

  @Test
  void testOneBumpAtLowHealthThreshold() {
    WrestlerState playerState = mock(WrestlerState.class);
    lenient().when(playerState.getPhysicalCondition()).thenReturn(100);
    Wrestler player = buildPlayerWrestler(10L, playerState, 4);

    SegmentParticipant participant = buildParticipant(player, 4); // exactly at lowHealth
    when(segment.getParticipants()).thenReturn(Set.of(participant));
    when(segment.getWrestlers()).thenReturn(List.of(player));
    when(segment.getWinners()).thenReturn(List.of(player));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(wrestlerService, times(1)).addBump(eq(10L), anyLong(), eq(BumpSource.WEAR_AND_TEAR));
  }

  @Test
  void testNoBumpAboveLowHealthThreshold() {
    WrestlerState playerState = mock(WrestlerState.class);
    lenient().when(playerState.getPhysicalCondition()).thenReturn(100);
    Wrestler player = buildPlayerWrestler(10L, playerState, 4);

    SegmentParticipant participant = buildParticipant(player, 5); // above lowHealth
    when(segment.getParticipants()).thenReturn(Set.of(participant));
    when(segment.getWrestlers()).thenReturn(List.of(player));
    when(segment.getWinners()).thenReturn(List.of(player));
    // Seed random so probabilistic check never fires (newCondition ~100%, bumpChance = 0)
    lenient().when(random.nextInt(100)).thenReturn(99);

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(wrestlerService, never()).addBump(eq(10L), anyLong(), eq(BumpSource.WEAR_AND_TEAR));
  }

  @Test
  void testRuleBumpNotSuppressedWhenHealthAboveThreshold() {
    WrestlerState playerState = mock(WrestlerState.class);
    lenient().when(playerState.getPhysicalCondition()).thenReturn(100);
    Wrestler player = buildPlayerWrestler(10L, playerState, 4);

    SegmentParticipant participant = buildParticipant(player, 8); // well above lowHealth
    when(segment.getParticipants()).thenReturn(Set.of(participant));
    when(segment.getWrestlers()).thenReturn(List.of(player));
    when(segment.getWinners()).thenReturn(List.of(player));

    SegmentRule rule = mock(SegmentRule.class);
    when(rule.getBumpAddition()).thenReturn(BumpAddition.ALL);
    when(rule.getName()).thenReturn("TestRule");
    when(segment.getSegmentRules()).thenReturn(Set.of(rule));
    // Ensure probabilistic check never fires
    lenient().when(random.nextInt(100)).thenReturn(99);

    segmentAdjudicationService.adjudicateMatch(segment);

    // Rule bump must fire because no health-based bump was added
    verify(wrestlerService, atLeastOnce()).addBump(eq(10L), anyLong(), eq(BumpSource.RULE));
  }

  @Test
  void testRuleBumpSuppressedWhenHealthBumpFired() {
    WrestlerState playerState = mock(WrestlerState.class);
    lenient().when(playerState.getPhysicalCondition()).thenReturn(100);
    Wrestler player = buildPlayerWrestler(10L, playerState, 4);

    SegmentParticipant participant = buildParticipant(player, 1); // triggers 2 health bumps
    when(segment.getParticipants()).thenReturn(Set.of(participant));
    when(segment.getWrestlers()).thenReturn(List.of(player));
    when(segment.getWinners()).thenReturn(List.of(player));

    SegmentRule rule = mock(SegmentRule.class);
    when(rule.getBumpAddition()).thenReturn(BumpAddition.ALL);
    when(rule.getName()).thenReturn("TestRule");
    when(segment.getSegmentRules()).thenReturn(Set.of(rule));

    segmentAdjudicationService.adjudicateMatch(segment);

    // Only WEAR_AND_TEAR bumps — no RULE bump
    verify(wrestlerService, never()).addBump(eq(10L), anyLong(), eq(BumpSource.RULE));
    verify(wrestlerService, times(2)).addBump(eq(10L), anyLong(), eq(BumpSource.WEAR_AND_TEAR));
  }

  @Test
  void applyTitleChange_tagTeamPartialPinfall_countsAsDefense() {
    // Regression: when only one of two tag team champions is listed as winner (the partner who got
    // the pin), the old containsAll check incorrectly triggered a title change. The fix uses
    // anyMatch so that one champion winning is enough to count as a successful defense.
    Wrestler champion1 = mock(Wrestler.class);
    Wrestler champion2 = mock(Wrestler.class);
    when(champion1.getId()).thenReturn(10L);
    when(champion2.getId()).thenReturn(11L);
    when(champion1.getState(anyLong())).thenReturn(Optional.empty());
    when(champion2.getState(anyLong())).thenReturn(Optional.empty());
    lenient()
        .when(wrestlerService.getOrCreateState(eq(10L), anyLong()))
        .thenReturn(mock(WrestlerState.class));
    lenient()
        .when(wrestlerService.getOrCreateState(eq(11L), anyLong()))
        .thenReturn(mock(WrestlerState.class));

    Wrestler challenger = mock(Wrestler.class);
    when(challenger.getId()).thenReturn(12L);
    when(challenger.getState(anyLong())).thenReturn(Optional.empty());
    lenient()
        .when(wrestlerService.getOrCreateState(eq(12L), anyLong()))
        .thenReturn(mock(WrestlerState.class));

    Title title = mock(Title.class);
    when(title.getCurrentChampions()).thenReturn(List.of(champion1, champion2));

    when(segment.getIsTitleSegment()).thenReturn(Boolean.TRUE);
    when(segment.getTitles()).thenReturn(Set.of(title));
    when(segment.getWrestlers()).thenReturn(List.of(champion1, champion2, challenger));
    when(segment.getWinners()).thenReturn(List.of(champion1)); // only the pin-taker listed
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleService, never()).awardTitleTo(any(Title.class), any(), any());
    verify(eventPublisher).publishEvent(any(ChampionshipDefendedEvent.class));
  }

  @Test
  void applyTitleChange_challengersWinTagTeamMatch_triggersNewReign() {
    Wrestler champion1 = mock(Wrestler.class);
    Wrestler champion2 = mock(Wrestler.class);
    when(champion1.getId()).thenReturn(10L);
    when(champion2.getId()).thenReturn(11L);
    when(champion1.getState(anyLong())).thenReturn(Optional.empty());
    when(champion2.getState(anyLong())).thenReturn(Optional.empty());
    lenient()
        .when(wrestlerService.getOrCreateState(eq(10L), anyLong()))
        .thenReturn(mock(WrestlerState.class));
    lenient()
        .when(wrestlerService.getOrCreateState(eq(11L), anyLong()))
        .thenReturn(mock(WrestlerState.class));

    Wrestler challenger1 = mock(Wrestler.class);
    Wrestler challenger2 = mock(Wrestler.class);
    when(challenger1.getId()).thenReturn(12L);
    when(challenger2.getId()).thenReturn(13L);
    when(challenger1.getState(anyLong())).thenReturn(Optional.empty());
    when(challenger2.getState(anyLong())).thenReturn(Optional.empty());
    lenient()
        .when(wrestlerService.getOrCreateState(eq(12L), anyLong()))
        .thenReturn(mock(WrestlerState.class));
    lenient()
        .when(wrestlerService.getOrCreateState(eq(13L), anyLong()))
        .thenReturn(mock(WrestlerState.class));

    Title title = mock(Title.class);
    when(title.getCurrentChampions()).thenReturn(List.of(champion1, champion2));

    when(segment.getIsTitleSegment()).thenReturn(Boolean.TRUE);
    when(segment.getTitles()).thenReturn(Set.of(title));
    when(segment.getWrestlers())
        .thenReturn(List.of(champion1, champion2, challenger1, challenger2));
    when(segment.getWinners()).thenReturn(List.of(challenger1, challenger2));
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleService)
        .awardTitleTo(eq(title), eq(List.of(challenger1, challenger2)), eq(segment));
    verify(eventPublisher).publishEvent(any(ChampionshipChangeEvent.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void triggerAchievements_enrichesScriptedContextWithSegmentVariables() {
    ScriptedAchievementEvaluator evaluator = mock(ScriptedAchievementEvaluator.class);
    when(evaluator.resolveNewlyUnlockedKeys(any(), any())).thenReturn(List.of());
    ReflectionTestUtils.setField(
        segmentAdjudicationService, "scriptedAchievementEvaluator", evaluator);

    Account account = new Account();
    account.setId(99L);
    account.setAchievements(Set.of());
    when(winner.getAccount()).thenReturn(account);

    SegmentParticipant winnerParticipant = mock(SegmentParticipant.class);
    when(winnerParticipant.getWrestler()).thenReturn(winner);
    when(winnerParticipant.getFinalMomentum()).thenReturn(8);
    when(winnerParticipant.getWinningCardName()).thenReturn("Frog Splash");
    when(segment.getParticipants()).thenReturn(Set.of(winnerParticipant));
    when(segment.getSegmentRating()).thenReturn(95);
    when(segment.getIsTitleSegment()).thenReturn(false);

    segmentAdjudicationService.adjudicateMatch(segment);

    ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
    verify(evaluator).resolveNewlyUnlockedKeys(eq(account), contextCaptor.capture());
    Map<String, Object> ctx = contextCaptor.getValue();
    assertThat(ctx).containsEntry("segmentRating", 95);
    assertThat(ctx).containsEntry("winningCardName", "Frog Splash");
    assertThat(ctx).containsEntry("winnerMomentum", 8);
    assertThat(ctx).containsEntry("participantMomentum", 8);
    assertThat(ctx).containsEntry("isChampionshipMatch", false);
  }

  @Test
  void applyTitleChange_onSuccessfulDefense_recordsDefense() {
    Title title = mock(Title.class);
    Wrestler champion = mock(Wrestler.class);
    when(champion.getId()).thenReturn(5L);
    when(title.getCurrentChampions()).thenReturn(List.of(champion));
    when(wrestlerService.getOrCreateState(eq(5L), anyLong())).thenReturn(mock(WrestlerState.class));
    when(champion.getState(anyLong())).thenReturn(Optional.empty());

    when(segment.getIsTitleSegment()).thenReturn(true);
    when(segment.getTitles()).thenReturn(Set.of(title));
    when(segment.getWinners()).thenReturn(List.of(champion));
    when(segment.getWrestlers()).thenReturn(List.of(champion, loser));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleService).recordSuccessfulDefense(title);
  }

  @Test
  void applyTitleChange_onDraw_doesNotRecordDefense() {
    Title title = mock(Title.class);
    Wrestler champion = mock(Wrestler.class);
    when(champion.getId()).thenReturn(5L);
    when(title.getCurrentChampions()).thenReturn(List.of(champion));
    when(wrestlerService.getOrCreateState(eq(5L), anyLong())).thenReturn(mock(WrestlerState.class));
    when(champion.getState(anyLong())).thenReturn(Optional.empty());

    when(segment.getIsTitleSegment()).thenReturn(true);
    when(segment.getTitles()).thenReturn(Set.of(title));
    when(segment.getWinners()).thenReturn(List.of());
    when(segment.getWrestlers()).thenReturn(List.of(champion, loser));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleService, never()).recordSuccessfulDefense(any());
  }

  @Test
  void applyTitleChange_onSuccessfulDefense_defeatedChallengersRemoved() {
    // The challenger who lost the title match no longer holds #1 contender status.
    Title title = mock(Title.class);
    Wrestler champion = mock(Wrestler.class);
    when(champion.getId()).thenReturn(5L);
    when(title.getCurrentChampions()).thenReturn(List.of(champion));
    when(wrestlerService.getOrCreateState(eq(5L), anyLong())).thenReturn(mock(WrestlerState.class));
    when(champion.getState(anyLong())).thenReturn(Optional.empty());

    when(segment.getIsTitleSegment()).thenReturn(true);
    when(segment.getTitles()).thenReturn(Set.of(title));
    when(segment.getWinners()).thenReturn(List.of(champion));
    when(segment.getWrestlers()).thenReturn(List.of(champion, loser));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleService).removeChallengers(title, List.of(loser));
  }

  @Test
  void applyTitleChange_onDraw_challengersNotRemoved() {
    // A draw leaves the match undecided: the challenger keeps #1 contender status.
    Title title = mock(Title.class);
    Wrestler champion = mock(Wrestler.class);
    when(champion.getId()).thenReturn(5L);
    when(title.getCurrentChampions()).thenReturn(List.of(champion));
    when(wrestlerService.getOrCreateState(eq(5L), anyLong())).thenReturn(mock(WrestlerState.class));
    when(champion.getState(anyLong())).thenReturn(Optional.empty());

    when(segment.getIsTitleSegment()).thenReturn(true);
    when(segment.getTitles()).thenReturn(Set.of(title));
    when(segment.getWinners()).thenReturn(List.of());
    when(segment.getWrestlers()).thenReturn(List.of(champion, loser));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(titleService, never()).removeChallengers(any(), any());
  }

  // --- Ambush wear at adjudication (ATW-2b9u) ---

  @Test
  void adjudicateMatch_ambushSegment_appliesAmbushWear() {
    when(segment.getId()).thenReturn(888L);
    segmentAdjudicationService.adjudicateMatch(segment);
    verify(titleOpportunityService).applyAmbushWearIfApplicable(segment);
  }

  @Test
  void adjudicateMatch_titleOpportunityServiceNull_skipsAmbushWear() {
    ReflectionTestUtils.setField(segmentAdjudicationService, "titleOpportunityService", null);
    when(segment.getId()).thenReturn(889L);
    // No exception: the hook is null-safe (unit tests).
    segmentAdjudicationService.adjudicateMatch(segment);
  }
}
