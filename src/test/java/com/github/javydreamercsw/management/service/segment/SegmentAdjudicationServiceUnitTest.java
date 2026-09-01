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
package com.github.javydreamercsw.management.service.segment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.campaign.WrestlerStatusService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.ringside.RingsideAiService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.title.ContenderSelectionService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.github.javydreamercsw.management.service.wrestler.RetirementService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SegmentAdjudicationServiceUnitTest {

  @Mock private RivalryService rivalryService;
  @Mock private WrestlerService wrestlerService;
  @Mock private FeudResolutionService feudResolutionService;
  @Mock private MultiWrestlerFeudService feudService;

  @Mock private Random random;
  @Mock private TitleService titleService;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;
  @Mock private LeagueRepository leagueRepository;
  @Mock private LeagueRosterRepository leagueRosterRepository;
  @Mock private LegacyService legacyService;
  @Mock private FactionService factionService;
  @Mock private RingsideActionService ringsideActionService;
  @Mock private RingsideAiService ringsideAiService;
  @Mock private RetirementService retirementService;
  @Mock private GameSettingService gameSettingService;
  @Mock private WrestlerRelationshipService relationshipService;
  @Mock private WrestlerStatusService wrestlerStatusService;
  @Mock private LocationService locationService;
  @Mock private ArenaService arenaService;
  @Mock private UniverseContextService universeContextService;

  private SegmentAdjudicationService adjudicationService;

  private Segment matchSegment;
  private Wrestler wrestler1;
  private Wrestler wrestler2;

  @BeforeEach
  public void setUp() {
    lenient().when(gameSettingService.isWearAndTearEnabled()).thenReturn(true);
    adjudicationService =
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

    wrestler1 = Wrestler.builder().build();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler 1");

    wrestler2 = Wrestler.builder().build();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler 2");

    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    promoType.setCode(WellKnownSegmentType.PROMO.getCode());

    Segment promoSegment = new Segment();
    promoSegment.setSegmentType(promoType);
    promoSegment.addParticipant(wrestler1);
    promoSegment.addParticipant(wrestler2);

    SegmentType matchType = new SegmentType();
    matchType.setName("Match");
    matchType.setCode("match");
    matchSegment = new Segment();
    matchSegment.setSegmentType(matchType);
    matchSegment.addParticipant(wrestler1);
    matchSegment.addParticipant(wrestler2);

    Show show = new Show();
    show.setId(5L);
    ShowType showType = new ShowType();
    showType.setName("Weekly Show");
    show.setType(showType);
    promoSegment.setShow(show);
    matchSegment.setShow(show);

    // Default mock behavior
    Mockito.lenient()
        .when(matchFulfillmentRepository.findBySegment(any(Segment.class)))
        .thenReturn(Optional.empty());
    Mockito.lenient().when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    WrestlerState defaultState = WrestlerState.builder().physicalCondition(100).build();
    Mockito.lenient().when(wrestlerService.getOrCreateState(any(), any())).thenReturn(defaultState);
  }

  @Test
  void testAdjudicateMatch_WithNoExistingRivalry_SkipsHeat() {
    // Plain matches do not create new rivalries; with no existing rivalry no heat is added.
    when(rivalryService.getRivalryBetweenWrestlers(wrestler1.getId(), wrestler2.getId()))
        .thenReturn(Optional.empty());

    adjudicationService.adjudicateMatch(matchSegment);

    verify(rivalryService).getRivalryBetweenWrestlers(wrestler1.getId(), wrestler2.getId());
    verify(rivalryService, never())
        .addHeatBetweenWrestlers(anyLong(), anyLong(), anyInt(), anyString(), anyLong());
    verify(rivalryService, never()).addHeat(anyLong(), anyInt(), anyString());
  }

  @Test
  void testAdjudicateMatch_WithExistingRivalry_AddsHeat() {
    // Plain matches add heat to an already-established rivalry without creating new ones.
    Rivalry rivalry = new Rivalry();
    rivalry.setId(99L);
    when(rivalryService.getRivalryBetweenWrestlers(wrestler1.getId(), wrestler2.getId()))
        .thenReturn(Optional.of(rivalry));

    adjudicationService.adjudicateMatch(matchSegment);

    verify(rivalryService, times(1)).addHeat(eq(99L), eq(1), eq("From segment: match"));
    verify(rivalryService, never())
        .addHeatBetweenWrestlers(anyLong(), anyLong(), anyInt(), anyString(), anyLong());
  }

  @Test
  void testAdjudicateMatch_WhenNoUniverseContext_DoesNotThrow() {
    // Simulate no VaadinSession / no ThreadLocal: getCurrentUniverseId returns null.
    // Build a separate service with an unstubbed mock to verify graceful handling.
    UniverseContextService noSessionService = mock(UniverseContextService.class);
    SegmentAdjudicationService service =
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
                noSessionService,
                random));
    // Unstubbed getCurrentUniverseId() returns null (default for Long)

    assertDoesNotThrow(() -> service.adjudicateMatch(matchSegment));
  }

  // ── Contender automation hooks ─────────────────────────────────────────────

  @Test
  void testAdjudicateMatch_ContenderMatch_DesignatesWinnerAsContender() {
    ContenderSelectionService contenderSelectionService = mock(ContenderSelectionService.class);
    adjudicationService.setContenderSelectionService(contenderSelectionService);
    lenient().when(gameSettingService.getContenderMatchFanMultiplier()).thenReturn(1.5);
    lenient().when(gameSettingService.getContenderMatchHeatBonus()).thenReturn(10);

    Title title = new Title();
    title.setName("World Title");
    matchSegment.setContenderMatch(true);
    matchSegment.getTitles().add(title);
    matchSegment.setWinners(List.of(wrestler1));

    adjudicationService.adjudicateMatch(matchSegment);

    verify(contenderSelectionService).designateAsContender(title, wrestler1);
    verify(contenderSelectionService, never()).autoSelectNextContender(any());
  }

  @Test
  void testAdjudicateMatch_TitleSegment_AutoSelectsNextContender() {
    ContenderSelectionService contenderSelectionService = mock(ContenderSelectionService.class);
    adjudicationService.setContenderSelectionService(contenderSelectionService);
    ReflectionTestUtils.setField(
        adjudicationService, "eventPublisher", mock(ApplicationEventPublisher.class));

    Title title = new Title();
    title.setName("World Title");
    matchSegment.setIsTitleSegment(true);
    matchSegment.getTitles().add(title);

    adjudicationService.adjudicateMatch(matchSegment);

    verify(contenderSelectionService).autoSelectNextContender(title);
    verify(contenderSelectionService, never()).designateAsContender(any(), any());
  }

  @Test
  void testAdjudicateMatch_ContenderMatch_AddsHeatBonusToExistingRivalry() {
    ContenderSelectionService contenderSelectionService = mock(ContenderSelectionService.class);
    adjudicationService.setContenderSelectionService(contenderSelectionService);
    lenient().when(gameSettingService.getContenderMatchFanMultiplier()).thenReturn(1.5);
    lenient().when(gameSettingService.getContenderMatchHeatBonus()).thenReturn(10);

    Rivalry rivalry = new Rivalry();
    rivalry.setId(99L);
    when(rivalryService.getRivalryBetweenWrestlers(wrestler1.getId(), wrestler2.getId()))
        .thenReturn(Optional.of(rivalry));

    matchSegment.setContenderMatch(true);

    adjudicationService.adjudicateMatch(matchSegment);

    verify(rivalryService).addHeat(eq(99L), eq(10), eq("Number one contender match"));
  }

  @Test
  void testAdjudicateMatch_ContenderMatch_NullService_DoesNotThrow() {
    // contenderSelectionService is not injected (unit-test construction) — must be a no-op.
    lenient().when(gameSettingService.getContenderMatchFanMultiplier()).thenReturn(1.5);
    lenient().when(gameSettingService.getContenderMatchHeatBonus()).thenReturn(10);
    matchSegment.setContenderMatch(true);
    matchSegment.setWinners(List.of(wrestler1));

    assertDoesNotThrow(() -> adjudicationService.adjudicateMatch(matchSegment));
  }


  // ── Rivalry/feud resolution (ATW-buyh) ─────────────────────────────────────

  @Test
  void testResolution_FeudWithMultipleParticipants_AttemptedOnlyOnce() {
    // A feud with several participants in the segment must get exactly one resolution roll.
    when(gameSettingService.isRivalryResolutionOnRegularShowsEnabled()).thenReturn(true);
    when(gameSettingService.getRivalryResolutionThresholdRegular()).thenReturn(25);

    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setId(7L);
    feud.setName("Shared Feud");
    when(feudService.getActiveFeudsForWrestler(wrestler1.getId())).thenReturn(List.of(feud));
    when(feudService.getActiveFeudsForWrestler(wrestler2.getId())).thenReturn(List.of(feud));

    adjudicationService.adjudicateMatch(matchSegment);

    verify(feudResolutionService, times(1)).attemptFeudResolution(feud);
  }

  @Test
  void testResolution_TaggedRivalry_NotRolledTwiceByPairScan() {
    // The AI-tagged rivalry is also the rivalry between the participants — one roll only.
    when(gameSettingService.isRivalryResolutionOnRegularShowsEnabled()).thenReturn(true);
    when(gameSettingService.getRivalryResolutionThresholdRegular()).thenReturn(25);

    Rivalry rivalry = new Rivalry();
    rivalry.setId(99L);
    when(rivalryService.getRivalryBetweenWrestlers(wrestler1.getId(), wrestler2.getId()))
        .thenReturn(Optional.of(rivalry));
    matchSegment.setRivalryId(99L);

    adjudicationService.adjudicateMatch(matchSegment);

    verify(rivalryService, times(1)).attemptResolution(eq(99L), anyInt(), anyInt(), eq(25));
  }

  @Test
  void testResolution_TaggedRivalry_PairScanStillCoversOtherRivalries() {
    // An AI-tagged rivalry must not suppress resolution attempts for OTHER active
    // rivalries between the participants (previously the pair-scan was skipped entirely).
    when(gameSettingService.isRivalryResolutionOnRegularShowsEnabled()).thenReturn(true);
    when(gameSettingService.getRivalryResolutionThresholdRegular()).thenReturn(25);

    Rivalry pairRivalry = new Rivalry();
    pairRivalry.setId(42L);
    when(rivalryService.getRivalryBetweenWrestlers(wrestler1.getId(), wrestler2.getId()))
        .thenReturn(Optional.of(pairRivalry));
    matchSegment.setRivalryId(50L); // tagged rivalry is a different one

    adjudicationService.adjudicateMatch(matchSegment);

    verify(rivalryService, times(1)).attemptResolution(eq(50L), anyInt(), anyInt(), eq(25));
    verify(rivalryService, times(1)).attemptResolution(eq(42L), anyInt(), anyInt(), eq(25));
  }
}
