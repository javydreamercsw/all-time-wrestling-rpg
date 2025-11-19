package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.SegmentSyncService;
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

  private NotionSyncService notionSyncService;
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
    notionSyncService = new NotionSyncService(objectMapper, notionSyncProperties);
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
    BaseSyncService.SyncResult result = notionSyncService.syncSegments("test-operation-123");

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
