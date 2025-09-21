package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.service.sync.entity.TitleSyncService;
import com.github.javydreamercsw.management.service.sync.entity.WrestlerSyncService;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "notion.sync.enabled=true",
      "notion.sync.entities.specific.titles.enabled=true",
      "notion.sync.entities.specific.titlereigns.enabled=true",
      "notion.sync.scheduler.enabled=false"
    })
@EnabledIf("isNotionTokenAvailable")
@Slf4j
class TitleReignSyncIntegrationTest extends BaseTest {

  @Autowired private NotionSyncService notionSyncService;

  @Autowired private TitleRepository titleRepository;

  @Autowired private TitleReignRepository titleReignRepository;

  @Autowired private WrestlerSyncService wrestlerSyncService;

  @Autowired private TitleSyncService titleSyncService;

  @BeforeEach
  void setUp() {
    // Ensure wrestlers and titles are synced first as title reigns depend on them.
    log.info("🚀 Pre-syncing wrestlers for title reign test...");
    wrestlerSyncService.syncWrestlers("wrestler-sync-for-reigns");
    log.info("✅ Wrestlers synced.");

    log.info("🏆 Pre-syncing titles for title reign test...");
    titleSyncService.syncTitles("title-sync-for-reigns");
    log.info("✅ Titles synced.");
  }

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
