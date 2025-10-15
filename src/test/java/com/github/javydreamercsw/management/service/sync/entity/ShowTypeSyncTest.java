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
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
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
  @Mock private SyncHealthMonitor healthMonitor;

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
    ReflectionTestUtils.setField(showTypeSyncService, "healthMonitor", healthMonitor);

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

    when(showTypeService.findAll()).thenReturn(Collections.emptyList());
    when(showTypeService.findByName(anyString())).thenReturn(Optional.empty());

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
    verify(showTypeService, times(1)).findAll(); // Once initially
    verify(showTypeService, times(2)).findByName(anyString()); // Once for Weekly, once for PLE
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
    verify(showTypeService, never()).save(any(ShowType.class)); // No saves on Notion failure
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
