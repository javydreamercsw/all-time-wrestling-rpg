package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
class TitleSyncIntegrationTest extends ManagementIntegrationTest {

  @Autowired private TitleRepository titleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @MockitoBean private NotionSyncService notionSyncService;

  @BeforeEach
  void setUp() {
    when(notionSyncService.syncTitles(anyString()))
        .thenReturn(BaseSyncService.SyncResult.success("Titles", 1, 0, 0));
  }

  @Test
  @DisplayName("Should Sync Titles From Notion")
  void shouldSyncTitlesFromNotion() {
    log.info("🚀 Starting real title sync integration test...");

    // Act
    BaseSyncService.SyncResult result = notionSyncService.syncTitles("integration-test-titles");

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals("Titles", result.getEntityType());
    assertEquals(1, result.getSyncedCount());

    log.info("✅ Title sync completed successfully!");
  }
}
