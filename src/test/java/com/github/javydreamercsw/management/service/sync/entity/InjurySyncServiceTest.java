package com.github.javydreamercsw.management.service.sync.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.InjuryPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for InjurySyncService covering all major functionality including error handling,
 * validation, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InjurySyncServiceTest {

  @Mock private InjuryTypeService injuryTypeService;
  @Mock private InjuryTypeRepository injuryTypeRepository;
  @Mock private NotionHandler notionHandler;
  private final NotionSyncProperties syncProperties; // Declare without @Mock
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionRateLimitService rateLimitService;

  private InjurySyncService injurySyncService;

  // Constructor to configure the mock before setUp()
  public InjurySyncServiceTest() {
    syncProperties = mock(NotionSyncProperties.class); // Manually create mock
    lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    lenient().when(syncProperties.isEntityEnabled(anyString())).thenReturn(true);
  }

  @BeforeEach
  void setUp() {
    injurySyncService = new InjurySyncService(objectMapper, syncProperties);

    // Use reflection to inject mocked dependencies
    injectMockDependencies();
  }

  private void injectMockDependencies() {
    try {
      // Inject mocked dependencies using reflection
      var injuryTypeServiceField = InjurySyncService.class.getDeclaredField("injuryTypeService");
      injuryTypeServiceField.setAccessible(true);
      injuryTypeServiceField.set(injurySyncService, injuryTypeService);

      var injuryTypeRepositoryField =
          InjurySyncService.class.getDeclaredField("injuryTypeRepository");
      injuryTypeRepositoryField.setAccessible(true);
      injuryTypeRepositoryField.set(injurySyncService, injuryTypeRepository);

      // Inject base service dependencies
      var notionHandlerField =
          InjurySyncService.class.getSuperclass().getDeclaredField("notionHandler");
      notionHandlerField.setAccessible(true);
      notionHandlerField.set(injurySyncService, notionHandler);

      var progressTrackerField =
          InjurySyncService.class.getSuperclass().getDeclaredField("progressTracker");
      progressTrackerField.setAccessible(true);
      progressTrackerField.set(injurySyncService, progressTracker);

      var healthMonitorField =
          InjurySyncService.class.getSuperclass().getDeclaredField("healthMonitor");
      healthMonitorField.setAccessible(true);
      healthMonitorField.set(injurySyncService, healthMonitor);

      var rateLimitServiceField =
          InjurySyncService.class.getSuperclass().getDeclaredField("rateLimitService");
      rateLimitServiceField.setAccessible(true);
      rateLimitServiceField.set(injurySyncService, rateLimitService);
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject mock dependencies", e);
    }
  }

  @Test
  void syncInjuryTypes_WhenDisabled_ShouldReturnSuccessWithoutSync() {
    // Given
    when(syncProperties.isEntityEnabled("injuries")).thenReturn(false);

    // When
    SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals("Injuries", result.getEntityType());
    assertEquals(0, result.getSyncedCount());
    verify(notionHandler, never()).loadAllInjuries();
  }

  @Test
  void syncInjuryTypes_WhenAlreadySyncedInSession_ShouldSkip() {
    // Given - Use reflection to mark as already synced since the method is protected
    try {
      var method = BaseSyncService.class.getDeclaredMethod("markAsSyncedInSession", String.class);
      method.setAccessible(true);
      method.invoke(injurySyncService, "injury-types");
    } catch (Exception e) {
      throw new RuntimeException("Failed to mark as synced", e);
    }

    // When
    SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals(0, result.getSyncedCount());
    verify(notionHandler, never()).loadAllInjuries();
  }

  @Test
  void syncInjuryTypes_WhenSuccessful_ShouldReturnCorrectResult() {
    // Given
    try (MockedStatic<EnvironmentVariableUtil> envUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      envUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);
      when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);

      List<InjuryPage> mockPages = createMockInjuryPages();
      when(notionHandler.loadAllInjuries()).thenReturn(mockPages);
      when(injuryTypeRepository.existsByInjuryName(anyString())).thenReturn(false);
      when(injuryTypeService.createInjuryType(
              anyString(), anyInt(), anyInt(), anyInt(), anyString()))
          .thenReturn(createMockInjuryType());
      when(injuryTypeService.updateInjuryType(any())).thenReturn(createMockInjuryType());

      // When
      SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

      // Then
      assertTrue(result.isSuccess());
      assertEquals("Injuries", result.getEntityType());
      assertEquals(2, result.getSyncedCount()); // Two mock injuries

      // Verify progress tracking
      verify(progressTracker).startOperation(eq("test-operation"), eq("Sync Injuries"), eq(4));
      verify(progressTracker).completeOperation(eq("test-operation"), eq(true), anyString(), eq(2));

      // Verify health monitoring
      verify(healthMonitor).recordSuccess(eq("Injuries"), anyLong(), eq(2));
    }
  }

  @Test
  void syncInjuryTypes_WhenNoInjuriesFound_ShouldReturnSuccessWithZeroCount() {
    // Given
    try (MockedStatic<EnvironmentVariableUtil> envUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      envUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);
      when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);
      when(notionHandler.loadAllInjuries()).thenReturn(Collections.emptyList());

      // When
      SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

      // Then
      assertTrue(result.isSuccess());
      assertEquals(0, result.getSyncedCount());
      verify(progressTracker)
          .completeOperation(eq("test-operation"), eq(true), eq("No injuries to sync"), eq(0));
    }
  }

  @Test
  void syncInjuryTypes_WhenDuplicateInjuries_ShouldSkipExisting() {
    // Given
    try (MockedStatic<EnvironmentVariableUtil> envUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      envUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);
      when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);

      List<InjuryPage> mockPages = createMockInjuryPages();
      when(notionHandler.loadAllInjuries()).thenReturn(mockPages);

      // Correctly mock repository behavior
      com.github.javydreamercsw.management.domain.injury.InjuryType existingInjury =
          createMockInjuryType();
      when(injuryTypeRepository.findByInjuryName("Head Injury"))
          .thenReturn(java.util.Optional.of(existingInjury));
      when(injuryTypeRepository.findByInjuryName("Back Injury"))
          .thenReturn(java.util.Optional.empty());

      when(injuryTypeService.createInjuryType(
              eq("Back Injury"), anyInt(), anyInt(), anyInt(), anyString()))
          .thenReturn(createMockInjuryType());
      when(injuryTypeService.updateInjuryType(any())).thenReturn(createMockInjuryType());

      // When
      SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

      // Then
      assertTrue(result.isSuccess());
      assertEquals(2, result.getSyncedCount()); // Both processed

      // Verify that create is only called for the new injury
      verify(injuryTypeService, times(1))
          .createInjuryType(eq("Back Injury"), anyInt(), anyInt(), anyInt(), anyString());
      verify(injuryTypeService, never())
          .createInjuryType(eq("Head Injury"), anyInt(), anyInt(), anyInt(), anyString());
    }
  }

  @Test
  void syncInjuryTypes_WhenNotionHandlerThrowsException_ShouldReturnFailure() {
    // Given
    try (MockedStatic<EnvironmentVariableUtil> envUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      envUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);
      when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);
      when(notionHandler.loadAllInjuries()).thenThrow(new RuntimeException("Notion API error"));

      // When
      SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

      // Then
      assertFalse(result.isSuccess());
      assertEquals("Injuries", result.getEntityType());
      assertTrue(result.getErrorMessage().contains("Notion API error"));

      // Verify error handling
      verify(progressTracker).failOperation(eq("test-operation"), anyString());
      verify(healthMonitor).recordFailure(eq("Injuries"), anyString());
    }
  }

  @Test
  void syncInjuryTypes_WhenServiceThrowsException_ShouldContinueWithOtherInjuries() {
    // Given
    try (MockedStatic<EnvironmentVariableUtil> envUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      envUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);
      when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);

      List<InjuryPage> mockPages = createMockInjuryPages();
      when(notionHandler.loadAllInjuries()).thenReturn(mockPages);
      when(injuryTypeRepository.existsByInjuryName(anyString())).thenReturn(false);
      when(injuryTypeService.createInjuryType(
              eq("Head Injury"), anyInt(), anyInt(), anyInt(), anyString()))
          .thenThrow(new RuntimeException("Database error"));
      when(injuryTypeService.createInjuryType(
              eq("Back Injury"), anyInt(), anyInt(), anyInt(), anyString()))
          .thenReturn(createMockInjuryType());
      when(injuryTypeService.updateInjuryType(any())).thenReturn(createMockInjuryType());

      // When
      SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

      // Then
      assertTrue(result.isSuccess());
      assertEquals(1, result.getSyncedCount()); // One succeeded despite one failure
    }
  }

  @Test
  void syncInjuryTypes_WhenValidationFails_ShouldReturnFailure() {
    // Given
    try (MockedStatic<EnvironmentVariableUtil> envUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      envUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);
      when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);

      List<InjuryPage> mockPages = createMockInjuryPages();
      when(notionHandler.loadAllInjuries()).thenReturn(mockPages);
      when(injuryTypeRepository.existsByInjuryName(anyString())).thenReturn(false);
      // Only one injury will be created, causing low success rate
      when(injuryTypeService.createInjuryType(
              eq("Head Injury"), anyInt(), anyInt(), anyInt(), anyString()))
          .thenReturn(null); // Simulate creation failure
      when(injuryTypeService.createInjuryType(
              eq("Back Injury"), anyInt(), anyInt(), anyInt(), anyString()))
          .thenReturn(null); // Simulate creation failure

      // When
      SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

      // Then
      assertFalse(result.isSuccess());
      assertTrue(result.getErrorMessage().contains("validation failed"));
    }
  }

  @Test
  void syncInjuryTypes_WhenNotionTokenNotAvailable_ShouldReturnFailure() {
    // Given
    try (MockedStatic<EnvironmentVariableUtil> envUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      envUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(false);
      when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);

      // When
      SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

      // Then
      assertFalse(result.isSuccess());
      assertEquals("Injuries", result.getEntityType());
      assertTrue(result.getErrorMessage().contains("NOTION_TOKEN is not available"));
      verify(notionHandler, never()).loadAllInjuries();
    }
  }

  private List<InjuryPage> createMockInjuryPages() {
    InjuryPage injury1 =
        createMockInjuryPage("injury-1", "Head Injury", -2, -1, -1, "No reversal ability");
    InjuryPage injury2 =
        createMockInjuryPage("injury-2", "Back Injury", -3, -2, 0, "Reduced mobility");
    return Arrays.asList(injury1, injury2);
  }

  private InjuryPage createMockInjuryPage(
      String id, String name, int health, int stamina, int card, String special) {
    InjuryPage page = mock(InjuryPage.class);
    when(page.getId()).thenReturn(id);
    when(page.getCreated_time()).thenReturn(Instant.now().toString());
    when(page.getLast_edited_time()).thenReturn(Instant.now().toString());

    // Mock raw properties with correct structure for name extraction
    Map<String, Object> properties = new HashMap<>();
    // The Name property should be a simple string when toString() is called
    Object nameProperty =
        new Object() {
          @Override
          public String toString() {
            return name;
          }
        };
    properties.put("Name", nameProperty);
    properties.put("Health Effect", health);
    properties.put("Stamina Effect", stamina);
    properties.put("Card Effect", card);
    properties.put("Special Effects", special);
    when(page.getRawProperties()).thenReturn(properties);

    return page;
  }

  private com.github.javydreamercsw.management.domain.injury.InjuryType createMockInjuryType() {
    com.github.javydreamercsw.management.domain.injury.InjuryType injuryType =
        new com.github.javydreamercsw.management.domain.injury.InjuryType();
    injuryType.setId(1L);
    injuryType.setInjuryName("Test Injury");
    return injuryType;
  }
}
