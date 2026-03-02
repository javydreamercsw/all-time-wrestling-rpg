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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentStatus;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

class SegmentNotionSyncServiceTest extends AbstractSyncTest {
  private SegmentNotionSyncService segmentNotionSyncService;
  @Captor private ArgumentCaptor<Segment> segmentCaptor;

  @BeforeEach
  protected void setUp() {
    super.setUp();
    segmentNotionSyncService =
        new SegmentNotionSyncService(segmentRepository, syncServiceDependencies, notionApiExecutor);
  }

  @Test
  @DisplayName("Test Sync to Notion with a single new Segment")
  void testSyncToNotionSingleNew() {
    // Given
    String operationId = UUID.randomUUID().toString();

    Show show = new Show();
    show.setName("Test Show");
    show.setExternalId(UUID.randomUUID().toString());

    SegmentType type = new SegmentType();
    type.setName("Match");
    type.setExternalId(UUID.randomUUID().toString());

    Wrestler w1 = new Wrestler();
    w1.setName("Wrestler 1");
    w1.setExternalId(UUID.randomUUID().toString());

    Npc referee = new Npc();
    referee.setName("Ref 1");
    referee.setExternalId(UUID.randomUUID().toString());

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(type);
    segment.setSegmentDate(Instant.now());
    segment.setStatus(SegmentStatus.COMPLETED);
    segment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
    segment.setNarration("Exciting match");
    segment.setSummary("Wrestler 1 won");
    segment.setSegmentOrder(1);
    segment.setMainEvent(true);
    segment.setIsTitleSegment(false);
    segment.setReferee(referee);
    segment.setRefereeAwarenessLevel(50);
    segment.addParticipant(w1);
    segment.setWinners(List.of(w1));

    NotionClient client = mock(NotionClient.class);

    when(segmentRepository.findAll()).thenReturn(List.of(segment));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");

    Page page = mock(Page.class);

    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    // When
    var result = segmentNotionSyncService.syncToNotion(operationId);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
    verify(segmentRepository, times(1)).saveAndFlush(segmentCaptor.capture());
    Segment savedSegment = segmentCaptor.getValue();
    assertNotNull(savedSegment.getExternalId());
    assertNotNull(savedSegment.getLastSync());
  }

  @Test
  @DisplayName("Test Sync to Notion with no Segments")
  void testSyncToNotionEmpty() {
    // Given
    String operationId = UUID.randomUUID().toString();

    when(segmentRepository.findAll()).thenReturn(Collections.emptyList());

    NotionClient client = mock(NotionClient.class);

    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");

    // When
    var result = segmentNotionSyncService.syncToNotion(operationId);

    // Then
    assertNotNull(result);
    assertEquals(0, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
    verify(segmentRepository, times(0)).save(any());
  }
}
