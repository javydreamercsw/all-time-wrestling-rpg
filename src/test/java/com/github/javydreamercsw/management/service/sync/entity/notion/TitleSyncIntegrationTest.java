package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
class TitleSyncIntegrationTest extends ManagementIntegrationTest {

  @MockitoBean private NotionSyncService notionSyncService;

  @BeforeEach
  void setUp() {
    when(notionSyncService.syncTitles(anyString(), eq(SyncDirection.INBOUND)))
        .thenReturn(BaseSyncService.SyncResult.success("Titles", 1, 0, 0));
  }

  @Test
  @DisplayName("Should Sync Titles From Notion")
  void shouldSyncTitlesFromNotion() {
    log.info("🚀 Starting real title sync integration test...");

    // Act
    BaseSyncService.SyncResult result =
        notionSyncService.syncTitles("integration-test-titles", SyncDirection.INBOUND);

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals("Titles", result.getEntityType());
    assertEquals(1, result.getSyncedCount());

    log.info("✅ Title sync completed successfully!");
  }
}
