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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import com.github.javydreamercsw.management.service.sync.parallel.ParallelSyncOrchestrator;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit test to verify Notion property resolution fixes by mocking dependencies. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Notion Property Unit Test")
class NotionPropertyTest {

  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;
  @Mock private SegmentSyncService segmentSyncService;
  @Mock private ParallelSyncOrchestrator parallelSyncOrchestrator;
  @Mock private EntitySyncConfiguration entitySyncConfiguration;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionSyncProperties notionSyncProperties;
  @Mock private NotionHandler notionHandler;

  private MockedStatic<EnvironmentVariableUtil> mockedEnvironmentVariableUtil;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Mock static method EnvironmentVariableUtil.isNotionTokenAvailable()
    mockedEnvironmentVariableUtil = mockStatic(EnvironmentVariableUtil.class);
    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::isNotionTokenAvailable)
        .thenReturn(true);

    // Configure mock behavior for NotionSyncProperties
    when(notionSyncProperties.getParallelThreads()).thenReturn(1);

    // Manually instantiate NotionSyncService with mocked constructor dependencies
    notionSyncService = new NotionSyncService(objectMapper, notionSyncProperties, notionHandler);
    ReflectionTestUtils.setField(notionSyncService, "segmentSyncService", segmentSyncService);
    ReflectionTestUtils.setField(
        notionSyncService, "parallelSyncOrchestrator", parallelSyncOrchestrator);
    ReflectionTestUtils.setField(
        notionSyncService, "entitySyncConfiguration", entitySyncConfiguration);
  }

  @AfterEach
  void tearDown() {
    mockedEnvironmentVariableUtil.close();
  }

  @Test
  void shouldResolveNotionPropertiesCorrectly() throws InterruptedException {
    // Given - Mock SegmentSyncService behavior
    List<String> mockSegmentIds =
        Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    when(segmentSyncService.getSegmentIds()).thenReturn(mockSegmentIds);
    when(segmentSyncService.syncSegments(anyString()))
        .thenReturn(BaseSyncService.SyncResult.success("Segment", 2, 0, 0));

    // When - Call getAllSegmentIds and syncSegments
    List<String> retrievedSegmentIds = notionSyncService.getAllSegmentIds();
    BaseSyncService.SyncResult result =
        notionSyncService.syncSegments("test-operation-123", SyncDirection.OUTBOUND);

    // Then - Verify interactions and results
    assertThat(retrievedSegmentIds).isEqualTo(mockSegmentIds);
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(2);

    verify(segmentSyncService, times(1)).getSegmentIds();
    verify(segmentSyncService, times(1)).syncSegments(anyString());
    verify(segmentSyncService, never())
        .syncSegment(anyString()); // Ensure individual sync is not called
  }
}
