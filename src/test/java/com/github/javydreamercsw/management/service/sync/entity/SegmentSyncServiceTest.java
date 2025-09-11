package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.base.ai.notion.SegmentPage;
import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SegmentSyncService Tests")
class SegmentSyncServiceTest extends BaseTest {

  @Mock private NotionHandler notionHandler;
  @Mock private SegmentService segmentService;
  @Mock private ShowService showService;
  @Mock private SegmentTypeService segmentTypeService;
  private NotionSyncProperties syncProperties; // Declare without @Mock
  @Mock private ObjectMapper objectMapper;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private NotionRateLimitService rateLimitService;
  @Mock private ShowSyncService showSyncService;

  private SegmentSyncService segmentSyncService;

  // Constructor to configure the mock before setUp()
  public SegmentSyncServiceTest() {
    syncProperties = mock(NotionSyncProperties.class); // Manually create mock
    lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
  }

  @BeforeEach
  void setUp() {
    segmentSyncService = new SegmentSyncService(objectMapper, syncProperties);

    // Manually inject the mocked dependencies using reflection
    setField(segmentSyncService, "notionHandler", notionHandler);
    setField(segmentSyncService, "progressTracker", progressTracker);
    setField(segmentSyncService, "rateLimitService", rateLimitService);
    setField(segmentSyncService, "segmentService", segmentService);
    setField(segmentSyncService, "showService", showService);
    setField(segmentSyncService, "segmentTypeService", segmentTypeService);
    setField(segmentSyncService, "showSyncService", showSyncService);
  }

  @Test
  @DisplayName("Should update existing matches successfully")
  void shouldUpdateExistingMatchesSuccessfully() {
    // Given
    SegmentPage matchPage = new SegmentPage();
    matchPage.setId("ext1");
    matchPage.setCreated_time(Instant.now().toString());
    matchPage.setLast_edited_time(Instant.now().toString());
    matchPage.setProperties(new SegmentPage.NotionProperties());
    matchPage
        .getProperties()
        .setShows(
            new SegmentPage.Property() {
              {
                setType("relation");
                setRelation(
                    List.of(
                        new NotionPage.Relation() {
                          {
                            setId("dummyShowExternalId");
                          }
                        }));
              }
            });
    matchPage.setRawProperties(
        Map.of(
            "Name",
            "Updated Test Match",
            "Segment Type",
            "Singles",
            "Participants",
            "",
            "Winners",
            ""));

    Segment existingMatch = new Segment();
    existingMatch.setId(1L);
    existingMatch.setExternalId("ext1");

    when(notionHandler.loadSegmentById(anyString())).thenReturn(Optional.of(matchPage));
    when(segmentService.findByExternalId(anyString())).thenReturn(Optional.of(existingMatch));
    when(showService.findByExternalId(anyString())).thenReturn(Optional.of(new Show()));
    when(segmentTypeService.findByName(anyString())).thenReturn(Optional.of(new SegmentType()));

    // When
    SyncResult result = segmentSyncService.syncSegment("ext1");

    // Then
    verify(segmentService, times(1)).updateSegment(any(Segment.class));
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should sync missing show when processing segment")
  void shouldSyncMissingShowWhenProcessingMatch() {
    // Given
    String matchId = "segment-with-missing-show";
    String missingShowExternalId = "missing-show-id";
    String matchName = "Test Match with Missing Show";

    SegmentPage matchPage = new SegmentPage();
    matchPage.setId(matchId);
    matchPage.setCreated_time(Instant.now().toString());
    matchPage.setLast_edited_time(Instant.now().toString());
    matchPage.setProperties(new SegmentPage.NotionProperties());
    matchPage
        .getProperties()
        .setShows(
            new SegmentPage.Property() {
              {
                setType("relation");
                setRelation(
                    List.of(
                        new NotionPage.Relation() {
                          {
                            setId(missingShowExternalId);
                          }
                        }));
              }
            });
    matchPage.setRawProperties(
        Map.of("Name", matchName, "Segment Type", "Singles", "Participants", "", "Winners", ""));

    // Mock NotionHandler to return the segment page
    when(notionHandler.loadSegmentById(matchId)).thenReturn(Optional.of(matchPage));

    // Mock matchResultService to return empty (new segment)
    when(segmentService.findByExternalId(anyString())).thenReturn(Optional.empty());

    // Mock showService to initially return empty, then return a show after sync
    Show syncedShow = new Show();
    syncedShow.setExternalId(missingShowExternalId);
    syncedShow.setName("Synced Show");
    when(showService.findByExternalId(missingShowExternalId))
        .thenReturn(Optional.empty()) // First call: show not found
        .thenReturn(Optional.of(syncedShow)); // Second call: show found after sync

    // Mock showSyncService to return success
    when(showSyncService.syncShow(missingShowExternalId))
        .thenReturn(SyncResult.success("Show", 1, 0));

    // Mock other dependencies
    when(segmentTypeService.findByName(anyString())).thenReturn(Optional.of(new SegmentType()));

    // When
    SyncResult result = segmentSyncService.syncSegment(matchId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    // Verify interactions
    verify(notionHandler).loadSegmentById(matchId);
    verify(showService, times(2)).findByExternalId(missingShowExternalId); // Called twice
    verify(showSyncService).syncShow(missingShowExternalId); // Called once to sync
    verify(segmentService, times(1)).updateSegment(any(Segment.class));
  }

  @Test
  @DisplayName("Should handle no segment found in Notion")
  void shouldHandleNoSegmentFoundInNotion() {
    // Given
    when(notionHandler.loadSegmentById(anyString())).thenReturn(Optional.empty());

    // When
    SyncResult result = segmentSyncService.syncSegment("non-existent-id");

    // Then
    verify(segmentService, times(0)).updateSegment(any(Segment.class));
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getSyncedCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should handle exception during Notion fetch")
  void shouldHandleExceptionDuringNotionFetch() {
    // Given
    when(notionHandler.loadSegmentById(anyString()))
        .thenThrow(new RuntimeException("Notion API error"));

    // When
    SyncResult result = segmentSyncService.syncSegment("any-id");

    // Then
    Assertions.assertFalse(result.isSuccess());
    verify(segmentService, times(0)).updateSegment(any(Segment.class));
  }
}
