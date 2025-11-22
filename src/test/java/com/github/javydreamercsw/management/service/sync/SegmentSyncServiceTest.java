package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.service.sync.entity.notion.SegmentSyncService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SegmentSyncService Unit Tests")
class SegmentSyncServiceTest extends ManagementIntegrationTest {

  @Autowired private SegmentSyncService segmentSyncService;

  @MockitoBean private NotionHandler notionHandler;

  @Test
  @DisplayName("Should return failure for non-existent segment ID")
  void shouldReturnFailureForNonExistentSegmentId() {
    when(notionHandler.getSegmentPage(anyString())).thenReturn(Optional.empty());
    ReflectionTestUtils.setField(segmentSyncService, "notionHandler", notionHandler);

    SegmentSyncService.SyncResult result =
        segmentSyncService.syncSegment(UUID.randomUUID().toString());

    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("not found in Notion");
    assertThat(result.getSyncedCount()).isEqualTo(0);
  }
}
