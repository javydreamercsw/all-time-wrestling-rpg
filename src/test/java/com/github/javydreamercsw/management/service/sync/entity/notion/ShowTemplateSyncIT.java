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
import com.github.javydreamercsw.base.ai.notion.ShowTemplatePage;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
@DisplayName("Show Template Sync Integration Tests")
class ShowTemplateSyncIT extends ManagementIntegrationTest {

  @Autowired private ShowTemplateSyncService showTemplateSyncService;
  @Autowired private ShowTemplateService showTemplateService;

  @MockBean private NotionHandler notionHandler;

  @Mock private ShowTemplatePage showTemplatePage;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should sync show templates from Notion")
  void shouldSyncShowTemplatesFromNotion() {
    log.info("🎭 Testing show template sync with mock Notion data");

    // Given
    ShowType showType = new ShowType();
    showType.setName("Weekly");
    showType.setDescription("A weekly show");
    showTypeRepository.save(showType);

    String templateId = UUID.randomUUID().toString();
    when(showTemplatePage.getId()).thenReturn(templateId);
    when(showTemplatePage.getRawProperties())
        .thenReturn(
            Map.of(
                "Name", "Test Template", "Description", "Test Description", "Show Type", "Weekly"));

    when(notionHandler.loadAllShowTemplates()).thenReturn(List.of(showTemplatePage));

    // When
    BaseSyncService.SyncResult result = showTemplateSyncService.syncShowTemplates("test-operation");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    List<ShowTemplate> allTemplates = showTemplateService.findAll();
    assertThat(allTemplates).hasSize(1);
    ShowTemplate template = allTemplates.get(0);
    assertThat(template.getName()).isEqualTo("Test Template");
    assertThat(template.getExternalId()).isEqualTo(templateId);
    assertThat(template.getShowType().getName()).isEqualTo("Weekly");

    // Test update
    when(showTemplatePage.getRawProperties())
        .thenReturn(
            Map.of(
                "Name",
                "Test Template Updated",
                "Description",
                "Test Description Updated",
                "Show Type",
                "Weekly"));

    showTemplateSyncService.syncShowTemplates("test-operation-2");

    allTemplates = showTemplateService.findAll();
    assertThat(allTemplates).hasSize(1);
    template = allTemplates.get(0);
    assertThat(template.getName()).isEqualTo("Test Template Updated");
  }
}
