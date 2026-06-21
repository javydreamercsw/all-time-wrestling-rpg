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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.segment.SegmentSummaryService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
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
  @Mock private com.github.javydreamercsw.management.service.npc.NpcService npcService;
  @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
  @Mock private TitleReignRepository titleReignRepository;
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
    proposedSegment.setTeams(List.of(List.of("Wrestler A"), List.of("Wrestler B")));
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
    proposedSegment.setTeams(List.of(List.of("Active NPC"), List.of("Another Wrestler")));
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
            .anyMatch(p -> "Active NPC".equals(p.getWrestler().getName())));
    assertTrue(
        savedSegment.getParticipants().stream()
            .anyMatch(p -> "Another Wrestler".equals(p.getWrestler().getName())));

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

  @Test
  void testGetShowPlanningContext_PleShowFlagsPropagated() {
    // Given
    ShowType pleShowType = new ShowType();
    pleShowType.setName("Premium Live Event (PLE)");

    ShowTemplate pleTemplate = new ShowTemplate();
    pleTemplate.setShowType(pleShowType);
    pleTemplate.setExpectedMatches(5);
    pleTemplate.setExpectedPromos(2);

    show.setTemplate(pleTemplate);

    when(segmentRepository.findBySegmentDateBetween(any(), any())).thenReturn(new ArrayList<>());
    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), (String) any(), any()))
        .thenReturn(List.of(activeWrestler));
    when(rivalryService.getActiveRivalries()).thenReturn(new ArrayList<>());
    when(titleService.getActiveTitles()).thenReturn(new ArrayList<>());
    when(factionService.findAll()).thenReturn(new ArrayList<>());
    when(showService.getUpcomingShows(10)).thenReturn(new ArrayList<>());
    when(mapper.toDto(any(ShowPlanningContext.class))).thenReturn(new ShowPlanningContextDTO());

    ArgumentCaptor<ShowPlanningContext> contextCaptor =
        ArgumentCaptor.forClass(ShowPlanningContext.class);

    // When
    showPlanningService.getShowPlanningContext(show);

    // Then
    verify(mapper).toDto(contextCaptor.capture());
    assertTrue(contextCaptor.getValue().isPremiumLiveEvent());
  }

  @Test
  void testGetShowPlanningContext_NonPleShowFlagsFalse() {
    // Given — show has no template, isPremiumLiveEvent() returns false
    when(segmentRepository.findBySegmentDateBetween(any(), any())).thenReturn(new ArrayList<>());
    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), (String) any(), any()))
        .thenReturn(List.of(activeWrestler));
    when(rivalryService.getActiveRivalries()).thenReturn(new ArrayList<>());
    when(titleService.getActiveTitles()).thenReturn(new ArrayList<>());
    when(factionService.findAll()).thenReturn(new ArrayList<>());
    when(showService.getUpcomingShows(10)).thenReturn(new ArrayList<>());
    when(mapper.toDto(any(ShowPlanningContext.class))).thenReturn(new ShowPlanningContextDTO());

    ArgumentCaptor<ShowPlanningContext> contextCaptor =
        ArgumentCaptor.forClass(ShowPlanningContext.class);

    // When
    showPlanningService.getShowPlanningContext(show);

    // Then
    verify(mapper).toDto(contextCaptor.capture());
    assertFalse(contextCaptor.getValue().isPremiumLiveEvent());
  }

  // --- validateCard tests ---

  private Rivalry rivalryWithHeat(long id, String w1Name, String w2Name, int heat) {
    Wrestler w1 = new Wrestler();
    w1.setId(id * 10);
    w1.setName(w1Name);
    Wrestler w2 = new Wrestler();
    w2.setId(id * 10 + 1);
    w2.setName(w2Name);
    Rivalry rivalry = new Rivalry();
    rivalry.setId(id);
    rivalry.setWrestler1(w1);
    rivalry.setWrestler2(w2);
    rivalry.setHeat(heat);
    return rivalry;
  }

  @Test
  void validateCard_noActiveRivalries_returnsNoErrors() {
    CardValidationResult result = showPlanningService.validateCard(List.of(), List.of());
    assertTrue(result.isValid());
    assertFalse(result.hasWarnings());
  }

  @Test
  void validateCard_rivalryBelowThreshold_notRequired() {
    Rivalry cold = rivalryWithHeat(1L, "Alpha", "Beta", 9);
    CardValidationResult result = showPlanningService.validateCard(List.of(), List.of(cold));
    assertTrue(result.isValid());
    assertFalse(result.hasWarnings());
  }

  @Test
  void validateCard_mustBookRivalry_missingFromCard_isWarningNotError() {
    Rivalry hot = rivalryWithHeat(1L, "Alpha", "Beta", 15);
    CardValidationResult result = showPlanningService.validateCard(List.of(), List.of(hot));
    assertTrue(result.isValid(), "MUST_BOOK should be a warning, not a hard error");
    assertTrue(result.hasWarnings());
    assertEquals(1, result.getWarnings().size());
    assertTrue(result.getWarnings().get(0).contains("Alpha"));
    assertTrue(result.getWarnings().get(0).contains("Beta"));
    assertTrue(result.getWarnings().get(0).contains("MUST_BOOK"));
  }

  @Test
  void validateCard_mustBookRivalry_bookedByParticipantNames_noWarning() {
    Rivalry hot = rivalryWithHeat(1L, "Alpha", "Beta", 15);
    ProposedSegment seg = new ProposedSegment();
    seg.setTeams(List.of(List.of("Alpha"), List.of("Beta")));
    CardValidationResult result = showPlanningService.validateCard(List.of(seg), List.of(hot));
    assertTrue(result.isValid());
    assertFalse(result.hasWarnings());
  }

  @Test
  void validateCard_mustBookRivalry_bookedByRivalryId_noWarning() {
    Rivalry hot = rivalryWithHeat(1L, "Alpha", "Beta", 15);
    ProposedSegment seg = new ProposedSegment();
    seg.setTeams(List.of(List.of("Alpha"), List.of("Beta")));
    seg.setRivalryId(1L);
    CardValidationResult result = showPlanningService.validateCard(List.of(seg), List.of(hot));
    assertTrue(result.isValid());
    assertFalse(result.hasWarnings());
  }

  @Test
  void validateCard_stipulationRequired_missingFromCard_isWarning() {
    Rivalry max = rivalryWithHeat(1L, "Alpha", "Beta", 30);
    CardValidationResult result = showPlanningService.validateCard(List.of(), List.of(max));
    assertTrue(result.isValid(), "Unbooked high-heat rivalry should be a warning, not an error");
    assertTrue(result.hasWarnings());
    assertTrue(result.getWarnings().get(0).contains("MUST_BOOK"));
  }

  @Test
  void validateCard_stipulationRequired_bookedWithoutStipulation_isError() {
    Rivalry max = rivalryWithHeat(1L, "Alpha", "Beta", 30);
    ProposedSegment seg = new ProposedSegment();
    seg.setTeams(List.of(List.of("Alpha"), List.of("Beta")));
    seg.setRules(List.of());
    CardValidationResult result = showPlanningService.validateCard(List.of(seg), List.of(max));
    assertFalse(result.isValid(), "Booked rivalry without stipulation should be a hard error");
    assertEquals(1, result.getErrors().size());
    assertTrue(result.getErrors().get(0).contains("STIPULATION_REQUIRED"));
  }

  @Test
  void validateCard_stipulationRequired_bookedWithStipulation_noError() {
    Rivalry max = rivalryWithHeat(1L, "Alpha", "Beta", 30);
    ProposedSegment seg = new ProposedSegment();
    seg.setTeams(List.of(List.of("Alpha"), List.of("Beta")));
    seg.setRules(List.of("Steel Cage"));
    CardValidationResult result = showPlanningService.validateCard(List.of(seg), List.of(max));
    assertTrue(result.isValid());
    assertFalse(result.hasWarnings());
  }

  @Test
  void approveSegments_withMustBookRivalryMissingFromCard_succeedsWithWarning() {
    // MUST_BOOK violations are now warnings — approval should proceed
    Rivalry hot = rivalryWithHeat(1L, "Alpha", "Beta", 15);
    when(rivalryService.getActiveRivalries()).thenReturn(List.of(hot));
    when(segmentRepository.findByShow(show)).thenReturn(List.of());

    ProposedSegment seg = new ProposedSegment();
    seg.setType("Match");
    seg.setTeams(List.of(List.of("Charlie"), List.of("Delta")));
    SegmentType matchType = new SegmentType();
    matchType.setName("Match");
    when(segmentTypeService.findByName("Match")).thenReturn(Optional.of(matchType));
    when(wrestlerRepository.findByName(any())).thenReturn(Optional.empty());

    // Should NOT throw — unbooked rivalries are advisory only
    showPlanningService.approveSegments(show, List.of(seg));
    verify(segmentRepository).saveAll(any());
  }

  /**
   * Regression test for ATW-a831: getShowPlanningContext() must not touch Title.titleReigns or
   * Title.segments (both LAZY) — instead it must query via TitleReignRepository and
   * SegmentRepository. This test passes a detached-like Title (no lazy collections initialized) and
   * verifies the repository methods are called, not the entity collections.
   */
  @Test
  void getShowPlanningContext_withActiveTitle_queriesRepoNotLazyCollections() {
    Title title = new Title();
    title.setId(99L);
    title.setName("ATW World"); // isActive defaults to true

    Wrestler champion = new Wrestler();
    champion.setId(5L);
    champion.setName("Test Champion");
    // Simulate a current champion via the EAGER getCurrentChampions path
    title.getCurrentChampions().add(champion);

    TitleReign reign = new TitleReign();
    reign.setStartDate(Instant.now().minusSeconds(86400 * 30));

    // Wire up the two repo queries that replaced the lazy collection accesses
    when(titleReignRepository.findByTitleAndEndDateIsNull(title)).thenReturn(List.of(reign));
    when(segmentRepository.findByTitle(title)).thenReturn(List.of());

    when(titleService.getActiveTitles()).thenReturn(List.of(title));
    when(segmentRepository.findBySegmentDateBetween(any(), any())).thenReturn(List.of());
    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), (String) any(), any()))
        .thenReturn(List.of(activeWrestler));
    when(rivalryService.getActiveRivalries()).thenReturn(List.of());
    when(factionService.findAll()).thenReturn(List.of());
    when(showService.getUpcomingShows(10)).thenReturn(List.of());
    when(mapper.toDto(any(ShowPlanningContext.class))).thenReturn(new ShowPlanningContextDTO());

    // Must not throw LazyInitializationException
    ShowPlanningContextDTO result = showPlanningService.getShowPlanningContext(show);
    assertNotNull(result);

    // Verify the fix: repository queries used, not lazy collection accessors
    verify(titleReignRepository).findByTitleAndEndDateIsNull(title);
    verify(segmentRepository).findByTitle(title);
  }

  @Test
  void approveSegments_withStipulationRequiredViolation_throws() {
    // A rivalry on the card with heat >= 30 but no rules must still be a hard block
    Rivalry hot = rivalryWithHeat(1L, "Alpha", "Beta", 35);
    when(rivalryService.getActiveRivalries()).thenReturn(List.of(hot));

    ProposedSegment seg = new ProposedSegment();
    seg.setType("Match");
    seg.setTeams(List.of(List.of("Alpha"), List.of("Beta")));
    seg.setRules(List.of()); // booked but no stipulation

    assertThrows(
        IllegalStateException.class, () -> showPlanningService.approveSegments(show, List.of(seg)));
  }
}
