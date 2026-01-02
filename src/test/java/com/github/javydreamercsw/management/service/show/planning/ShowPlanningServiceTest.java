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
package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ShowPlanningServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private Clock clock;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ShowPlanningService showPlanningService;

  @Captor private ArgumentCaptor<List<Segment>> segmentsCaptor;

  @BeforeEach
  void setUp() {
    // Mock the clock to a fixed time
    when(clock.getZone()).thenReturn(java.time.ZoneOffset.UTC);
  }

  @Test
  void testApproveSegments_AllFieldsMapped() {
    // Setup
    Show show = new Show();
    show.setId(1L);
    show.setName("Test Show");
    show.setShowDate(LocalDate.now());

    ProposedSegment proposedSegment = new ProposedSegment();
    proposedSegment.setType("Singles Match");
    proposedSegment.setNarration("A great match");
    proposedSegment.setSummary("A summary of the match");
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

    // Execute
    showPlanningService.approveSegments(show, List.of(proposedSegment));

    // Verify
    org.mockito.Mockito.verify(segmentRepository).saveAll(segmentsCaptor.capture());
    List<Segment> capturedSegments = segmentsCaptor.getValue();
    assertEquals(1, capturedSegments.size());
    Segment capturedSegment = capturedSegments.get(0);

    assertEquals("A great match", capturedSegment.getNarration());
    assertEquals("A summary of the match", capturedSegment.getSummary());
    assertTrue(capturedSegment.getIsTitleSegment());
    assertEquals(1, capturedSegment.getTitles().size());
    assertEquals("World Championship", capturedSegment.getTitles().iterator().next().getName());
    assertEquals(1, capturedSegment.getSegmentRules().size());
    assertEquals("Rule 1", capturedSegment.getSegmentRules().get(0).getName());
    assertEquals(2, capturedSegment.getParticipants().size());
    assertEquals(1, capturedSegment.getWinners().size());
    assertEquals("Wrestler A", capturedSegment.getWinners().get(0).getName());
  }
}
