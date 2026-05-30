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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.GameSettingService;
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
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.github.javydreamercsw.management.service.wrestler.RetirementService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

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

  @Mock
  private com.github.javydreamercsw.management.domain.league.LeagueRosterRepository
      leagueRosterRepository;

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

  private SegmentAdjudicationService segmentAdjudicationService;
  @Mock private Universe universe;

  @BeforeEach
  public void setUp() {
    lenient().when(gameSettingService.isWearAndTearEnabled()).thenReturn(true);
    lenient().when(gameSettingService.isRivalryResolutionOnRegularShowsEnabled()).thenReturn(false);
    lenient().when(gameSettingService.getRivalryResolutionThresholdPle()).thenReturn(30);
    lenient().when(gameSettingService.getRivalryResolutionThresholdRegular()).thenReturn(35);
    segmentAdjudicationService =
        new SegmentAdjudicationService(
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
            random);
    org.springframework.test.util.ReflectionTestUtils.setField(
        segmentAdjudicationService, "eventPublisher", eventPublisher);

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

    when(leagueRosterRepository.findByLeagueAndWrestler(league, winner))
        .thenReturn(Optional.of(winnerRoster));
    when(leagueRosterRepository.findByLeagueAndWrestler(league, loser))
        .thenReturn(Optional.of(loserRoster));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(winnerRoster).setWins(any(Integer.class));
    verify(loserRoster).setLosses(any(Integer.class));
    verify(leagueRosterRepository).save(winnerRoster);
    verify(leagueRosterRepository).save(loserRoster);
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
    segmentAdjudicationService.adjudicateMatch(segment);
    verify(rivalryService).addHeatBetweenWrestlers(eq(1L), eq(2L), eq(1), anyString());
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
        .addHeatBetweenWrestlers(eq(10L), eq(11L), anyInt(), anyString());
    verify(rivalryService, never())
        .addHeatBetweenWrestlers(eq(11L), eq(10L), anyInt(), anyString());
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

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(rivalryService).addHeatBetweenWrestlers(eq(10L), eq(11L), anyInt(), anyString());
  }

  @Test
  void testPleResolutionTriggeredForNonStandardMatchType() {
    Rivalry rivalry = mock(Rivalry.class);
    when(rivalry.getId()).thenReturn(99L);
    when(show.isPremiumLiveEvent()).thenReturn(true);
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
    when(rivalryService.attemptResolution(anyLong(), anyInt(), anyInt()))
        .thenReturn(new ResolutionResult<>(true, "resolved", rivalry, 15, 18, 33));
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());

    segmentAdjudicationService.adjudicateMatch(segment);

    // Winner (w1) should have resolution attempted against both opponents
    verify(rivalryService).getRivalryBetweenWrestlers(eq(10L), eq(11L));
    verify(rivalryService).getRivalryBetweenWrestlers(eq(10L), eq(12L));
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
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(rivalryService).addHeat(eq(42L), eq(3), anyString());
  }

  @Test
  void adjudicateMatch_aiTaggedRivalry_attemptsResolutionOnPle() {
    com.github.javydreamercsw.management.domain.rivalry.Rivalry rivalry =
        mock(com.github.javydreamercsw.management.domain.rivalry.Rivalry.class);
    when(segment.getRivalryId()).thenReturn(42L);
    when(show.isPremiumLiveEvent()).thenReturn(true);
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());
    when(rivalryService.attemptResolution(anyLong(), anyInt(), anyInt(), anyInt()))
        .thenReturn(
            new com.github.javydreamercsw.management.service.resolution.ResolutionResult<>(
                false, "not resolved", rivalry, 5, 6, 11));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(rivalryService).attemptResolution(eq(42L), anyInt(), anyInt(), anyInt());
    // Generic pair-scan should NOT run when rivalryId is set
    verify(rivalryService, never()).getRivalryBetweenWrestlers(anyLong(), anyLong());
  }

  @Test
  void adjudicateMatch_noRivalryId_usesGenericPairScanOnPle() {
    when(segment.getRivalryId()).thenReturn(null);
    when(show.isPremiumLiveEvent()).thenReturn(true);
    when(feudService.getActiveFeudsForWrestler(anyLong())).thenReturn(List.of());
    when(rivalryService.getRivalryBetweenWrestlers(anyLong(), anyLong()))
        .thenReturn(Optional.empty());

    segmentAdjudicationService.adjudicateMatch(segment);

    // Generic pair-scan must run for winner vs loser
    verify(rivalryService).getRivalryBetweenWrestlers(eq(1L), eq(2L));
    // Direct resolution must NOT be called with a specific id
    verify(rivalryService, never()).attemptResolution(anyLong(), anyInt(), anyInt(), anyInt());
  }
}
