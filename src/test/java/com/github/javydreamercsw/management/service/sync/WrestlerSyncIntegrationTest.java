package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.entity.WrestlerSyncService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Real integration test for wrestler sync that uses actual Spring services and real Notion API
 * calls. This test requires the NOTION_TOKEN environment variable to be set.
 *
 * <p>Run with: mvn test -Dtest=WrestlerSyncIntegrationTest -DNOTION_TOKEN=your_token
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "notion.sync.enabled=true",
      "notion.sync.entities=wrestlers",
      "notion.sync.scheduler.enabled=true"
    })
@EnabledIf("isNotionTokenAvailable")
@Slf4j
class WrestlerSyncIntegrationTest extends BaseTest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerSyncService wrestlerSyncService;

  @Test
  @DisplayName("Should correctly sync fans property from Notion")
  void shouldSyncFansPropertyFromNotion() {
    log.info("🧪 Verifying fans property sync from Notion...");

    // Ensure sync has run
    NotionSyncService.SyncResult result = wrestlerSyncService.syncWrestlers("fans-sync-test");

    SoftAssertions sa = new SoftAssertions();

    wrestlerRepository
        .findAll()
        .forEach(
            w ->
                sa.assertThat(w.getFans())
                    .withFailMessage(
                        "Wrestler '"
                            + w.getName()
                            + "' fans should be correctly synced from Notion. Expected > 0, Actual:"
                            + " "
                            + w.getFans())
                    .isGreaterThan(0));

    // The operation should complete (success or failure is less important than proper handling)
    sa.assertThat(result.isSuccess()).withFailMessage("Success status should be defined").isTrue();
    // Always log the result for debugging
    log.info("📊 Sync Result Summary:");
    log.info("   - Success: {}", result.isSuccess());
    log.info("   - Entity: {}", result.getEntityType());
    log.info("   - Synced: {}", result.getSyncedCount());
    log.info("   - Errors: {}", result.getErrorCount());
    if (!result.isSuccess()) {
      log.info("   - Error: {}", result.getErrorMessage());
    }
    sa.assertAll();
  }
}
