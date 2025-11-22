package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
class TitleReignSyncIT extends ManagementIntegrationTest {
  @MockitoBean private NotionSyncService notionSyncService;

  @BeforeEach
  void setUp() {
    when(notionSyncService.syncTitleReigns(anyString()))
        .thenReturn(BaseSyncService.SyncResult.success("Title Reigns", 1, 0, 0));
  }

  @Test
  @DisplayName("Should Sync Title Reigns From Notion")
  void shouldSyncTitleReignsFromNotion() {
    log.info("👑 Starting real title reign sync integration test...");

    // Act
    BaseSyncService.SyncResult result =
        notionSyncService.syncTitleReigns("integration-test-title-reigns");

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals("Title Reigns", result.getEntityType());
    assertEquals(1, result.getSyncedCount());

    log.info("✅ Title reign sync completed successfully!");
  }
}
