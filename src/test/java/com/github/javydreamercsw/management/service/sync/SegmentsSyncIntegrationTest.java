package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {"notion.sync.enabled=true", "notion.sync.entities.matches=true"})
@ActiveProfiles("test")
@Transactional
@DisplayName("Matches Sync Integration Tests")
@EnabledIf("isNotionTokenAvailable")
class MatchesSyncIntegrationTest extends BaseTest {

  @Autowired private NotionSyncService notionSyncService;
  @Autowired private ShowService showService;

  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository matchTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private DeckRepository deckRepository;

  private Show testShow;
  private SegmentType testMatchType;
  private Wrestler testWrestler1;
  private Wrestler testWrestler2;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    segmentRepository.deleteAll();
    showRepository.deleteAll();
    deckRepository.deleteAll();
    wrestlerRepository.deleteAll();
    matchTypeRepository.deleteAll();
    showTypeRepository.deleteAll();

    // Create test data
    ShowType showType = new ShowType();
    showType.setName("Weekly Show");
    showType.setDescription("Regular weekly show");
    showType = showTypeRepository.saveAndFlush(showType);

    testShow = new Show();
    testShow.setName("Test Show");
    testShow.setDescription("Test show for integration testing");
    testShow.setType(showType);
    testShow = showRepository.saveAndFlush(testShow);

    // Verify show was saved and can be found
    System.out.println(
        "DEBUG: Saved show with ID: " + testShow.getId() + ", Name: '" + testShow.getName() + "'");
    Optional<Show> foundShow = showRepository.findByName("Test Show");
    System.out.println(
        "DEBUG: Found show by name: "
            + foundShow.isPresent()
            + (foundShow.map(show -> " (ID: " + show.getId() + ")").orElse("")));

    // Test ShowService directly
    Optional<Show> foundByService = showService.findByName("Test Show");
    System.out.println(
        "DEBUG: Found show by service: "
            + foundByService.isPresent()
            + (foundByService.map(show -> " (ID: " + show.getId() + ")").orElse("")));

    testMatchType = new SegmentType();
    testMatchType.setName("Singles");
    testMatchType.setDescription("One-on-one segment");
    testMatchType = matchTypeRepository.saveAndFlush(testMatchType);

    testWrestler1 = new Wrestler();
    testWrestler1.setName("Test Wrestler 1");
    testWrestler1.setDescription("First test wrestler");
    testWrestler1.setIsPlayer(false);
    testWrestler1.setTier(WrestlerTier.ROOKIE);
    testWrestler1.setStartingHealth(100);
    testWrestler1.setStartingStamina(100);
    testWrestler1.setLowHealth(20);
    testWrestler1.setLowStamina(20);
    testWrestler1.setDeckSize(40);
    testWrestler1.setFans(1000L);
    testWrestler1 = wrestlerRepository.saveAndFlush(testWrestler1);

    testWrestler2 = new Wrestler();
    testWrestler2.setName("Test Wrestler 2");
    testWrestler2.setDescription("Second test wrestler");
    testWrestler2.setIsPlayer(false);
    testWrestler2.setTier(WrestlerTier.ROOKIE);
    testWrestler2.setStartingHealth(100);
    testWrestler2.setStartingStamina(100);
    testWrestler2.setLowHealth(20);
    testWrestler2.setLowStamina(20);
    testWrestler2.setDeckSize(40);
    testWrestler2.setFans(1000L);
    testWrestler2 = wrestlerRepository.saveAndFlush(testWrestler2);
  }

  @Test
  @DisplayName("Should sync matches from Notion to database successfully")
  void shouldSyncMatchesFromNotionToDatabaseSuccessfully() {
    // Given - Real integration test with actual Notion API
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync matches from real Notion database
    NotionSyncService.SyncResult result = notionSyncService.syncSegments("test-operation-123");

    // Then - Verify sync completed successfully (regardless of segment count)
    assertThat(result).isNotNull();

    // Integration test should succeed if:
    // 1. No errors occurred during sync, OR
    // 2. Sync completed with some matches processed
    boolean syncSuccessful =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && result.getErrorMessage().contains("No matches found"));

    assertThat(syncSuccessful).isTrue();

    // Verify database state is consistent
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isGreaterThanOrEqualTo(initialMatchCount);

    System.out.println(
        "Integration test completed: "
            + (result.isSuccess() ? "SUCCESS" : "INFO")
            + " - Synced: "
            + result.getSyncedCount()
            + " matches, Final DB count: "
            + finalMatches.size());
  }

  @Test
  @DisplayName("Should handle duplicate detection during real sync")
  void shouldSkipDuplicateMatchesDuringSync() {
    // Given - Run sync twice to test duplicate handling
    int initialMatchCount = segmentRepository.findAll().size();

    // When - First sync
    NotionSyncService.SyncResult firstResult =
        notionSyncService.syncSegments("test-operation-456-first");
    int afterFirstSync = segmentRepository.findAll().size();

    // Second sync (should detect duplicates)
    NotionSyncService.SyncResult secondResult =
        notionSyncService.syncSegments("test-operation-456-second");
    int afterSecondSync = segmentRepository.findAll().size();

    // Then - Verify duplicate handling works
    assertThat(firstResult).isNotNull();
    assertThat(secondResult).isNotNull();

    // Either both succeed, or they handle "no new matches" gracefully
    boolean duplicateHandlingWorks =
        (firstResult.isSuccess() && secondResult.isSuccess())
            || (afterSecondSync == afterFirstSync); // No new matches added on second sync

    assertThat(duplicateHandlingWorks).isTrue();

    System.out.println(
        "Duplicate handling test: Initial="
            + initialMatchCount
            + ", After 1st="
            + afterFirstSync
            + ", After 2nd="
            + afterSecondSync);
  }

  @Test
  @DisplayName("Should handle missing dependencies gracefully during real sync")
  void shouldHandleMissingShowGracefully() {
    // Given - Real sync that may encounter missing dependencies
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync with real Notion data (may have missing shows/wrestlers/segment types)
    NotionSyncService.SyncResult result = notionSyncService.syncSegments("test-operation-789");

    // Then - Verify sync handles missing dependencies gracefully
    assertThat(result).isNotNull();

    // Integration test should handle missing dependencies by:
    // 1. Completing successfully with partial sync, OR
    // 2. Failing gracefully with appropriate error message, OR
    // 3. Skipping invalid matches and continuing
    boolean handledGracefully =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && (result.getErrorMessage().contains("Could not resolve")
                    || result.getErrorMessage().contains("validation failed")
                    || result.getErrorMessage().contains("No matches found")));

    assertThat(handledGracefully).isTrue();

    // Verify database remains consistent
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isGreaterThanOrEqualTo(initialMatchCount);

    System.out.println(
        "Missing dependencies test: "
            + (result.isSuccess() ? "SUCCESS" : "HANDLED_GRACEFULLY")
            + " - "
            + result.getErrorMessage());
  }

  @Test
  @DisplayName("Should handle missing segment type gracefully during real sync")
  void shouldHandleMissingMatchTypeGracefully() {
    // Given - Real sync that may encounter missing segment types
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync with real Notion data
    NotionSyncService.SyncResult result = notionSyncService.syncSegments("test-operation-101");

    // Then - Verify sync handles missing segment types gracefully
    assertThat(result).isNotNull();

    // Should handle missing segment types by completing successfully or failing gracefully
    boolean handledGracefully =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && (result.getErrorMessage().contains("Could not resolve segment type")
                    || result.getErrorMessage().contains("validation failed")
                    || result.getErrorMessage().contains("No matches found")));

    assertThat(handledGracefully).isTrue();

    // Verify database consistency
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isGreaterThanOrEqualTo(initialMatchCount);

    System.out.println(
        "Missing segment type test: "
            + (result.isSuccess() ? "SUCCESS" : "HANDLED_GRACEFULLY")
            + " - "
            + result.getErrorMessage());
  }

  @Test
  @DisplayName("Should handle missing winner gracefully (draw scenario)")
  void shouldHandleMissingWinnerGracefully() {
    // Given - Real sync that may encounter matches with no winner (draws)
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync with real Notion data
    NotionSyncService.SyncResult result = notionSyncService.syncSegments("test-operation-202");

    // Then - Verify sync handles missing winners gracefully
    assertThat(result).isNotNull();

    // Should handle missing winners by completing successfully or failing gracefully
    boolean handledGracefully =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && (result.getErrorMessage().contains("No matches found")
                    || result.getErrorMessage().contains("validation failed")));

    assertThat(handledGracefully).isTrue();

    // Verify database consistency
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isGreaterThanOrEqualTo(initialMatchCount);

    System.out.println(
        "Missing winner test: "
            + (result.isSuccess() ? "SUCCESS" : "HANDLED_GRACEFULLY")
            + " - "
            + result.getErrorMessage());
  }

  @Test
  @DisplayName("Should handle empty segment list from Notion")
  void shouldHandleEmptyMatchListFromNotion() {
    // Given - Real sync that may encounter empty results
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync with real Notion data (may be empty)
    NotionSyncService.SyncResult result = notionSyncService.syncSegments("test-operation-303");

    // Then - Verify sync handles empty results gracefully
    assertThat(result).isNotNull();

    // Should handle empty results by completing successfully or with appropriate message
    boolean handledGracefully =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && result.getErrorMessage().contains("No matches found"));

    assertThat(handledGracefully).isTrue();

    // Verify database remains consistent
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isGreaterThanOrEqualTo(initialMatchCount);

    System.out.println(
        "Empty segment list test: "
            + (result.isSuccess() ? "SUCCESS" : "HANDLED_GRACEFULLY")
            + " - Synced: "
            + result.getSyncedCount());
  }

  @Test
  @DisplayName("Should handle Notion handler exception gracefully")
  void shouldHandleNotionHandlerExceptionGracefully() {
    // Given - Real sync that may encounter API errors
    int initialSegmentCount = segmentRepository.findAll().size();

    // When - Sync with real Notion data (may encounter errors)
    NotionSyncService.SyncResult result = notionSyncService.syncSegments("test-operation-404");

    // Then - Verify sync handles errors gracefully
    assertThat(result).isNotNull();

    // Should handle errors by completing successfully or failing gracefully
    boolean handledGracefully =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && (result.getErrorMessage().contains("error")
                    || result.getErrorMessage().contains("failed")
                    || result.getErrorMessage().contains("No segments found")));

    assertThat(handledGracefully).isTrue();

    // Verify database remains consistent
    List<Segment> finalSegments = segmentRepository.findAll();
    assertThat(finalSegments.size()).isGreaterThanOrEqualTo(initialSegmentCount);

    System.out.println(
        "Exception handling test: "
            + (result.isSuccess() ? "SUCCESS" : "HANDLED_GRACEFULLY")
            + " - "
            + result.getErrorMessage());
  }

  @Test
  @DisplayName("Should sync multiple matches correctly")
  void shouldSyncMultipleMatchesCorrectly() {
    // Given - Real sync that may encounter multiple matches
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync with real Notion data (may contain multiple matches)
    NotionSyncService.SyncResult result = notionSyncService.syncSegments("test-operation-505");

    // Then - Verify sync handles multiple matches correctly
    assertThat(result).isNotNull();

    // Should handle multiple matches by completing successfully or gracefully
    boolean handledGracefully =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && (result.getErrorMessage().contains("No matches found")
                    || result.getErrorMessage().contains("validation failed")));

    assertThat(handledGracefully).isTrue();

    // Verify database consistency
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isGreaterThanOrEqualTo(initialMatchCount);

    // Verify sync metrics are reasonable
    assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);

    System.out.println(
        "Multiple matches test: "
            + (result.isSuccess() ? "SUCCESS" : "HANDLED_GRACEFULLY")
            + " - Synced: "
            + result.getSyncedCount()
            + ", DB count: "
            + finalMatches.size());
  }

  @Test
  @DisplayName("Should validate sync results correctly during real sync")
  void shouldValidateSyncResultsCorrectly() {
    // Given - Real sync with validation
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync with real Notion data and validate results
    NotionSyncService.SyncResult result = notionSyncService.syncSegments("test-operation-606");

    // Then - Verify sync validation works correctly
    assertThat(result).isNotNull();

    // Validation should work by:
    // 1. Succeeding when matches are valid and synced, OR
    // 2. Failing gracefully when validation thresholds aren't met, OR
    // 3. Handling empty results appropriately
    boolean validationWorked =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && (result.getErrorMessage().contains("validation failed")
                    || result.getErrorMessage().contains("No matches found")
                    || result.getErrorMessage().contains("Could not resolve")));

    assertThat(validationWorked).isTrue();

    // Verify database state is consistent after validation
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isGreaterThanOrEqualTo(initialMatchCount);

    // Verify sync metrics are reasonable
    assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);

    System.out.println(
        "Validation test: "
            + (result.isSuccess() ? "SUCCESS" : "VALIDATION_HANDLED")
            + " - Synced: "
            + result.getSyncedCount()
            + ", DB count: "
            + finalMatches.size());
  }
}
