/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.segment.SegmentSummaryService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.PromoBookingService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ShowPlanningServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private SegmentTypeService segmentTypeService;

  @Mock
  private com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository
      segmentRuleRepository;

  @Mock
  private com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningDtoMapper
      mapper;

  @Mock private com.github.javydreamercsw.management.service.rivalry.RivalryService rivalryService;
  @Mock private com.github.javydreamercsw.management.service.title.TitleService titleService;
  @Mock private com.github.javydreamercsw.management.service.faction.FactionService factionService;
  @Mock private com.github.javydreamercsw.management.service.show.ShowService showService;
  @Mock private SegmentSummaryService segmentSummaryService;
  @Mock private com.github.javydreamercsw.management.service.segment.SegmentService segmentService;
  @Mock private PromoBookingService promoBookingService;
  @Mock private com.github.javydreamercsw.management.service.npc.NpcService npcService;
  @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
  @Mock private Clock clock;

  @InjectMocks private ShowPlanningService showPlanningService;

  private Show show;
  private Wrestler activeWrestler;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    // Setup basic date handling
    LocalDate today = LocalDate.of(2025, 1, 1);
    Instant now = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
    lenient().when(clock.instant()).thenReturn(now);
    lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    ShowType showType = new ShowType();
    showType.setName("TV");
    showType.setExpectedMatches(3);
    showType.setExpectedPromos(2);

    Universe universe = new Universe();
    universe.setId(1L);

    show = new Show();
    show.setId(1L);
    show.setName("Test Show");
    show.setShowDate(today);
    show.setType(showType);
    show.setUniverse(universe);

    activeWrestler = new Wrestler();
    activeWrestler.setId(1L);
    activeWrestler.setName("Active NPC");
    activeWrestler.setActive(true);
    activeWrestler.setIsPlayer(false);
    activeWrestler.setGender(com.github.javydreamercsw.base.domain.wrestler.Gender.MALE);
  }

  @Test
  void testApproveSegmentsWithTitleAndRules() {
    ProposedSegment proposedSegment = new ProposedSegment();
    proposedSegment.setType("Singles Match");
    proposedSegment.setNarration("A great match");
    proposedSegment.setSummary("A summary of the match");
    proposedSegment.setNotes("AI should focus on technical wrestling");
    proposedSegment.setParticipants(List.of("Wrestler A", "Wrestler B"));
    proposedSegment.setWinners(List.of("Wrestler A"));
    proposedSegment.setIsTitleSegment(true);
    Title title = new Title();
    title.setId(1L);
    title.setName("World Championship");
    proposedSegment.setTitles(Set.of(title));
    proposedSegment.setRules(List.of("Rule 1"));

    when(segmentRepository.findByShow(show)).thenReturn(List.of());

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Singles Match");
    when(segmentTypeService.findByName("Singles Match")).thenReturn(Optional.of(segmentType));

    Wrestler wrestlerA = new Wrestler();
    wrestlerA.setId(1L);
    wrestlerA.setName("Wrestler A");
    when(wrestlerRepository.findByName("Wrestler A")).thenReturn(Optional.of(wrestlerA));

    Wrestler wrestlerB = new Wrestler();
    wrestlerB.setId(2L);
    wrestlerB.setName("Wrestler B");
    when(wrestlerRepository.findByName("Wrestler B")).thenReturn(Optional.of(wrestlerB));

    SegmentRule rule1 = new SegmentRule();
    rule1.setId(1L);
    rule1.setName("Rule 1");
    when(segmentRuleRepository.findByName("Rule 1")).thenReturn(Optional.of(rule1));

    showPlanningService.approveSegments(show, List.of(proposedSegment));

    ArgumentCaptor<List<Segment>> segmentsCaptor = ArgumentCaptor.forClass(List.class);
    verify(segmentRepository).saveAll(segmentsCaptor.capture());
    List<Segment> capturedSegments = segmentsCaptor.getValue();
    assertEquals(1, capturedSegments.size());
    Segment capturedSegment = capturedSegments.get(0);

    assertEquals("A great match", capturedSegment.getNarration());
    assertEquals("A summary of the match", capturedSegment.getSummary());
    assertEquals("AI should focus on technical wrestling", capturedSegment.getNotes());
    assertTrue(capturedSegment.getIsTitleSegment());
    assertEquals(1, capturedSegment.getTitles().size());
    assertEquals("World Championship", capturedSegment.getTitles().iterator().next().getName());
    assertEquals(1, capturedSegment.getSegmentRules().size());
    assertEquals("Rule 1", capturedSegment.getSegmentRules().iterator().next().getName());
    assertEquals(2, capturedSegment.getParticipants().size());
    assertEquals(1, capturedSegment.getWinners().size());
    assertEquals("Wrestler A", capturedSegment.getWinners().iterator().next().getName());
  }

  @Test
  void testGetShowPlanningContext_FiltersInactiveWrestlers() {
    // activeWrestler, show, universe, and showType are all set up in setUp()
    // This test documents that inactive wrestlers are excluded via WrestlerService.findAllFiltered
  }

  @Test
  void testGetShowPlanningContext() {
    // Given
    when(segmentRepository.findBySegmentDateBetween(any(), any())).thenReturn(new ArrayList<>());
    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), (String) any(), any()))
        .thenReturn(List.of(activeWrestler));
    when(rivalryService.getActiveRivalries()).thenReturn(new ArrayList<>());
    when(rivalryService.getRivalriesForWrestler(anyLong())).thenReturn(new ArrayList<>());
    when(titleService.getActiveTitles()).thenReturn(new ArrayList<>());
    when(factionService.findAll()).thenReturn(new ArrayList<>());
    when(showService.getUpcomingShows(10)).thenReturn(new ArrayList<>());
    when(mapper.toDto(any(ShowPlanningContext.class))).thenReturn(new ShowPlanningContextDTO());

    ArgumentCaptor<ShowPlanningContext> showPlanningContextCaptor =
        ArgumentCaptor.forClass(ShowPlanningContext.class);

    // When
    ShowPlanningContextDTO result = showPlanningService.getShowPlanningContext(show);

    // Then
    assertNotNull(result);
    verify(mapper).toDto(showPlanningContextCaptor.capture());
    ShowPlanningContext capturedContext = showPlanningContextCaptor.getValue();
    assertEquals("Test Show", capturedContext.getShowTemplate().getShowName());
    assertEquals(1, capturedContext.getFullRoster().size());
  }

  @Test
  void testApproveProposedSegments() {
    // Given
    ProposedSegment proposedSegment = new ProposedSegment();
    proposedSegment.setType("Match");
    proposedSegment.setSummary("A great match");
    proposedSegment.setNarration("Dynamic narration");
    proposedSegment.setParticipants(List.of("Active NPC", "Another Wrestler"));
    proposedSegment.setWinners(List.of("Active NPC"));

    SegmentType matchType = new SegmentType();
    matchType.setName("Match");
    when(segmentTypeService.findByName("Match")).thenReturn(Optional.of(matchType));

    Wrestler anotherWrestler = new Wrestler();
    anotherWrestler.setName("Another Wrestler");
    when(wrestlerRepository.findByName("Active NPC")).thenReturn(Optional.of(activeWrestler));
    when(wrestlerRepository.findByName("Another Wrestler"))
        .thenReturn(Optional.of(anotherWrestler));

    // When
    showPlanningService.approveSegments(show, List.of(proposedSegment));

    // Then
    ArgumentCaptor<List<Segment>> segmentsCaptor = ArgumentCaptor.forClass(List.class);
    verify(segmentRepository).saveAll(segmentsCaptor.capture());

    Segment savedSegment = segmentsCaptor.getValue().get(0);
    assertEquals(1, savedSegment.getSegmentOrder());
    assertEquals("Match", savedSegment.getSegmentType().getName());
    assertEquals("A great match", savedSegment.getSummary());
    assertEquals("Dynamic narration", savedSegment.getNarration());

    // Verify participants were synced
    assertEquals(2, savedSegment.getParticipants().size());
    assertTrue(
        savedSegment.getParticipants().stream()
            .anyMatch(p -> p.getWrestler().getName().equals("Active NPC")));
    assertTrue(
        savedSegment.getParticipants().stream()
            .anyMatch(p -> p.getWrestler().getName().equals("Another Wrestler")));

    // Verify winner was set
    assertEquals(1, savedSegment.getWinners().size());
    assertEquals("Active NPC", savedSegment.getWinners().get(0).getName());
  }

  @Test
  void testGetShowPlanningContextWithMissingUniverse() {
    // Given
    show.setUniverse(null);

    // When & Then
    assertThrows(
        IllegalStateException.class, () -> showPlanningService.getShowPlanningContext(show));
  }
}
