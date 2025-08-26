package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.InjuryPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for injury sync functionality in NotionSyncService. Uses sample injury data from real
 * Notion database structure.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Injury Sync Unit Tests")
class InjurySyncTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private InjuryTypeService injuryTypeService;
  @Mock private InjuryTypeRepository injuryTypeRepository;

  // Mock other required dependencies
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private RetryService retryService;
  @Mock private CircuitBreakerService circuitBreakerService;
  @Mock private SyncValidationService validationService;
  @Mock private SyncTransactionManager syncTransactionManager;
  @Mock private DataIntegrityChecker integrityChecker;

  private NotionSyncService notionSyncService;
  private ObjectMapper realObjectMapper;

  @BeforeEach
  void setUp() {
    // Create the service with simplified constructor
    notionSyncService = new NotionSyncService(objectMapper, syncProperties);

    // Manually inject the mocked dependencies using reflection
    setField(notionSyncService, "notionHandler", notionHandler);
    setField(notionSyncService, "progressTracker", progressTracker);
    setField(notionSyncService, "healthMonitor", healthMonitor);
    setField(notionSyncService, "retryService", retryService);
    setField(notionSyncService, "circuitBreakerService", circuitBreakerService);
    setField(notionSyncService, "validationService", validationService);
    setField(notionSyncService, "syncTransactionManager", syncTransactionManager);
    setField(notionSyncService, "integrityChecker", integrityChecker);
    setField(notionSyncService, "injuryTypeService", injuryTypeService);
    setField(notionSyncService, "injuryTypeRepository", injuryTypeRepository);

    // Create real ObjectMapper for JSON parsing
    realObjectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("Should sync injury types from Notion sample data successfully")
  void shouldSyncInjuryTypesFromNotionSampleDataSuccessfully() throws Exception {
    // Mock NOTION_TOKEN as available
    try (MockedStatic<EnvironmentVariableUtil> mockedUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      mockedUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);

      // Given
      List<InjuryPage> sampleInjuries = loadSampleInjuries();
      when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);
      when(notionHandler.loadAllInjuries()).thenReturn(sampleInjuries);

      // Mock no existing injury types (all new)
      when(injuryTypeRepository.findByExternalId(anyString())).thenReturn(Optional.empty());

      // Mock injury type creation
      when(injuryTypeService.createInjuryType(anyString(), any(), any(), any(), anyString()))
          .thenAnswer(
              invocation -> {
                InjuryType injuryType = new InjuryType();
                injuryType.setId(1L);
                injuryType.setInjuryName(invocation.getArgument(0));
                injuryType.setHealthEffect(invocation.getArgument(1));
                injuryType.setStaminaEffect(invocation.getArgument(2));
                injuryType.setCardEffect(invocation.getArgument(3));
                injuryType.setSpecialEffects(invocation.getArgument(4));
                return injuryType;
              });

      when(injuryTypeRepository.saveAndFlush(any(InjuryType.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // When
      NotionSyncService.SyncResult result = notionSyncService.syncInjuryTypes("test-operation-id");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getEntityType()).isEqualTo("Injuries");
      assertThat(result.getSyncedCount()).isEqualTo(3); // 3 sample injuries
      assertThat(result.getErrorCount()).isEqualTo(0);

      // Verify interactions
      verify(notionHandler).loadAllInjuries();

      // Verify all 3 injury types were created
      verify(injuryTypeService, times(3))
          .createInjuryType(anyString(), any(), any(), any(), anyString());
      verify(injuryTypeRepository, times(3)).saveAndFlush(any(InjuryType.class));
    }
  }

  @Test
  @DisplayName("Should handle empty injury list from Notion")
  void shouldHandleEmptyInjuryListFromNotion() {
    // Mock NOTION_TOKEN as available
    try (MockedStatic<EnvironmentVariableUtil> mockedUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      mockedUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);

      // Given
      when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);
      when(notionHandler.loadAllInjuries()).thenReturn(List.of());

      // When
      NotionSyncService.SyncResult result = notionSyncService.syncInjuryTypes("test-operation-id");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getEntityType()).isEqualTo("Injuries");
      assertThat(result.getSyncedCount()).isEqualTo(0);
      assertThat(result.getErrorCount()).isEqualTo(0);

      verify(injuryTypeService, never())
          .createInjuryType(anyString(), any(), any(), any(), anyString());
    }
  }

  @Test
  @DisplayName("Should update existing injury types instead of creating duplicates")
  void shouldUpdateExistingInjuryTypesInsteadOfCreatingDuplicates() throws Exception {
    // Given
    List<InjuryPage> sampleInjuries = loadSampleInjuries();
    when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);
    when(notionHandler.loadAllInjuries()).thenReturn(sampleInjuries);

    // Note: Injury sync doesn't use progress tracking

    // Mock existing injury type for "Head injury"
    InjuryType existingHeadInjury = new InjuryType();
    existingHeadInjury.setId(1L);
    existingHeadInjury.setInjuryName("Head injury");
    existingHeadInjury.setExternalId("22890edc-c30f-80ae-8692-ca9f045b7c01");

    when(injuryTypeRepository.findByExternalId("22890edc-c30f-80ae-8692-ca9f045b7c01"))
        .thenReturn(Optional.of(existingHeadInjury));
    when(injuryTypeRepository.findByExternalId("22890edc-c30f-80db-a037-c7c1a610d1db"))
        .thenReturn(Optional.empty());
    when(injuryTypeRepository.findByExternalId("22890edc-c30f-80fe-a8ac-c02bb9299825"))
        .thenReturn(Optional.empty());

    when(injuryTypeRepository.saveAndFlush(any(InjuryType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Mock creation for new injury types
    when(injuryTypeService.createInjuryType(anyString(), any(), any(), any(), anyString()))
        .thenAnswer(
            invocation -> {
              InjuryType injuryType = new InjuryType();
              injuryType.setId(2L);
              injuryType.setInjuryName(invocation.getArgument(0));
              return injuryType;
            });

    // When
    NotionSyncService.SyncResult result = notionSyncService.syncInjuryTypes("test-operation-id");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(3); // 1 updated + 2 created

    // Verify existing injury was updated (saveAndFlush called 3 times total)
    verify(injuryTypeRepository, times(3)).saveAndFlush(any(InjuryType.class));
    // Verify only 2 new injury types were created (not the existing one)
    verify(injuryTypeService, times(2))
        .createInjuryType(anyString(), any(), any(), any(), anyString());
  }

  /** Load sample injury data from JSON files. */
  private List<InjuryPage> loadSampleInjuries() throws Exception {
    File samplesDir = new File("src/test/resources/notion-samples");

    InjuryPage injury1 =
        realObjectMapper.readValue(new File(samplesDir, "real-injury-1.json"), InjuryPage.class);
    InjuryPage injury2 =
        realObjectMapper.readValue(new File(samplesDir, "real-injury-2.json"), InjuryPage.class);
    InjuryPage injury3 =
        realObjectMapper.readValue(new File(samplesDir, "real-injury-3.json"), InjuryPage.class);

    return Arrays.asList(injury1, injury2, injury3);
  }

  @Test
  @DisplayName("Should handle injury sync when disabled in configuration")
  void shouldHandleInjurySyncWhenDisabledInConfiguration() {
    // Given
    when(syncProperties.isEntityEnabled("injuries")).thenReturn(false);

    // When
    NotionSyncService.SyncResult result = notionSyncService.syncInjuryTypes("test-operation-id");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("Injuries");
    assertThat(result.getSyncedCount()).isEqualTo(0);
    assertThat(result.getErrorCount()).isEqualTo(0);
    assertThat(result.getErrorMessage()).isNull(); // No error for disabled sync

    // Verify no Notion API calls were made
    verify(notionHandler, never()).loadAllInjuries();
    verify(injuryTypeService, never())
        .createInjuryType(anyString(), any(), any(), any(), anyString());
  }

  @Test
  @DisplayName("Should handle injury with missing required fields gracefully")
  void shouldHandleInjuryWithMissingRequiredFieldsGracefully() throws Exception {
    // Given
    InjuryPage invalidInjury = createInvalidInjuryPage();
    when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);
    when(notionHandler.loadAllInjuries()).thenReturn(List.of(invalidInjury));

    // Note: Injury sync doesn't use progress tracking

    // When
    NotionSyncService.SyncResult result = notionSyncService.syncInjuryTypes("test-operation-id");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue(); // Should still succeed overall
    assertThat(result.getSyncedCount()).isEqualTo(0); // But no injuries synced

    // Verify no injury types were created due to invalid data
    verify(injuryTypeService, never())
        .createInjuryType(anyString(), any(), any(), any(), anyString());
  }

  @Test
  @DisplayName("Should handle service exceptions during injury creation")
  void shouldHandleServiceExceptionsDuringInjuryCreation() throws Exception {
    // Given
    List<InjuryPage> sampleInjuries = loadSampleInjuries();
    when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);
    when(notionHandler.loadAllInjuries()).thenReturn(sampleInjuries);

    // Note: Injury sync doesn't use progress tracking

    when(injuryTypeRepository.findByExternalId(anyString())).thenReturn(Optional.empty());

    // Mock service to throw exception
    when(injuryTypeService.createInjuryType(anyString(), any(), any(), any(), anyString()))
        .thenThrow(new RuntimeException("Database connection failed"));

    // When
    NotionSyncService.SyncResult result = notionSyncService.syncInjuryTypes("test-operation-id");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue(); // Should handle exceptions gracefully
    assertThat(result.getSyncedCount()).isEqualTo(0); // No injuries created due to exceptions

    // Note: Injury sync doesn't use progress tracking
  }

  /** Create an invalid injury page with missing required fields. */
  private InjuryPage createInvalidInjuryPage() {
    InjuryPage invalidInjury = new InjuryPage();
    invalidInjury.setId(null); // Missing external ID
    invalidInjury.setRawProperties(
        Map.of(
            "Injury Name",
            "", // Empty injury name
            "Health Effect",
            "-1",
            "Stamina Effect",
            "0",
            "Card Effect",
            "0",
            "Special Effects",
            "N/A"));
    return invalidInjury;
  }

  /**
   * Helper method to set private fields via reflection for testing. This is needed because we
   * switched from constructor injection to field injection.
   */
  private void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }
}
