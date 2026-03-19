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
package com.github.javydreamercsw.management.service.show;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

  @Mock private ShowRepository showRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SegmentAdjudicationService segmentAdjudicationService;
  @Mock private ApplicationEventPublisher eventPublisher; // Needed to avoid NPE
  @Mock private GameSettingService gameSettingService;
  @Mock private NewsGenerationService newsGenerationService;
  @Mock private LegacyService legacyService;
  @Mock private SecurityUtils securityUtils;
  @Mock private ArenaRepository arenaRepository;

  @Mock
  private com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository
      commentaryTeamRepository;

  @InjectMocks private ShowService showService;

  private Show show;
  private Segment segment;
  private Segment pendingSegment;
  private Segment adjudicatedSegment;
  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;

  @BeforeEach
  void setUp() {
    showService =
        new ShowService(
            showRepository,
            null,
            null,
            null,
            null,
            java.time.Clock.systemUTC(),
            segmentAdjudicationService,
            segmentRepository,
            eventPublisher,
            wrestlerService,
            wrestlerRepository,
            gameSettingService,
            commentaryTeamRepository,
            newsGenerationService,
            legacyService,
            securityUtils,
            arenaRepository);
    show = new Show();
    show.setId(1L);

    segment = new Segment();
    segment.setId(1L);
    segment.setShow(show);
    SegmentType segmentType = new SegmentType();
    segmentType.setName("Promo");
    segment.setSegmentType(segmentType);
    segment.setAdjudicationStatus(AdjudicationStatus.PENDING);

    SegmentType pendingSegmentType = new SegmentType();
    pendingSegmentType.setName("One on One");

    pendingSegment = new Segment();
    pendingSegment.setId(10L);
    pendingSegment.setSegmentType(pendingSegmentType);
    pendingSegment.setAdjudicationStatus(AdjudicationStatus.PENDING);

    adjudicatedSegment = new Segment();
    adjudicatedSegment.setId(11L);
    adjudicatedSegment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);

    wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler3 = new Wrestler();
    wrestler3.setId(3L);
  }

  @Test
  void testAdjudicateShow() {
    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(segmentRepository.findByShow(show))
        .thenReturn(Arrays.asList(pendingSegment, adjudicatedSegment));
    when(wrestlerRepository.findAll()).thenReturn(List.of(wrestler1, wrestler2, wrestler3));

    // When
    showService.adjudicateShow(1L);

    verify(segmentAdjudicationService, times(1)).adjudicateMatch(pendingSegment);
    verify(segmentAdjudicationService, never()).adjudicateMatch(adjudicatedSegment);

    verify(segmentRepository, times(1)).save(pendingSegment);
    verify(segmentRepository, never()).save(adjudicatedSegment);

    Assertions.assertEquals(AdjudicationStatus.ADJUDICATED, pendingSegment.getAdjudicationStatus());
  }

  @Test
  void testAdjudicateShow_HealsNonParticipatingWrestlers() {
    // Given
    segment.getSegmentType().setName("Match");
    segment.addParticipant(wrestler1);
    segment.addParticipant(wrestler2);

    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));
    when(wrestlerRepository.findAll()).thenReturn(List.of(wrestler1, wrestler2, wrestler3));

    // When
    showService.adjudicateShow(1L);

    // Then
    Assertions.assertNotNull(wrestler3.getId());
    verify(wrestlerService, times(1)).healChance(wrestler3.getId());
    Assertions.assertNotNull(wrestler1.getId());
    verify(wrestlerService, never()).healChance(wrestler1.getId());
    Assertions.assertNotNull(wrestler2.getId());
    verify(wrestlerService, never()).healChance(wrestler2.getId());
  }

  @Test
  void testAdjudicateShow_UpdatesGameDateAndPublishesEvent() {
    // Given
    LocalDate showDate = LocalDate.of(2025, 1, 1);
    show.setShowDate(showDate);
    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(segmentRepository.findByShow(show)).thenReturn(Collections.emptyList());
    when(wrestlerRepository.findAll()).thenReturn(Collections.emptyList());

    // When
    showService.adjudicateShow(1L);

    // Then
    verify(gameSettingService, times(1)).saveCurrentGameDate(showDate);
    verify(newsGenerationService, times(1)).rollForRumor();
    verify(eventPublisher, times(1)).publishEvent(any(AdjudicationCompletedEvent.class));
  }

  @Test
  void testAdjudicateShow_WrestlersInPromoAreNotParticipants() {
    // Given
    // segment is a promo with wrestler1 and wrestler2
    segment.getSegmentType().setName("Promo");
    segment.addParticipant(wrestler1);
    segment.addParticipant(wrestler2);

    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));
    when(wrestlerRepository.findAll()).thenReturn(List.of(wrestler1, wrestler2, wrestler3));

    // When
    showService.adjudicateShow(1L);

    // Then
    // All wrestlers should get a heal chance because promo wrestlers are not "participants"
    Assertions.assertNotNull(wrestler1.getId());
    verify(wrestlerService, times(1)).healChance(wrestler1.getId());
    Assertions.assertNotNull(wrestler2.getId());
    verify(wrestlerService, times(1)).healChance(wrestler2.getId());
    Assertions.assertNotNull(wrestler3.getId());
    verify(wrestlerService, times(1)).healChance(wrestler3.getId());
  }

  @Test
  void testAdjudicateShow_ShowNotFound() {
    // Given
    when(showRepository.findById(anyLong())).thenReturn(Optional.empty());

    // When & Then
    IllegalArgumentException exception =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> showService.adjudicateShow(1L));
    Assertions.assertEquals("Show not found: 1", exception.getMessage());
  }
}
