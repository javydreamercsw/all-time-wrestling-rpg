package com.github.javydreamercsw.management.service.sync.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for WrestlerSyncService covering wrestler synchronization including stats,
 * relationships, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class WrestlerSyncServiceTest {

  @Mock private WrestlerRepository wrestlerRepository;

  @Mock private WrestlerService wrestlerService;

  @Mock private NotionHandler notionHandler;
  private final NotionSyncProperties syncProperties; // Declare without @Mock
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionRateLimitService rateLimitService;

  private WrestlerSyncService wrestlerSyncService;

  // Constructor to configure the mock before setUp()
  public WrestlerSyncServiceTest() {
    syncProperties = mock(NotionSyncProperties.class); // Manually create mock
    lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    lenient().when(syncProperties.isEntityEnabled(anyString())).thenReturn(true);
  }

  @BeforeEach
  void setUp() {
    wrestlerSyncService = new WrestlerSyncService(objectMapper, syncProperties);
    injectMockDependencies();
  }

  private void injectMockDependencies() {
    try {
      var wrestlerRepositoryField =
          WrestlerSyncService.class.getDeclaredField("wrestlerRepository");
      wrestlerRepositoryField.setAccessible(true);
      wrestlerRepositoryField.set(wrestlerSyncService, wrestlerRepository);

      var wrestlerServiceField = WrestlerSyncService.class.getDeclaredField("wrestlerService");
      wrestlerServiceField.setAccessible(true);
      wrestlerServiceField.set(wrestlerSyncService, wrestlerService);

      var notionHandlerField =
          WrestlerSyncService.class.getSuperclass().getDeclaredField("notionHandler");
      notionHandlerField.setAccessible(true);
      notionHandlerField.set(wrestlerSyncService, notionHandler);

      var progressTrackerField =
          WrestlerSyncService.class.getSuperclass().getDeclaredField("progressTracker");
      progressTrackerField.setAccessible(true);
      progressTrackerField.set(wrestlerSyncService, progressTracker);

      var healthMonitorField =
          WrestlerSyncService.class.getSuperclass().getDeclaredField("healthMonitor");
      healthMonitorField.setAccessible(true);
      healthMonitorField.set(wrestlerSyncService, healthMonitor);

      var rateLimitServiceField =
          WrestlerSyncService.class.getSuperclass().getDeclaredField("rateLimitService");
      rateLimitServiceField.setAccessible(true);
      rateLimitServiceField.set(wrestlerSyncService, rateLimitService);
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject mock dependencies", e);
    }
  }

  @Test
  void syncWrestlers_WhenSuccessful_ShouldReturnCorrectResult() {
    // Given
    List<WrestlerPage> mockPages = createMockWrestlerPages();
    when(notionHandler.loadAllWrestlers()).thenReturn(mockPages);
    when(wrestlerService.findByExternalId(anyString())).thenReturn(Optional.empty());
    when(wrestlerService.findByName(anyString())).thenReturn(Optional.empty());
    when(wrestlerService.save(any(Wrestler.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    SyncResult result = wrestlerSyncService.syncWrestlers("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals("Wrestlers", result.getEntityType());
    verify(wrestlerService, times(2)).save(any(Wrestler.class));
    verify(healthMonitor).recordSuccess(eq("Wrestlers"), anyLong(), anyInt());
  }

  @Test
  void syncWrestlers_WhenDisabled_ShouldSkipSync() {
    // Given
    when(syncProperties.isEntityEnabled("wrestlers")).thenReturn(false);

    // When
    SyncResult result = wrestlerSyncService.syncWrestlers("test-operation");

    // Then
    assertTrue(result.isSuccess());
    verify(notionHandler, never()).loadAllWrestlers();
  }

  @Test
  void syncWrestlers_WhenNoWrestlersFound_ShouldReturnSuccess() {
    // Given
    when(notionHandler.loadAllWrestlers()).thenReturn(Collections.emptyList());

    // When
    SyncResult result = wrestlerSyncService.syncWrestlers("test-operation");

    // Then
    assertTrue(result.isSuccess());
    verify(wrestlerRepository, never()).save(any(Wrestler.class));
  }

  private List<WrestlerPage> createMockWrestlerPages() {
    WrestlerPage wrestler1 = createMockWrestlerPage("wrestler-1", "John Cena", 85, 90, 95);
    WrestlerPage wrestler2 = createMockWrestlerPage("wrestler-2", "The Rock", 90, 95, 85);
    return Arrays.asList(wrestler1, wrestler2);
  }

  private WrestlerPage createMockWrestlerPage(
      String id, String name, int health, int stamina, int charisma) {
    WrestlerPage page = mock(WrestlerPage.class);
    when(page.getId()).thenReturn(id);

    Map<String, Object> properties = new HashMap<>();
    properties.put("Name", Map.of("title", List.of(Map.of("text", Map.of("content", name)))));
    properties.put("Health", health);
    properties.put("Stamina", stamina);
    properties.put("Charisma", charisma);
    when(page.getRawProperties()).thenReturn(properties);

    return page;
  }
}
