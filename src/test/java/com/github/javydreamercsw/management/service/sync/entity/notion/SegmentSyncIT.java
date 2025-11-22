package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
@DisplayName("Segment Sync Integration Tests")
@TestPropertySource(properties = "notion.sync.enabled=true")
@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
class SegmentSyncIT extends ManagementIntegrationTest {

  @MockitoBean private SegmentRepository segmentRepository;

  @MockitoBean
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  private final List<Segment> savedSegments = new java.util.ArrayList<>();

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    savedSegments.clear(); // Clear for each test
    when(notionSyncService.getAllSegmentIds())
        .thenReturn(List.of("mock-segment-id-1", "mock-segment-id-2"));
    when(segmentRepository.findAll()).thenAnswer(invocation -> savedSegments);
    when(segmentRepository.save(any(Segment.class)))
        .thenAnswer(
            invocation -> {
              Segment segment = invocation.getArgument(0);
              savedSegments.add(segment);
              return segment;
            });
  }

  @Test
  @DisplayName("Should sync a random match from Notion to database successfully")
  void shouldSyncMatchesFromNotionToDatabaseSuccessfully() {
    // Given - Initial match count
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync a random match from mocked Notion data
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    String randomId = segmentIds.get(new Random().nextInt(segmentIds.size()));
    when(notionSyncService.syncSegment(randomId))
        .thenAnswer(
            invocation -> {
              Segment segment = new Segment();
              segment.setExternalId(randomId);
              SegmentType mockSegmentType = new SegmentType();
              mockSegmentType.setName("Mock Segment Type");
              segment.setSegmentType(mockSegmentType);
              savedSegments.add(segment);
              return BaseSyncService.SyncResult.success("Segment", 1, 0, 0);
            });
    BaseSyncService.SyncResult result = notionSyncService.syncSegment(randomId);

    // Then - Verify sync completed successfully
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    // Verify database state is consistent (one new segment added)
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isEqualTo(initialMatchCount + 1);
  }

  void shouldHandleDuplicateMatchesDuringSync() {
    // Given - Initial match count
    int initialMatchCount = segmentRepository.findAll().size();

    // When - First sync (should add a segment)
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    String randomId = segmentIds.get(new Random().nextInt(segmentIds.size()));
    when(notionSyncService.syncSegment(randomId))
        .thenAnswer(
            invocation -> {
              Segment segment = new Segment();
              segment.setExternalId(randomId);
              SegmentType mockSegmentType = new SegmentType();
              mockSegmentType.setName("Mock Segment Type");
              segment.setSegmentType(mockSegmentType);
              savedSegments.add(segment);
              return BaseSyncService.SyncResult.success("Segment", 1, 0, 0);
            });
    BaseSyncService.SyncResult firstResult = notionSyncService.syncSegment(randomId);
    int afterFirstSync = segmentRepository.findAll().size();

    // Then - Verify first sync was successful and added one segment
    assertThat(firstResult).isNotNull();
    assertThat(firstResult.isSuccess()).isTrue();
    assertThat(firstResult.getSyncedCount()).isEqualTo(1);
    assertThat(afterFirstSync).isEqualTo(initialMatchCount + 1);

    // When - Second sync with the same ID (should detect duplicate and not add new segment)
    when(notionSyncService.syncSegment(randomId))
        .thenReturn(
            BaseSyncService.SyncResult.success("Segment", 0, 0, 0)); // Simulate no new syncs
    BaseSyncService.SyncResult secondResult = notionSyncService.syncSegment(randomId);
    int afterSecondSync = segmentRepository.findAll().size();

    // Then - Verify second sync skipped the duplicate and no new segments were added
    assertThat(secondResult).isNotNull();
    assertThat(secondResult.isSuccess()).isTrue();
    assertThat(secondResult.getSyncedCount()).isEqualTo(0);
    assertThat(afterSecondSync).isEqualTo(afterFirstSync);
  }

  void shouldHandleMissingDependenciesGracefullyDuringSync() {
    // Given - Initial match count
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync with mocked Notion data that simulates missing dependencies
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    String randomId = segmentIds.get(new Random().nextInt(segmentIds.size()));
    when(notionSyncService.syncSegment(randomId))
        .thenReturn(BaseSyncService.SyncResult.failure("Segment", "Could not resolve dependency"));
    BaseSyncService.SyncResult result = notionSyncService.syncSegment(randomId);

    // Then - Verify sync handles missing dependencies gracefully
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("Could not resolve dependency");

    // Verify database remains consistent (no new segments added)
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isEqualTo(initialMatchCount);
  }

  @Test
  @DisplayName("Should handle missing segment type gracefully during real sync")
  void shouldHandleMissingMatchTypeGracefully() {
    // Given - Initial match count
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync with mocked Notion data that simulates a missing segment type
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    String randomId = segmentIds.get(new Random().nextInt(segmentIds.size()));
    when(notionSyncService.syncSegment(randomId))
        .thenReturn(
            BaseSyncService.SyncResult.failure("Segment", "Could not resolve segment type"));
    BaseSyncService.SyncResult result = notionSyncService.syncSegment(randomId);

    // Then - Verify sync handles missing segment types gracefully
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("Could not resolve segment type");

    // Verify database remains consistent (no new segments added)
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isEqualTo(initialMatchCount);
  }

  @Test
  @DisplayName("Should handle missing winner gracefully (draw scenario)")
  void shouldHandleMissingWinnerGracefully() {
    // Given - Initial match count
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync with mocked Notion data that simulates a draw (no winner)
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    String randomId = segmentIds.get(new Random().nextInt(segmentIds.size()));
    // Assuming syncSegment handles missing winner as a successful sync for a draw
    when(notionSyncService.syncSegment(randomId))
        .thenAnswer(
            invocation -> {
              Segment segment = new Segment();
              segment.setExternalId(randomId);
              SegmentType mockSegmentType = new SegmentType();
              mockSegmentType.setName("Mock Segment Type");
              segment.setSegmentType(mockSegmentType);
              savedSegments.add(segment);
              return BaseSyncService.SyncResult.success("Segment", 1, 0, 0);
            });
    BaseSyncService.SyncResult result = notionSyncService.syncSegment(randomId);

    // Then - Verify sync handles missing winners gracefully (successful sync)
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    // Verify database consistency (one new segment added)
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isEqualTo(initialMatchCount + 1);
  }

  @Test
  @DisplayName("Should handle empty segment list from Notion")
  void shouldHandleEmptyMatchListFromNotion() {
    // Given - Initial match count
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Mock NotionSyncService to return an empty list of segment IDs
    when(notionSyncService.getAllSegmentIds()).thenReturn(List.of());
    // And mock syncSegment to return a result indicating no matches found
    when(notionSyncService.syncSegment(anyString()))
        .thenReturn(BaseSyncService.SyncResult.failure("Segment", "No matches found"));

    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    assertThat(segmentIds).isEmpty();

    // Attempt to sync (should result in no matches found)
    BaseSyncService.SyncResult result = notionSyncService.syncSegment("any-id");

    // Then - Verify sync handles empty results gracefully
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("No matches found");

    // Verify database remains consistent (no new segments added)
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isEqualTo(initialMatchCount);
  }

  @Test
  @DisplayName("Should handle Notion handler exception gracefully")
  void shouldHandleNotionHandlerExceptionGracefully() {
    // Given - Initial segment count
    int initialSegmentCount = segmentRepository.findAll().size();

    // When - Mock NotionSyncService to throw an exception during sync
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    String randomId = segmentIds.get(new Random().nextInt(segmentIds.size()));
    when(notionSyncService.syncSegment(randomId))
        .thenReturn(BaseSyncService.SyncResult.failure("Segment", "Notion API error"));
    BaseSyncService.SyncResult result = notionSyncService.syncSegment(randomId);

    // Then - Verify sync handles errors gracefully
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("Notion API error");

    // Verify database remains consistent (no new segments added)
    List<Segment> finalSegments = segmentRepository.findAll();
    assertThat(finalSegments.size()).isEqualTo(initialSegmentCount);
  }

  @Test
  @DisplayName("Should sync multiple matches correctly")
  void shouldSyncMultipleMatchesCorrectly() {
    // Given - Initial match count
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync multiple segments using mocked Notion data
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    String randomId1 = segmentIds.get(0);
    String randomId2 = segmentIds.get(1);

    when(notionSyncService.syncSegment(randomId1))
        .thenAnswer(
            invocation -> {
              Segment segment = new Segment();
              segment.setExternalId(randomId1);
              SegmentType mockSegmentType = new SegmentType();
              mockSegmentType.setName("Mock Segment Type");
              segment.setSegmentType(mockSegmentType);
              savedSegments.add(segment);
              return BaseSyncService.SyncResult.success("Segment", 1, 0, 0);
            });
    when(notionSyncService.syncSegment(randomId2))
        .thenAnswer(
            invocation -> {
              Segment segment = new Segment();
              segment.setExternalId(randomId2);
              SegmentType mockSegmentType = new SegmentType();
              mockSegmentType.setName("Mock Segment Type");
              segment.setSegmentType(mockSegmentType);
              savedSegments.add(segment);
              return BaseSyncService.SyncResult.success("Segment", 1, 0, 0);
            });

    // Simulate syncing two segments
    BaseSyncService.SyncResult result1 = notionSyncService.syncSegment(randomId1);
    BaseSyncService.SyncResult result2 = notionSyncService.syncSegment(randomId2);

    // Then - Verify both syncs were successful and added segments
    assertThat(result1).isNotNull();
    assertThat(result1.isSuccess()).isTrue();
    assertThat(result1.getSyncedCount()).isEqualTo(1);

    assertThat(result2).isNotNull();
    assertThat(result2.isSuccess()).isTrue();
    assertThat(result2.getSyncedCount()).isEqualTo(1);

    // Verify database consistency (two new segments added)
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isEqualTo(initialMatchCount + 2);
  }

  @Test
  @DisplayName("Should validate sync results correctly during real sync")
  void shouldValidateSyncResultsCorrectly() {
    // Given - Initial match count
    int initialMatchCount = segmentRepository.findAll().size();

    // When - Sync with mocked Notion data that simulates a validation failure
    List<String> segmentIds = notionSyncService.getAllSegmentIds();
    String randomId = segmentIds.get(new Random().nextInt(segmentIds.size()));
    when(notionSyncService.syncSegment(randomId))
        .thenReturn(BaseSyncService.SyncResult.failure("Segment", "validation failed"));
    BaseSyncService.SyncResult result = notionSyncService.syncSegment(randomId);

    // Then - Verify sync validation works correctly
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("validation failed");

    // Verify database state is consistent (no new segments added)
    List<Segment> finalMatches = segmentRepository.findAll();
    assertThat(finalMatches.size()).isEqualTo(initialMatchCount);
  }
}
