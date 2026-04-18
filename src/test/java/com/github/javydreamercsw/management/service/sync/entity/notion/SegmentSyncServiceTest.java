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
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
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
  @Mock private ShowRepository showRepository;
  @Mock private SegmentTypeRepository segmentTypeRepository;
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
  void setUp() {
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
            showRepository,
            segmentTypeRepository,
            segmentRuleRepository,
            wrestlerService,
            titleRepository);
  }

  @Test
  void testProcessSingleSegment() {
    SegmentSyncService.SegmentSyncDTO dto = new SegmentSyncService.SegmentSyncDTO();
    dto.setExternalId("segment-1");
    dto.setName("Test Segment");
    dto.setShowExternalId("show-1");
    dto.setSegmentTypeExternalId("type-1");

    Segment segment = new Segment();
    segment.setExternalId("segment-1");

    Show show = new Show();
    show.setExternalId("show-1");

    SegmentType segmentType = new SegmentType();
    segmentType.setExternalId("type-1");

    when(segmentRepository.findByExternalId("segment-1")).thenReturn(Optional.of(segment));
    when(showRepository.findByExternalId("show-1")).thenReturn(Optional.of(show));
    when(segmentTypeRepository.findByExternalId("type-1")).thenReturn(Optional.of(segmentType));
    when(segmentRepository.saveAndFlush(any(Segment.class))).thenReturn(segment);

    boolean result = segmentSyncService.processSingleSegment(dto);
    assertTrue(result);
    verify(segmentRepository).saveAndFlush(any(Segment.class));
  }
}
