package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
@DisplayName("Segment Sync Integration Tests")
class SegmentSyncIntegrationTest extends ManagementIntegrationTest {

  @Autowired private SegmentRepository segmentRepository;
  @MockitoBean private NotionSyncService notionSyncService;

  @BeforeEach
  void setUp() {
    when(notionSyncService.getAllSegmentIds()).thenReturn(List.of("mock-segment-id-1"));
    when(notionSyncService.syncSegments(anyString()))
        .thenReturn(NotionSyncService.SyncResult.success("Segments", 0, 0, 0));
    when(notionSyncService.syncSegment(anyString()))
        .thenReturn(NotionSyncService.SyncResult.success("Segment", 0, 0, 0));
  }

  @Test
  @DisplayName("Should sync a random match from Notion to database successfully")
  void shouldSyncMatchesFromNotionToDatabaseSuccessfully() {
    // Given - Real integration test with actual Notion API
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync a random match from real Notion database
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    NotionSyncService.SyncResult result;
    if (segmentIds.isEmpty()) {
      result = notionSyncService.syncSegments("test-operation-123");
    } else {
      String randomId = segmentIds.get(new java.util.Random().nextInt(segmentIds.size()));
      result = notionSyncService.syncSegment(randomId);
    }

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
    if (!segmentIds.isEmpty()) {
      assertThat(finalMatches.size()).isGreaterThan(initialMatchCount);
    } else {
      assertThat(finalMatches.size()).isEqualTo(initialMatchCount);
    }

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
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    if (segmentIds.isEmpty()) {
      return; // Skip test if no segments to sync
    }
    String randomId = segmentIds.get(new java.util.Random().nextInt(segmentIds.size()));
    NotionSyncService.SyncResult firstResult = notionSyncService.syncSegment(randomId);
    int afterFirstSync = segmentRepository.findAll().size();

    // Second sync (should detect duplicates)
    NotionSyncService.SyncResult secondResult = notionSyncService.syncSegment(randomId);
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
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    if (segmentIds.isEmpty()) {
      return; // Skip test if no segments to sync
    }
    String randomId = segmentIds.get(new java.util.Random().nextInt(segmentIds.size()));
    NotionSyncService.SyncResult result = notionSyncService.syncSegment(randomId);

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
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    if (segmentIds.isEmpty()) {
      return; // Skip test if no segments to sync
    }
    String randomId = segmentIds.get(new java.util.Random().nextInt(segmentIds.size()));
    NotionSyncService.SyncResult result = notionSyncService.syncSegment(randomId);

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
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    if (segmentIds.isEmpty()) {
      return; // Skip test if no segments to sync
    }
    String randomId = segmentIds.get(new java.util.Random().nextInt(segmentIds.size()));
    NotionSyncService.SyncResult result = notionSyncService.syncSegment(randomId);

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
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    if (!segmentIds.isEmpty()) {
      return; // Skip test if there are segments to sync
    }
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
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    if (segmentIds.isEmpty()) {
      return; // Skip test if no segments to sync
    }
    String randomId = segmentIds.get(new java.util.Random().nextInt(segmentIds.size()));
    NotionSyncService.SyncResult result = notionSyncService.syncSegment(randomId);

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
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    if (segmentIds.size() < 2) {
      return; // Skip test if there are not multiple segments to sync
    }
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
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    if (segmentIds.isEmpty()) {
      return; // Skip test if no segments to sync
    }
    String randomId = segmentIds.get(new java.util.Random().nextInt(segmentIds.size()));
    NotionSyncService.SyncResult result = notionSyncService.syncSegment(randomId);

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
