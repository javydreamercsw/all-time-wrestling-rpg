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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionRateLimitService;
import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.dto.SegmentDTO;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@Transactional
class SegmentSyncServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private SegmentService segmentService;
  @Mock private ShowService showService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private TitleRepository titleRepository;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionApiExecutor notionApiExecutor;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionRateLimitService rateLimitService;
  @Mock private SyncServiceDependencies syncServiceDependencies;

  private SegmentSyncService segmentSyncService;

  @BeforeEach
  public void setUp() {
    lenient().when(syncServiceDependencies.getNotionSyncProperties()).thenReturn(syncProperties);
    lenient().when(syncServiceDependencies.getNotionHandler()).thenReturn(notionHandler);
    lenient().when(syncServiceDependencies.getProgressTracker()).thenReturn(progressTracker);
    lenient().when(syncServiceDependencies.getHealthMonitor()).thenReturn(healthMonitor);
    lenient().when(syncServiceDependencies.getRateLimitService()).thenReturn(rateLimitService);
    segmentSyncService =
        new SegmentSyncService(
            objectMapper,
            syncServiceDependencies,
            notionApiExecutor,
            segmentRepository,
            segmentService,
            showService,
            segmentTypeService,
            segmentRuleRepository,
            wrestlerService,
            titleRepository);
  }

  @Test
  void testProcessSingleSegment() {
    SegmentDTO dto = new SegmentDTO();
    dto.setExternalId("segment-1");
    dto.setName("Test Segment");
    dto.setShowExternalId("show-1");
    dto.setSegmentTypeName("Match");
    dto.setParticipantNames(new java.util.ArrayList<>());
    dto.setWinnerNames(new java.util.ArrayList<>());

    Segment segment = new Segment();
    segment.setExternalId("segment-1");

    Show show = new Show();
    show.setExternalId("show-1");

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Match");

    when(segmentService.findByExternalId("segment-1")).thenReturn(Optional.of(segment));
    when(showService.findByExternalId("show-1")).thenReturn(Optional.of(show));
    when(segmentTypeService.findByName("Match")).thenReturn(Optional.of(segmentType));

    boolean result = segmentSyncService.processSingleSegment(dto);
    assertTrue(result);
    verify(existingSegment, times(1)).syncParticipants(any(java.util.List.class));
    verify(segmentService, times(1)).updateSegment(existingSegment);

    // Verify participants were updated correctly
    List<String> participantNames = new ArrayList<>();
    existingSegment.getParticipants().forEach(p -> participantNames.add(p.getWrestler().getName()));
    assertTrue(participantNames.contains("Wrestler A"));
    assertTrue(participantNames.contains("Wrestler C"));
    assertTrue(participantNames.size() == 2);

    // Verify winners were updated correctly
    List<String> winnerNames = new ArrayList<>();
    existingSegment.getWinners().forEach(w -> winnerNames.add(w.getName()));
    assertTrue(winnerNames.contains("Wrestler C"));
    assertTrue(winnerNames.size() == 1);
  }
}
