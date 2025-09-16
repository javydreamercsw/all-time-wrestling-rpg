package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
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
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
class TitleSyncIntegrationTest extends BaseTest {

  @Autowired private NotionSyncService notionSyncService;

  @Autowired private TitleRepository titleRepository;

  @Autowired private WrestlerRepository wrestlerRepository;

  @Autowired private WrestlerSyncService wrestlerSyncService;

  @Autowired private TitleReignRepository titleReignRepository;

  @Autowired private TitleSyncService titleSyncService;

  @BeforeEach
  void setUp() {
    // Ensure wrestlers are synced first so champion relations can be resolved.
    log.info("🚀 Pre-syncing wrestlers for title test...");
    wrestlerSyncService.syncWrestlers("wrestler-sync-for-titles");
    log.info("✅ Wrestlers synced.");
  }

  @Test
  @DisplayName("Should Sync Titles From Notion")
  void shouldSyncTitlesFromNotion() {
    log.info("🚀 Starting real title sync integration test...");

    // Act
    NotionSyncService.SyncResult result = notionSyncService.syncTitles("integration-test-titles");

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals("Titles", result.getEntityType());

    log.info("✅ Title sync completed successfully!");

    // Verify data in the database
    Optional<Title> atwWorldTitleOpt = titleRepository.findByName("ATW World");
    assertTrue(atwWorldTitleOpt.isPresent(), "ATW World title should be in the database");

    Title atwWorldTitle = atwWorldTitleOpt.get();
    log.info("Verifying ATW World Title Champion...");
    // This assertion depends on the data in your Notion database.
    // If it fails, check that the 'ATW World' championship has a champion assigned in Notion.
    assertFalse(
        atwWorldTitle.getCurrentChampions().isEmpty(), "ATW World Champion should not be empty");
    log.info(
        "ATW World Champion is: {}",
        atwWorldTitle.getCurrentChampions().stream()
            .map(Wrestler::getName)
            .collect(java.util.stream.Collectors.joining(" & ")));

    log.info("Verifying ATW World Title #1 Contender...");
    // This assertion depends on the data in your Notion database.
    // If it fails, check that the 'ATW World' championship has a #1 contender assigned in Notion.
    assertNotNull(atwWorldTitle.getContender(), "ATW World #1 Contender should not be null");
    assertFalse(
        atwWorldTitle.getContender().isEmpty(), "ATW World #1 Contender should not be empty");
    log.info("ATW World #1 Contender is: {}", atwWorldTitle.getContender().get(0).getName());

    // Verify title reigns
    log.info("Verifying ATW World Title Reigns...");
    List<TitleReign> reigns = titleReignRepository.findByTitle(atwWorldTitle);
    assertFalse(reigns.isEmpty(), "ATW World title should have reigns");
    log.info("Found {} reigns for ATW World Title.", reigns.size());

    // Example: Assert at least one reign exists and check some properties
    TitleReign firstReign = reigns.get(0);
    assertFalse(firstReign.getChampions().isEmpty(), "First reign should have champions");
    assertNotNull(firstReign.getStartDate(), "First reign should have a start date");
    log.info(
        "First reign: Champion {} from {}",
        firstReign.getChampions().stream()
            .map(Wrestler::getName)
            .collect(java.util.stream.Collectors.joining(" & ")),
        firstReign.getStartDate());

    // Log all wrestler external IDs in the database
    List<Wrestler> allWrestlers = wrestlerRepository.findAll();
    log.info(
        "All wrestler external IDs in DB: {}",
        allWrestlers.stream().map(Wrestler::getExternalId).toList());
    // Log the champion IDs from Notion for ATW World
    log.info(
        "ATW World title current champions: {}",
        atwWorldTitle.getCurrentChampions().stream().map(Wrestler::getExternalId).toList());
  }
}
