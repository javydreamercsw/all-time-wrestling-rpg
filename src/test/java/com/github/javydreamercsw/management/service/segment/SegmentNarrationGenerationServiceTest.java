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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.Commentator;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentResult;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link SegmentNarrationGenerationService}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentNarrationGenerationServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private NpcService npcService;
  @Mock private WrestlerStatsService wrestlerStatsService;
  @Mock private RivalryService rivalryService;
  @Mock private UniverseContextService universeContextService;
  @Mock private SegmentNarrationServiceFactory narrationServiceFactory;
  @Mock private SegmentNarrationService narrationService;

  private SegmentNarrationGenerationService service;
  private Segment segment;
  private Show show;
  private Universe universe;

  @BeforeEach
  void setUp() {
    service =
        new SegmentNarrationGenerationService(
            segmentRepository,
            npcService,
            wrestlerStatsService,
            rivalryService,
            universeContextService,
            narrationServiceFactory);

    universe = new Universe();
    universe.setId(1L);
    show = new Show();
    show.setName("Test Show");

    SegmentType matchType = new SegmentType();
    matchType.setName("One on One");

    segment = new Segment();
    segment.setId(10L);
    segment.setSegmentType(matchType);

    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    w1.setName("Alice");
    Wrestler w2 = new Wrestler();
    w2.setId(2L);
    w2.setName("Bob");
    segment.addParticipant(w1);
    segment.addParticipant(w2);

    lenient().when(segmentRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(segment));
    lenient().when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    lenient().when(narrationServiceFactory.getBestAvailableService()).thenReturn(narrationService);
    lenient().when(narrationService.isAvailable()).thenReturn(true);
    lenient()
        .when(
            narrationService.narrateSegment(
                any(SegmentNarrationService.SegmentNarrationContext.class)))
        .thenReturn("A hard-fought battle unfolded.");
    lenient()
        .when(narrationServiceFactory.summarizeNarration(anyString()))
        .thenReturn("Alice defeats Bob.");
    lenient().when(npcService.findAllByTypeUnfiltered(anyString())).thenReturn(List.of());
    lenient().when(wrestlerStatsService.findAllBySegment(any(), anyLong())).thenReturn(List.of());
    lenient()
        .when(rivalryService.getActiveRivalriesForWrestlerAsDTO(anyLong()))
        .thenReturn(List.of());
  }

  @Test
  void generatesNarrationSetsFlagAndFillsBlankSummary() {
    service.generateForSegment(10L);

    assertEquals("A hard-fought battle unfolded.", segment.getNarration());
    assertTrue(segment.isNarrationGenerated());
    assertEquals("Alice defeats Bob.", segment.getSummary());
    verify(segmentRepository).save(segment);
  }

  @Test
  void showCommentaryTeamTakesPrecedenceOverPool() {
    // Owner follow-up (2.13.0 review): when the show has an assigned commentary team
    // (from its template), the narration context uses that team — not the global pool.
    CommentaryTeam team = new CommentaryTeam();
    team.setName("Raw announce");
    Commentator lead = new Commentator();
    Npc leadNpc = new Npc();
    leadNpc.setName("Michael Cole");
    lead.setNpc(leadNpc);
    lead.setStyle("Play-by-play");
    team.setCommentators(new ArrayList<>(List.of(lead)));
    show.setCommentaryTeam(team);
    segment.setShow(show);

    Npc poolCommentator = new Npc();
    poolCommentator.setName("Some Pool Voice");
    when(npcService.findAllByTypeUnfiltered("Commentator"))
        .thenReturn(new ArrayList<>(List.of(poolCommentator)));

    service.generateForSegment(10L);

    var captor = ArgumentCaptor.forClass(SegmentNarrationService.SegmentNarrationContext.class);
    verify(narrationService).narrateSegment(captor.capture());
    List<String> names =
        captor.getValue().getNpcs().stream()
            .map(SegmentNarrationService.NPCContext::getName)
            .toList();
    assertTrue(names.contains("Michael Cole"), "assigned team commentator must be used");
    assertFalse(names.contains("Some Pool Voice"), "pool must not be used when a team is set");
  }

  @Test
  void noCommentaryTeamFallsBackToPool() {
    // Owner follow-up (2.13.0 review): shows without an assigned team keep the
    // NarrationDialog default behavior — the full Commentator pool as context.
    Npc poolCommentator = new Npc();
    poolCommentator.setName("Some Pool Voice");
    when(npcService.findAllByTypeUnfiltered(anyString()))
        .thenReturn(new ArrayList<>(List.of(poolCommentator)));

    service.generateForSegment(10L);

    var captor = ArgumentCaptor.forClass(SegmentNarrationService.SegmentNarrationContext.class);
    verify(narrationService).narrateSegment(captor.capture());
    List<String> names =
        captor.getValue().getNpcs().stream()
            .map(SegmentNarrationService.NPCContext::getName)
            .toList();
    assertTrue(names.contains("Some Pool Voice"), "pool used when show has no team");
  }

  @Test
  void existingSummaryIsNotOverwritten() {
    segment.setSummary("Booked summary");

    service.generateForSegment(10L);

    assertEquals("Booked summary", segment.getSummary());
    verify(narrationServiceFactory, never()).summarizeNarration(anyString());
  }

  @Test
  void bookedDrawInformsNarrationContext() {
    segment.declareResult(SegmentResult.DRAW);

    service.generateForSegment(10L);

    var captor = ArgumentCaptor.forClass(SegmentNarrationService.SegmentNarrationContext.class);
    verify(narrationService).narrateSegment(captor.capture());
    assertTrue(captor.getValue().getDeterminedOutcome().toLowerCase().contains("draw"));
  }

  @Test
  void bookedNoContestInformsNarrationContext() {
    // 2.13.0 (coverage gap): NO_CONTEST must feed its own outcome text, not the draw one.
    segment.declareResult(SegmentResult.NO_CONTEST);

    service.generateForSegment(10L);

    var captor = ArgumentCaptor.forClass(SegmentNarrationService.SegmentNarrationContext.class);
    verify(narrationService).narrateSegment(captor.capture());
    String outcome = captor.getValue().getDeterminedOutcome().toLowerCase();
    assertTrue(outcome.contains("no-contest"), "outcome must say no-contest, got: " + outcome);
  }

  @Test
  void titleSegmentsIncludeChampionshipContext() {
    // 2.13.0 (coverage gap): a title segment names the championship and flags titleMatch.
    Title title = new Title();
    title.setName("World Heavyweight Championship");
    segment.getTitles().add(title);

    service.generateForSegment(10L);

    var captor = ArgumentCaptor.forClass(SegmentNarrationService.SegmentNarrationContext.class);
    verify(narrationService).narrateSegment(captor.capture());
    var context = captor.getValue();
    assertEquals("World Heavyweight Championship", context.getSegmentChampionship());
    assertTrue(context.isTitleMatch(), "title segment must flag titleMatch");
  }

  @Test
  void enrichSegment_skipsRefereeAssignmentWhenOneIsBooked() {
    // 2.13.0 (coverage gap): an existing referee is never replaced.
    Npc booked = new Npc();
    booked.setId(5L);
    booked.setName("Already Booked");
    segment.setReferee(booked);
    segment.setShow(show);
    Npc referee = new Npc();
    referee.setName("Earl Hebner");
    when(npcService.findAllByType("Referee")).thenReturn(List.of(referee));
    when(npcService.pickUnusedReferee(anyList(), anyList())).thenReturn(referee);

    service.generateForSegment(10L);

    assertSame(booked, segment.getReferee(), "booked referee must not be replaced");
  }

  @Test
  void enrichSegment_refereePoolEmpty_leavesSegmentWithoutReferee() {
    // 2.13.0 (coverage gap): no referees configured — segment proceeds without one.
    segment.setShow(show);
    when(npcService.findAllByType("Referee")).thenReturn(List.of());

    service.generateForSegment(10L);

    assertNull(segment.getReferee());
  }

  @Test
  void buildContext_titleNamesJoinWithAnd() {
    // 2.13.0 (coverage gap): two titles on one segment render "A and B".
    Title t1 = new Title();
    t1.setName("Intercontinental Title");
    Title t2 = new Title();
    t2.setName("World Title");
    segment.getTitles().add(t1);
    segment.getTitles().add(t2);

    service.generateForSegment(10L);

    var captor = ArgumentCaptor.forClass(SegmentNarrationService.SegmentNarrationContext.class);
    verify(narrationService).narrateSegment(captor.capture());
    assertEquals(
        "Intercontinental Title and World Title", captor.getValue().getSegmentChampionship());
  }

  @Test
  void nonPromoSegmentWithoutRefereeGetsAutoAssignedReferee() {
    Npc referee = new Npc();
    referee.setName("Earl Hebner");
    when(npcService.findAllByType("Referee")).thenReturn(List.of(referee));
    when(npcService.pickUnusedReferee(anyList(), anyList())).thenReturn(referee);
    // Segment needs a show for the sibling-referee lookup.
    segment.setShow(show);

    service.generateForSegment(10L);

    assertEquals(referee, segment.getReferee());
  }

  @Test
  void promoSegmentDoesNotGetReferee() {
    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    promoType.setCode(WellKnownSegmentType.PROMO.getCode());
    segment.setSegmentType(promoType);
    assertTrue(WellKnownSegmentType.PROMO.matches(segment.getSegmentType()));
    Npc referee = new Npc();
    referee.setName("Earl Hebner");
    when(npcService.findAllByType("Referee")).thenReturn(List.of(referee));
    when(npcService.pickUnusedReferee(anyList(), anyList())).thenReturn(referee);

    service.generateForSegment(10L);

    assertNull(segment.getReferee());
  }

  @Test
  void providerUnavailableLeavesSegmentUntouched() {
    when(narrationServiceFactory.getBestAvailableService()).thenReturn(null);

    service.generateForSegment(10L);

    assertFalse(segment.isNarrationGenerated());
    assertNull(segment.getNarration());
  }

  @Test
  void narrationProviderAvailableMirrorsFactoryAvailability() {
    // 2.13.0 pre-flight: the view asks the generation service (which owns the factory)
    // whether narration will actually run, so the dialog can say so up front.
    when(narrationServiceFactory.getBestAvailableService()).thenReturn(narrationService);
    assertTrue(service.isNarrationProviderAvailable());

    when(narrationServiceFactory.getBestAvailableService()).thenReturn(null);
    assertFalse(service.isNarrationProviderAvailable());
  }

  @Test
  void narrationProviderAvailableFalseWhenProviderNotUsable() {
    // A factory that returns a service whose isAvailable() is false must count as unavailable.
    when(narrationService.isAvailable()).thenReturn(false);
    when(narrationServiceFactory.getBestAvailableService()).thenReturn(narrationService);

    assertFalse(service.isNarrationProviderAvailable());
  }

  @Test
  void providerFailurePropagatesToShowService() {
    when(narrationService.narrateSegment(
            any(SegmentNarrationService.SegmentNarrationContext.class)))
        .thenThrow(new AIServiceException(500, "Error", "Claude", "boom", null));

    assertThrows(AIServiceException.class, () -> service.generateForSegment(10L));
    // Flag never set — ShowService's catch decides what happens next.
    assertFalse(segment.isNarrationGenerated());
  }
}
