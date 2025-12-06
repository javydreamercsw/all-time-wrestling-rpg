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
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.TitleReignPage;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
class TitleReignSyncIT extends ManagementIntegrationTest {
  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @MockitoBean private NotionHandler notionHandler;

  @Mock private TitleReignPage titleReignPage;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should Sync Title Reigns From Notion")
  @Transactional
  void shouldSyncTitleReignsFromNotion() {
    log.info("👑 Starting title reign sync integration test...");

    // Given
    Wrestler wrestler = createTestWrestler("Test Wrestler");
    wrestler.setExternalId("wrestler-id");
    wrestlerRepository.save(wrestler);

    Title title = new Title();
    title.setName("Test Title");
    title.setExternalId("title-id");
    title.setTier(WrestlerTier.MIDCARDER);
    titleRepository.save(title);

    String reignId = UUID.randomUUID().toString();
    when(titleReignPage.getId()).thenReturn(reignId);
    when(titleReignPage.getTitleRelationId()).thenReturn("title-id");
    when(titleReignPage.getChampionRelationId()).thenReturn("wrestler-id");
    when(titleReignPage.getReignNumber()).thenReturn(1);
    when(titleReignPage.getNotes()).thenReturn("Test Notes");
    when(titleReignPage.getStartDate()).thenReturn(LocalDate.now().toString());

    when(notionHandler.loadAllTitleReigns()).thenReturn(List.of(titleReignPage));

    // When
    BaseSyncService.SyncResult result =
        notionSyncService.syncTitleReigns("integration-test-title-reigns");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    List<TitleReign> reigns = titleReignRepository.findAll();
    assertThat(reigns).hasSize(1);
    TitleReign reign = reigns.get(0);
    assertThat(reign.getExternalId()).isEqualTo(reignId);
    assertThat(reign.getTitle().getName()).isEqualTo("Test Title");
    assertThat(reign.getChampions()).hasSize(1);
    assertThat(reign.getChampions().get(0).getName()).isEqualTo("Test Wrestler");
  }
}
