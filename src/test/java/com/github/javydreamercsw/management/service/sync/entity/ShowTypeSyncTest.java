package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for ShowTypeSyncService. These tests verify the service's logic in isolation, mocking
 * external dependencies like NotionHandler and repositories.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Show Type Sync Unit Tests")
class ShowTypeSyncTest {

  private ShowTypeSyncService showTypeSyncService;
  @Mock private NotionHandler notionHandler;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionSyncProperties notionSyncProperties;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private ShowTypeService showTypeService;

  private static final String TEST_OPERATION_ID = "unit-test-show-types";

  private ShowType weekly;
  private ShowType ple;

  @Mock private NotionRateLimitService rateLimitService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    notionSyncProperties = new NotionSyncProperties();
    // Manually instantiate ShowTypeSyncService with mocked constructor dependencies
    showTypeSyncService = new ShowTypeSyncService(objectMapper, notionSyncProperties);

    // Manually inject mocked SyncProgressTracker
    ReflectionTestUtils.setField(showTypeSyncService, "progressTracker", progressTracker);
    ReflectionTestUtils.setField(showTypeSyncService, "showTypeService", showTypeService);
    ReflectionTestUtils.setField(showTypeSyncService, "notionHandler", notionHandler);
    ReflectionTestUtils.setField(showTypeSyncService, "rateLimitService", rateLimitService);

    weekly = new ShowType();
    weekly.setName("Weekly");
    weekly.setDescription("Weekly television show format");

    ple = new ShowType();
    ple.setName("Premium Live Event (PLE)");
    ple.setDescription("Premium live event or pay-per-view format");
  }

  @Test
  @DisplayName("Should ensure default show types exist in database")
  void shouldEnsureDefaultShowTypesExist() {
    // Given - Setup mock behavior for ShowTypeService

    when(showTypeService.findAll()).thenReturn(Collections.emptyList(), Arrays.asList(weekly, ple));
    when(showTypeService.findByName("Weekly")).thenReturn(Optional.of(weekly));
    when(showTypeService.findByName("Premium Live Event (PLE)")).thenReturn(Optional.of(ple));

    // When - Sync show types (should create defaults when no Notion data available)
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(TEST_OPERATION_ID);

    // Then - Should have created default show types
    assertNotNull(result, "Sync result should not be null");
    assertTrue(result.isSuccess(), "Sync should be successful");
    assertThat(result.getCreatedCount()).isEqualTo(2);
    assertThat(result.getUpdatedCount()).isEqualTo(0);

    verify(showTypeService, times(1)).save(argThat(st -> st.getName().equals("Weekly")));
    verify(showTypeService, times(1))
        .save(argThat(st -> st.getName().equals("Premium Live Event (PLE)")));
    verify(showTypeService, times(2)).findAll(); // Once initially, once after sync
    verify(showTypeService, times(2)).findByName(anyString()); // Once for Weekly, once for PLE
  }

  @Test
  @DisplayName("Should not duplicate show types on subsequent syncs")
  void shouldNotDuplicateShowTypesOnSubsequentSyncs() {

    when(showTypeService.findByName("Weekly")).thenReturn(Optional.of(weekly));
    when(showTypeService.findByName("Premium Live Event (PLE)")).thenReturn(Optional.of(ple));

    // When - Run sync
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(TEST_OPERATION_ID);

    // Then - Should not create duplicates
    assertNotNull(result, "Sync result should not be null");
    assertTrue(result.isSuccess(), "Sync should be successful");
    assertThat(result.getCreatedCount()).isEqualTo(0);
    assertThat(result.getUpdatedCount())
        .isEqualTo(2); // Existing types are "updated" by saving them again

    verify(showTypeService, times(1)).save(argThat(st -> st.getName().equals("Weekly")));
    verify(showTypeService, times(1))
        .save(argThat(st -> st.getName().equals("Premium Live Event (PLE)")));
    verify(showTypeService, times(1)).findAll();
    verify(showTypeService, times(2)).findByName(anyString());
  }

  @Test
  @DisplayName("Should handle sync when show types already exist")
  void shouldHandleSyncWhenShowTypesAlreadyExist() {
    // Given - Manually create a show type
    ShowType existingType = new ShowType();
    existingType.setName("Weekly");
    existingType.setDescription("Pre-existing weekly show type");

    when(showTypeService.findAll()).thenReturn(Arrays.asList(existingType));
    when(showTypeService.findByName("Weekly")).thenReturn(Optional.of(existingType));

    // When - Run sync
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(TEST_OPERATION_ID);

    // Then - Should not overwrite existing show type, but update it
    assertNotNull(result, "Sync result should not be null");
    assertTrue(result.isSuccess(), "Sync should be successful");
    assertThat(result.getCreatedCount()).isEqualTo(0);
    assertThat(result.getUpdatedCount())
        .isEqualTo(1); // Existing type is "updated" by saving it again

    verify(showTypeService, times(1)).save(argThat(st -> st.getName().equals("Weekly")));
    verify(showTypeService, times(1)).findAll();
    verify(showTypeService, times(1)).findByName("Weekly");
  }

  @Test
  @DisplayName("Should handle sync failures gracefully")
  void shouldHandleSyncFailuresGracefully() {
    // Given - Simulate a failure during Notion interaction
    when(notionHandler.loadAllShowsForSync()).thenThrow(new RuntimeException("Notion API error"));

    // When - Attempt sync
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(TEST_OPERATION_ID);

    // Then - Should handle gracefully and report failure
    assertNotNull(result, "Sync result should not be null");
    assertFalse(result.isSuccess(), "Sync should fail");
    assertThat(result.getEntityType()).isEqualTo("Show Types");
    assertThat(result.getErrorMessage()).contains("Notion API error");

    verify(notionHandler, times(1)).loadAllShowsForSync();
    verify(showTypeService, times(1)).findAll(); // Called to check for defaults
    verify(showTypeService, never()).save(any(ShowType.class)); // No saves on Notion failure
  }

  @Test
  @DisplayName("Should track sync progress correctly")
  void shouldTrackSyncProgressCorrectly() {
    // Given - No specific Notion data, so defaults are created

    when(showTypeService.findAll()).thenReturn(Collections.emptyList(), Arrays.asList(weekly, ple));
    when(showTypeService.findByName("Weekly")).thenReturn(Optional.of(weekly));
    when(showTypeService.findByName("Premium Live Event (PLE)")).thenReturn(Optional.of(ple));

    // When - Run sync with operation ID for progress tracking
    String operationId = TEST_OPERATION_ID + "-progress";
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(operationId);

    // Then - Should complete operation tracking
    assertNotNull(result, "Sync result should not be null");
    assertTrue(result.isSuccess(), "Sync should be successful");
    assertThat(result.getSyncedCount()).isEqualTo(2);

    verify(showTypeService, times(1)).save(argThat(st -> st.getName().equals("Weekly")));
    verify(showTypeService, times(1))
        .save(argThat(st -> st.getName().equals("Premium Live Event (PLE)")));
  }

  @Test
  @DisplayName("Should sync show types from Notion when data is available")
  void shouldSyncShowTypesFromNotionWhenDataAvailable() {
    // Given - Notion returns some show types
    ShowPage notionShow1 = new ShowPage();
    Map<String, Object> props1 = new HashMap<>();
    props1.put("Show Type", "Notion Weekly");
    notionShow1.setRawProperties(props1);

    ShowPage notionShow2 = new ShowPage();
    Map<String, Object> props2 = new HashMap<>();
    props2.put("Show Type", "Notion PPV");
    notionShow2.setRawProperties(props2);

    when(notionHandler.loadAllShowsForSync()).thenReturn(Arrays.asList(notionShow1, notionShow2));

    // And - No existing show types in DB initially
    when(showTypeService.findAll()).thenReturn(Collections.emptyList());
    when(showTypeService.findByName("Notion Weekly")).thenReturn(Optional.empty());
    when(showTypeService.findByName("Notion PPV")).thenReturn(Optional.empty());

    // When - Sync with Notion data
    BaseSyncService.SyncResult result =
        showTypeSyncService.syncShowTypes(TEST_OPERATION_ID + "-notion");

    // Then - Should create show types from Notion
    assertNotNull(result, "Sync result should not be null");
    assertTrue(result.isSuccess(), "Sync should be successful");
    assertThat(result.getCreatedCount()).isEqualTo(2);
    assertThat(result.getUpdatedCount()).isEqualTo(0);

    verify(showTypeService, times(1)).save(argThat(st -> st.getName().equals("Notion Weekly")));
    verify(showTypeService, times(1)).save(argThat(st -> st.getName().equals("Notion PPV")));
    verify(notionHandler, times(1)).loadAllShowsForSync();
  }
}
