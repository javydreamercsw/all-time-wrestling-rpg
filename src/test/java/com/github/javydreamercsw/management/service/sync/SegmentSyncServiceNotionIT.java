package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.SegmentSyncService;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("SegmentSyncService Integration Tests")
class SegmentSyncServiceNotionIT extends ManagementIntegrationTest {

  @MockitoBean private SegmentSyncService segmentSyncService;

  @MockitoBean private NotionSyncService notionSyncService;
  @MockitoBean private com.github.javydreamercsw.base.ai.notion.NotionHandler notionHandler;

  @BeforeEach
  void setUp() {
    when(notionHandler.getDatabasePageIds(anyString())).thenReturn(List.of("mock-segment-id-1"));
    when(segmentSyncService.getSegmentIds()).thenReturn(List.of("mock-segment-id-1"));
    when(segmentSyncService.syncSegment(anyString()))
        .thenReturn(BaseSyncService.SyncResult.success("Segment", 1, 0, 0));
    when(notionSyncService.syncSegment(anyString()))
        .thenReturn(BaseSyncService.SyncResult.success("Segment", 1, 0, 0));
  }

  @Test
  @DisplayName("Should sync a single segment by ID")
  void shouldSyncSingleSegmentById() {
    List<String> matchIds = segmentSyncService.getSegmentIds();
    Assumptions.assumeFalse(matchIds.isEmpty(), "No segments found in Notion to test sync.");
    Random r = new Random();
    Arrays.asList(
            matchIds.get(r.nextInt(matchIds.size())), matchIds.get(r.nextInt(matchIds.size())))
        .forEach(
            knownSegmentId -> {
              // When
              BaseSyncService.SyncResult result = segmentSyncService.syncSegment(knownSegmentId);
              // Then
              assertThat(result).isNotNull();
              assertThat(result.isSuccess()).isTrue();
              assertThat(result.getSyncedCount()).isEqualTo(1);
            });
  }
}
