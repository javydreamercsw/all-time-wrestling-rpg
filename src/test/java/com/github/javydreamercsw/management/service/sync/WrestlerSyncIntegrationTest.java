package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.WrestlerSyncService;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Real integration test for wrestler sync that uses actual Spring services and real Notion API
 * calls. This test requires the NOTION_TOKEN environment variable to be set.
 *
 * <p>Run with: mvn test -Dtest=WrestlerSyncIntegrationTest -DNOTION_TOKEN=your_token
 */
@Slf4j
@TestPropertySource(properties = "notion.sync.load-from-json=false")
class WrestlerSyncIntegrationTest extends ManagementIntegrationTest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerSyncService wrestlerSyncService;

  @MockBean private NotionHandler notionHandler;

  @BeforeEach
  void setUp() {
    wrestlerSyncService.notionHandler = notionHandler;
    // Mock NotionHandler to return a dummy NotionPage
    when(notionHandler.loadAllWrestlers())
        .thenReturn(
            List.of(
                new WrestlerPage() {
                  {
                    setId("dummy-id");
                    setRawProperties(
                        Map.of(
                            "Name",
                            Map.of(
                                "title",
                                List.of(Map.of("text", Map.of("content", "Test Wrestler")))),
                            "Fans",
                            100000));
                  }
                }));
  }

  @Test
  @DisplayName("Should correctly sync fans property from Notion")
  void shouldSyncFansPropertyFromNotion() {
    log.info("🧪 Verifying fans property sync from Notion...");

    // Ensure sync has run
    BaseSyncService.SyncResult result = wrestlerSyncService.syncWrestlers("fans-sync-test");

    // The operation should complete (success or failure is less important than proper handling)
    assertThat(result.isSuccess()).withFailMessage("Success status should be defined").isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    // Verify the fans property in the database
    wrestlerRepository
        .findByExternalId("dummy-id")
        .ifPresentOrElse(
            wrestler -> assertThat(wrestler.getFans()).isEqualTo(100000L),
            () -> fail("Wrestler with externalId 'dummy-id' not found in database"));

    // Always log the result for debugging
    log.info("📊 Sync Result Summary:");
    log.info("   - Success: {}", result.isSuccess());
    log.info("   - Entity: {}", result.getEntityType());
    log.info("   - Synced: {}", result.getSyncedCount());
    log.info("   - Errors: {}", result.getErrorCount());
    if (!result.isSuccess()) {
      log.info("   - Error: {}", result.getErrorMessage());
    }
  }
}
