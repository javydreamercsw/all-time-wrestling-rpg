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
package com.github.javydreamercsw.management.service.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventAggregationServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private TitleReignRepository titleReignRepository;

  @InjectMocks private EventAggregationService eventAggregationService;

  private Segment recentSegment;
  private TitleReign recentTitleChange;

  @BeforeEach
  void setUp() {
    Instant now = Instant.now();

    Show show = new Show();
    show.setName("Raw");

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Singles Match");

    Wrestler winner = new Wrestler();
    winner.setId(1L);
    winner.setName("The Champion");

    recentSegment = new Segment();
    recentSegment.setSegmentDate(now.minus(5, ChronoUnit.DAYS));
    recentSegment.setShow(show);
    recentSegment.setSegmentType(segmentType);
    recentSegment.setIsTitleSegment(true);
    SegmentParticipant participant = new SegmentParticipant();
    participant.setWrestler(winner);
    participant.setIsWinner(true);
    recentSegment.getParticipants().add(participant);

    Title title = new Title();
    title.setName("World Heavyweight Championship");

    recentTitleChange = new TitleReign();
    recentTitleChange.setStartDate(now.minus(10, ChronoUnit.DAYS));
    recentTitleChange.setTitle(title);
    recentTitleChange.setChampions(Set.of(winner));
  }

  @Test
  void getMonthlySummary_returnsSegmentsWithinLastMonth() {
    when(segmentRepository.findBySegmentDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(recentSegment));
    when(titleReignRepository.findByStartDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of());

    EventAggregationService.MonthlySummary summary = eventAggregationService.getMonthlySummary();

    assertThat(summary.getSegments()).hasSize(1);
    assertThat(summary.getSegments().get(0)).isSameAs(recentSegment);
    assertThat(summary.getTitleChanges()).isEmpty();
  }

  @Test
  void getMonthlySummary_returnsTitleChangesWithinLastMonth() {
    when(segmentRepository.findBySegmentDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of());
    when(titleReignRepository.findByStartDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(recentTitleChange));

    EventAggregationService.MonthlySummary summary = eventAggregationService.getMonthlySummary();

    assertThat(summary.getTitleChanges()).hasSize(1);
    assertThat(summary.getSegments()).isEmpty();
  }

  @Test
  void getMonthlySummary_hasStartAndEndDates() {
    when(segmentRepository.findBySegmentDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of());
    when(titleReignRepository.findByStartDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of());

    EventAggregationService.MonthlySummary summary = eventAggregationService.getMonthlySummary();

    assertThat(summary.getStartDate()).isNotNull();
    assertThat(summary.getEndDate()).isNotNull();
    assertThat(summary.getStartDate()).isBefore(summary.getEndDate());
  }

  @Test
  void formatMonthlySummary_withTitleChange_includesTitleInfo() {
    when(segmentRepository.findBySegmentDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of());
    when(titleReignRepository.findByStartDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(recentTitleChange));

    EventAggregationService.MonthlySummary summary = eventAggregationService.getMonthlySummary();
    String formatted = eventAggregationService.formatMonthlySummary(summary);

    assertThat(formatted).contains("TITLE CHANGES");
    assertThat(formatted).contains("World Heavyweight Championship");
    assertThat(formatted).contains("The Champion");
  }

  @Test
  void formatMonthlySummary_withTitleSegment_includesMatchResults() {
    when(segmentRepository.findBySegmentDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of(recentSegment));
    when(titleReignRepository.findByStartDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of());

    EventAggregationService.MonthlySummary summary = eventAggregationService.getMonthlySummary();
    String formatted = eventAggregationService.formatMonthlySummary(summary);

    assertThat(formatted).contains("KEY MATCH RESULTS");
    assertThat(formatted).contains("The Champion");
  }

  @Test
  void formatMonthlySummary_empty_showsOnlyPeriod() {
    when(segmentRepository.findBySegmentDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of());
    when(titleReignRepository.findByStartDateBetween(any(Instant.class), any(Instant.class)))
        .thenReturn(List.of());

    EventAggregationService.MonthlySummary summary = eventAggregationService.getMonthlySummary();
    String formatted = eventAggregationService.formatMonthlySummary(summary);

    assertThat(formatted).contains("PERIOD:");
    assertThat(formatted).doesNotContain("TITLE CHANGES");
    assertThat(formatted).doesNotContain("KEY MATCH RESULTS");
  }
}
