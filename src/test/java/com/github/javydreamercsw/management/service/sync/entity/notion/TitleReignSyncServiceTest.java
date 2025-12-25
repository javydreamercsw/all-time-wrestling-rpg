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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.notion.TitleReignPage;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TitleReignSyncServiceTest extends AbstractSyncTest {

  private TitleReignSyncService service;

  @BeforeEach
  @Override
  protected void setUp() {
    super.setUp(); // Call parent setup first
    service = new TitleReignSyncService(objectMapper, syncServiceDependencies, notionApiExecutor);
  }

  @Test
  void syncTitleReigns_whenAlreadySynced_shouldSkip() {
    // Given: A sync has already run successfully in this session.
    when(syncServiceDependencies.getSyncSessionManager().isAlreadySyncedInSession("titlereigns"))
        .thenReturn(true);
    lenient().when(syncProperties.isEntityEnabled("titlereigns")).thenReturn(true);
    lenient().when(notionHandler.loadAllTitleReigns()).thenReturn(Collections.emptyList());

    // When: The sync is called a second time.
    SyncResult result = service.syncTitleReigns("second-op");

    // Then: The sync should be skipped.
    assertTrue(result.isSuccess());
    // Notion should only have been called once.
    verify(notionHandler, never()).loadAllTitleReigns();
  }

  @Test
  void syncTitleReigns_whenDisabled_shouldSkip() {
    // Given: The entity sync is disabled in properties.
    lenient().when(syncProperties.isEntityEnabled("titlereigns")).thenReturn(false);

    // When: The sync is executed.
    SyncResult result = service.syncTitleReigns("test-op");

    // Then: The sync should be skipped.
    assertTrue(result.isSuccess());
    verify(notionHandler, never()).loadAllTitleReigns();
  }

  @Test
  void syncTitleReigns_whenSuccessful_shouldSaveReigns() {
    // Given: Notion returns a valid title reign page.
    lenient().when(syncProperties.isEntityEnabled("titlereigns")).thenReturn(true);

    TitleReignPage page = new TitleReignPage();
    page.setId("page-id-1");
    Map<String, Object> props = new HashMap<>();
    props.put("Title", "title-id-1");
    props.put("Champion", "wrestler-id-1");
    props.put("Reign Number", 1);
    props.put("Start Date", "2023-01-15");
    page.setRawProperties(props);
    List<TitleReignPage> pages = Collections.singletonList(page);

    lenient().when(notionHandler.loadAllTitleReigns()).thenReturn(pages);

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

    lenient()
        .when(titleReignRepository.findByTitleAndReignNumber(title, 1))
        .thenReturn(Optional.empty());

    // When: The sync is executed.
    SyncResult result = service.syncTitleReigns("test-op");

    // Then: The sync should succeed and a new reign should be saved.
    assertTrue(result.isSuccess());
    verify(titleReignRepository).save(any(TitleReign.class));
    // Verify that the saved reign has the correct champion(s)
    ArgumentCaptor<TitleReign> reignCaptor = ArgumentCaptor.forClass(TitleReign.class);
    verify(titleReignRepository).save(reignCaptor.capture());
    TitleReign savedReign = reignCaptor.getValue();
    assertThat(savedReign.getChampions()).containsExactly(wrestler);
    verify(healthMonitor).recordSuccess(anyString(), anyLong(), anyInt());
  }

  @Test
  void syncTitleReigns_whenTitleNotFound_shouldSkipReign() {
    // Given: Notion returns a page where the related title does not exist locally.
    lenient().when(syncProperties.isEntityEnabled("titlereigns")).thenReturn(true);

    TitleReignPage page = new TitleReignPage();
    page.setId("page-id-1");
    Map<String, Object> props = new HashMap<>();
    props.put("Title", "non-existent-title-id");
    props.put("Champion", "wrestler-id-1");
    page.setRawProperties(props);
    List<TitleReignPage> pages = Collections.singletonList(page);

    lenient().when(notionHandler.loadAllTitleReigns()).thenReturn(pages);

    lenient()
        .when(titleRepository.findByExternalId("non-existent-title-id"))
        .thenReturn(Optional.empty());

    // When: The sync is executed.
    SyncResult result = service.syncTitleReigns("test-op");

    // Then: The sync should still report success, but no reign should be saved.
    assertTrue(result.isSuccess());
    verify(titleReignRepository, never()).save(any(TitleReign.class));
  }
}
