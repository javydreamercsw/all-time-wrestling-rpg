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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.notion.TitleReignPage;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

class TitleReignSyncServiceTest extends AbstractSyncTest {

  private TitleReignSyncService service;
  @Mock private TitleReignNotionSyncService titleReignNotionSyncService;

  @BeforeEach
  @Override
  protected void setUp() {
    super.setUp(); // Call parent setup first
    service =
        new TitleReignSyncService(
            objectMapper, syncServiceDependencies, titleReignNotionSyncService, notionApiExecutor);
  }

  @Test
  void syncTitleReigns_whenAlreadySynced_shouldSkip() {
    // Given: A sync has already run successfully in this session.
    when(syncSessionManager.isAlreadySyncedInSession(SyncEntityType.TITLE_REIGN.getKey()))
        .thenReturn(true);
    lenient()
        .when(syncProperties.isEntityEnabled(SyncEntityType.TITLE_REIGN.getKey()))
        .thenReturn(true);

    // When: The sync is called a second time.
    SyncResult result = service.syncTitleReigns("second-op");

    // Then: The sync should be skipped.
    assertTrue(result.isSuccess());
    verify(notionHandler, never()).loadAllTitleReigns();
  }

  @Test
  void syncTitleReigns_whenSuccessful_shouldSaveReigns() {
    // Given: Notion returns a valid title reign page.
    lenient()
        .when(syncProperties.isEntityEnabled(SyncEntityType.TITLE_REIGN.getKey()))
        .thenReturn(true);

    TitleReignPage page = mock(TitleReignPage.class);
    when(page.getId()).thenReturn("page-id-1");
    when(page.getTitleRelationId()).thenReturn("title-id-1");
    when(page.getChampionRelationIds()).thenReturn(List.of("wrestler-id-1"));
    when(page.getReignNumber()).thenReturn(1);
    when(page.getStartDate()).thenReturn("2023-01-15");
    when(page.getWonAtSegmentRelationId()).thenReturn("segment-id-1");

    lenient().when(notionHandler.loadAllTitleReigns()).thenReturn(List.of(page));

    Title title = new Title();
    title.setExternalId("title-id-1");
    title.setName("World Championship");
    lenient().when(titleRepository.findByExternalId("title-id-1")).thenReturn(Optional.of(title));

    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setExternalId("wrestler-id-1");
    wrestler.setName("Champion Wrestler");
    lenient()
        .when(wrestlerRepository.findByExternalId("wrestler-id-1"))
        .thenReturn(Optional.of(wrestler));

    Segment segment = new Segment();
    segment.setExternalId("segment-id-1");
    lenient()
        .when(segmentRepository.findByExternalId("segment-id-1"))
        .thenReturn(Optional.of(segment));

    lenient()
        .when(titleReignRepository.findByTitleAndReignNumber(title, 1))
        .thenReturn(Optional.empty());

    // When: The sync is executed.
    SyncResult result = service.syncTitleReigns("test-op");

    // Then: The sync should succeed and a new reign should be saved.
    assertTrue(result.isSuccess());
    ArgumentCaptor<TitleReign> reignCaptor = ArgumentCaptor.forClass(TitleReign.class);
    verify(titleReignRepository).save(reignCaptor.capture());
    TitleReign savedReign = reignCaptor.getValue();
    assertThat(savedReign.getChampions()).containsExactly(wrestler);
    assertThat(savedReign.getWonAtSegment()).isEqualTo(segment);
    verify(healthMonitor)
        .recordSuccess(eq(SyncEntityType.TITLE_REIGN.getKey()), anyLong(), anyInt());
  }
}
