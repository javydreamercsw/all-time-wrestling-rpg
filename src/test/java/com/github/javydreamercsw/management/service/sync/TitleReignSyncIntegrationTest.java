package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

@Slf4j
@EnabledIf("isNotionTokenAvailable")
class TitleReignSyncIntegrationTest extends ManagementIntegrationTest {

  @Test
  @DisplayName("Should Sync Title Reigns From Notion")
  void shouldSyncTitleReignsFromNotion() {
    log.info("👑 Starting real title reign sync integration test...");

    // Act
    NotionSyncService.SyncResult result =
        notionSyncService.syncTitleReigns("integration-test-title-reigns");

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals("Title Reigns", result.getEntityType());

    log.info("✅ Title reign sync completed successfully!");

    // Verify data in the database
    List<TitleReign> allReigns = titleReignRepository.findAll();
    assertFalse(allReigns.isEmpty(), "Should have synced some title reigns");
    log.info("Found {} title reigns in the database.", allReigns.size());

    // Example: Verify a specific reign (adjust based on your Notion data)
    Optional<Title> atwWorldTitleOpt = titleRepository.findByName("ATW World");
    assertTrue(atwWorldTitleOpt.isPresent(), "ATW World title should exist");
    Title atwWorldTitle = atwWorldTitleOpt.get();

    List<TitleReign> atwWorldReigns = titleReignRepository.findByTitle(atwWorldTitle);
    assertFalse(atwWorldReigns.isEmpty(), "ATW World should have associated reigns");
    log.info("ATW World has {} reigns.", atwWorldReigns.size());

    // Further assertions can be added here to check specific reign details
    // e.g., check champion, start/end dates, reign number for a known reign.
  }
}
