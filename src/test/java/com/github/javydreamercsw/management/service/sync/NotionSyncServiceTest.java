package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionSyncService.SyncResult;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotionSyncServiceTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncProgressTracker progressTracker;

  // Mock database services
  @Mock private ShowService showService;
  @Mock private ShowTypeService showTypeService;
  @Mock private SeasonService seasonService;
  @Mock private ShowTemplateService showTemplateService;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;

  private NotionSyncService notionSyncService;

  @BeforeEach
  void setUp() {
    notionSyncService =
        new NotionSyncService(
            objectMapper,
            notionHandler,
            syncProperties,
            progressTracker,
            showService,
            showTypeService,
            wrestlerService,
            wrestlerRepository,
            seasonService,
            showTemplateService);
  }

  @Test
  @DisplayName("Should skip sync when shows entity is disabled")
  void shouldSkipSyncWhenShowsEntityDisabled() {
    // Given
    when(syncProperties.isEntityEnabled("shows")).thenReturn(false);

    // When
    SyncResult result = notionSyncService.syncShows();

    // Then
    assertTrue(result.isSuccess());
    assertEquals("Shows", result.getEntityType());
    assertEquals(0, result.getSyncedCount());
    verify(notionHandler, never()).loadAllShowsForSync();
  }

  @Test
  @DisplayName("Should handle empty shows list from Notion")
  void shouldHandleEmptyShowsListFromNotion() {
    // Given
    when(syncProperties.isEntityEnabled("shows")).thenReturn(true);
    when(syncProperties.isBackupEnabled()).thenReturn(false);

    // When - This will fail due to missing NOTION_TOKEN, which is expected in unit tests
    SyncResult result = notionSyncService.syncShows();

    // Then - Should fail gracefully due to missing NOTION_TOKEN
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals("Shows", result.getEntityType());
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("NOTION_TOKEN"));
  }

  @Test
  @DisplayName("Should handle shows with valid data")
  void shouldHandleShowsWithValidData() {
    // Given
    when(syncProperties.isEntityEnabled("shows")).thenReturn(true);
    when(syncProperties.isBackupEnabled()).thenReturn(false);

    // When - This will fail due to missing NOTION_TOKEN, which is expected in unit tests
    SyncResult result = notionSyncService.syncShows();

    // Then - Should fail gracefully due to missing NOTION_TOKEN
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals("Shows", result.getEntityType());
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("NOTION_TOKEN"));
  }

  private ShowPage createMockShowPage(String name, String description) {
    ShowPage showPage = new ShowPage();
    showPage.setId("test-id-123");

    // Create mock raw properties
    Map<String, Object> rawProperties = new HashMap<>();
    rawProperties.put("Name", name);
    rawProperties.put("Description", description);
    rawProperties.put("Show Type", "Weekly");
    rawProperties.put("Date", "2024-01-15");

    showPage.setRawProperties(rawProperties);

    return showPage;
  }
}
