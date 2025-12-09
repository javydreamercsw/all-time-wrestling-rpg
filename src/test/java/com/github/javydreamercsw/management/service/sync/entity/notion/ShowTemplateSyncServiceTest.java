/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPageDataExtractor;
import com.github.javydreamercsw.base.ai.notion.NotionRateLimitService;
import com.github.javydreamercsw.base.ai.notion.ShowTemplatePage;
import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.SyncSessionManager;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for ShowTemplateSyncService. These tests verify the sync logic with mocked
 * dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Show Template Sync Service Unit Tests")
@Slf4j
class ShowTemplateSyncServiceTest {

  @Mock private ShowTemplateService showTemplateService;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private NotionHandler notionHandler;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private NotionRateLimitService rateLimitService;
  @Mock private ShowTypeRepository showTypeRepository;
  @Mock private SyncServiceDependencies syncServiceDependencies;
  @Mock private SyncSessionManager syncSessionManager;
  @Mock private NotionPageDataExtractor notionPageDataExtractor;
  @Mock private NotionApiExecutor notionApiExecutor;

  private ShowTemplateSyncService syncService;

  private MockedStatic<EnvironmentVariableUtil> mockedEnvironmentVariableUtil;
  private ExecutorService executorService;

  @BeforeEach
  void setUp() {
    executorService = Executors.newSingleThreadExecutor();
    mockedEnvironmentVariableUtil = mockStatic(EnvironmentVariableUtil.class);

    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::isNotionTokenAvailable)
        .thenReturn(true);

    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::getNotionToken)
        .thenReturn("test-token");

    lenient().when(syncServiceDependencies.getNotionSyncProperties()).thenReturn(syncProperties);
    lenient().when(syncServiceDependencies.getProgressTracker()).thenReturn(progressTracker);
    lenient().when(syncServiceDependencies.getHealthMonitor()).thenReturn(healthMonitor);
    lenient().when(syncServiceDependencies.getNotionHandler()).thenReturn(notionHandler);
    lenient().when(syncServiceDependencies.getShowTypeRepository()).thenReturn(showTypeRepository);
    lenient().when(syncServiceDependencies.getSyncSessionManager()).thenReturn(syncSessionManager);
    lenient().when(syncServiceDependencies.getRateLimitService()).thenReturn(rateLimitService);
    lenient()
        .when(syncServiceDependencies.getNotionPageDataExtractor())
        .thenReturn(notionPageDataExtractor);
    lenient().when(notionApiExecutor.getSyncExecutorService()).thenReturn(executorService);

    syncService =
        new ShowTemplateSyncService(
            objectMapper, syncServiceDependencies, showTemplateService, notionApiExecutor);

    // Mock repository calls to simulate existing show types

    ShowType weekly = new ShowType();

    ReflectionTestUtils.setField(weekly, "id", 1L);

    weekly.setName("Weekly");

    lenient().when(showTypeRepository.findByName("Weekly")).thenReturn(Optional.of(weekly));

    ShowType ple = new ShowType();

    ReflectionTestUtils.setField(ple, "id", 2L);

    ple.setName("Premium Live Event (PLE)");

    lenient()
        .when(showTypeRepository.findByName("Premium Live Event (PLE)"))
        .thenReturn(Optional.of(ple));
  }

  @AfterEach
  void tearDown() {
    mockedEnvironmentVariableUtil.close();
  }

  @Test
  @DisplayName("Should skip sync when entity is disabled")
  void shouldSkipSyncWhenEntityIsDisabled() {
    // Given
    when(syncProperties.isEntityEnabled("templates")).thenReturn(false);

    // When
    BaseSyncService.SyncResult result = syncService.syncShowTemplates("test-operation");

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(0);

    verify(syncProperties).isEntityEnabled("templates");
    verifyNoInteractions(showTemplateService);
  }

  @Test
  @DisplayName("Should fail when NotionHandler is not available")
  void shouldFailWhenNotionHandlerNotAvailable() {
    // Given
    when(syncProperties.isEntityEnabled("templates")).thenReturn(true);
    doReturn(null).when(syncServiceDependencies).getNotionHandler();

    // When
    BaseSyncService.SyncResult result = syncService.syncShowTemplates("test-operation");

    // Then
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("NotionHandler is not available");
  }

  @Test
  @DisplayName("Should handle empty template list gracefully")
  void shouldHandleEmptyTemplateListGracefully() {
    // Given
    when(syncProperties.isEntityEnabled("templates")).thenReturn(true);
    when(notionHandler.loadAllShowTemplates()).thenReturn(Arrays.asList());

    // When
    BaseSyncService.SyncResult result = syncService.syncShowTemplates("test-operation");

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(0);
    assertThat(result.getEntityType()).isEqualTo("Show Templates");

    verify(notionHandler).loadAllShowTemplates();
  }

  @Test
  @DisplayName("Should handle exceptions during sync gracefully")
  void shouldHandleExceptionsDuringSyncGracefully() {
    // Given
    when(syncProperties.isEntityEnabled("templates")).thenReturn(true);
    when(notionHandler.loadAllShowTemplates()).thenThrow(new RuntimeException("Notion API error"));

    // When
    BaseSyncService.SyncResult result = syncService.syncShowTemplates("test-operation");

    // Then
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("Notion API error");
  }

  @Test
  @DisplayName("Should track progress during sync operation")
  void shouldTrackProgressDuringSyncOperation() {
    // Given
    lenient().when(syncProperties.isEntityEnabled("templates")).thenReturn(true);
    // ReflectionTestUtils.setField(syncService, "notionHandler", notionHandler); // Redundant
    when(notionHandler.loadAllShowTemplates()).thenReturn(Arrays.asList());

    // When
    syncService.syncShowTemplates("test-operation");

    // Then - Verify progress tracking calls
    verify(progressTracker).startOperation("test-operation", "Sync Show Templates", 3);
    verify(progressTracker)
        .updateProgress(
            eq("test-operation"), eq(1), eq("Retrieving show templates from Notion..."));
    verify(progressTracker)
        .completeOperation(eq("test-operation"), eq(true), eq("No show templates to sync"), eq(0));
  }

  @Test
  @DisplayName("Should process templates with valid show type mappings")
  void shouldProcessTemplatesWithValidShowTypeMappings() {
    // Given
    when(syncProperties.isEntityEnabled("templates")).thenReturn(true);

    // Create a simple mock template page
    ShowTemplatePage templatePage = mock(ShowTemplatePage.class);
    when(templatePage.getId()).thenReturn("template-1");

    // Create basic property map with simple string values
    Map<String, Object> properties = new HashMap<>();
    properties.put("Name", "Test Template");
    properties.put("Description", "Test Description");
    when(templatePage.getRawProperties()).thenReturn(properties);

    when(notionHandler.loadAllShowTemplates()).thenReturn(Arrays.asList(templatePage));

    // When
    BaseSyncService.SyncResult result = syncService.syncShowTemplates("test-operation");

    // Then - Should complete without errors
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("Show Templates");

    verify(notionHandler).loadAllShowTemplates();
  }

  @Test
  @DisplayName("Should skip sync when already synced in current session")
  void shouldSkipSyncWhenAlreadySyncedInCurrentSession() {
    // Given
    when(syncProperties.isEntityEnabled("templates")).thenReturn(true);
    when(notionHandler.loadAllShowTemplates()).thenReturn(List.of());

    // First sync
    syncService.syncShowTemplates("first-operation");

    // When - Second sync in same session
    BaseSyncService.SyncResult result = syncService.syncShowTemplates("second-operation");

    // Then - Should be skipped
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(0);

    // Verify notionHandler was only called once
    verify(notionHandler, times(2)).loadAllShowTemplates();
  }

  @Test
  @DisplayName("Should create DTOs with show type intelligence")
  void shouldCreateDTOsWithShowTypeIntelligence() {
    // Given
    when(syncProperties.isEntityEnabled("templates")).thenReturn(true);

    // Create mock templates that will trigger show type intelligence
    ShowTemplatePage weeklyTemplate = createSimpleMockPage("weekly-1", "RAW Template");
    ShowTemplatePage pleTemplate = createSimpleMockPage("ple-1", "WrestleMania Template");

    when(notionHandler.loadAllShowTemplates())
        .thenReturn(Arrays.asList(weeklyTemplate, pleTemplate));

    // When
    BaseSyncService.SyncResult result = syncService.syncShowTemplates("test-operation");

    // Then - Should process templates
    assertThat(result.isSuccess()).isTrue();
    verify(notionHandler).loadAllShowTemplates();
  }

  @Test
  @DisplayName("Should sync mix of Weekly and PLE show templates with correct type mapping")
  void shouldSyncMixOfWeeklyAndPLEShowTemplatesWithCorrectTypeMapping() {
    // Given
    when(syncProperties.isEntityEnabled("templates")).thenReturn(true);

    // Create a realistic mix of show templates with show types from Notion
    ShowTemplatePage mondayNightRaw =
        createMockPageWithShowType("raw-1", "Monday Night RAW", "Weekly");
    ShowTemplatePage fridaySmackdown =
        createMockPageWithShowType("sd-1", "Friday Night SmackDown", "Weekly");
    ShowTemplatePage nxtShow = createMockPageWithShowType("nxt-1", "NXT Weekly", "Weekly");
    ShowTemplatePage wrestleMania =
        createMockPageWithShowType("wm-1", "WrestleMania 40", "Premium Live Event (PLE)");
    ShowTemplatePage summerSlam =
        createMockPageWithShowType("ss-1", "SummerSlam", "Premium Live Event (PLE)");
    ShowTemplatePage royalRumble =
        createMockPageWithShowType("rr-1", "Royal Rumble", "Premium Live Event (PLE)");
    ShowTemplatePage aewRevolution =
        createMockPageWithShowType("rev-1", "AEW Revolution", "Premium Live Event (PLE)");
    ShowTemplatePage aewDynamite = createMockPageWithShowType("dyn-1", "AEW Dynamite", "Weekly");

    List<ShowTemplatePage> mixedTemplates =
        Arrays.asList(
            mondayNightRaw,
            fridaySmackdown,
            nxtShow,
            wrestleMania,
            summerSlam,
            royalRumble,
            aewRevolution,
            aewDynamite);
    when(notionHandler.loadAllShowTemplates()).thenReturn(mixedTemplates);

    // Mock successful saves for both Weekly and PLE templates
    when(showTemplateService.save(any(ShowTemplate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    BaseSyncService.SyncResult result = syncService.syncShowTemplates("test-operation");

    // Then - Should successfully sync mixed show types
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(8); // All 8 templates should be processed
    assertThat(result.getEntityType()).isEqualTo("Show Templates");

    // Verify Weekly show templates were created correctly
    verify(showTemplateService, times(8)).save(any());

    // Verify health monitoring recorded the mixed sync
    verify(healthMonitor).recordSuccess(eq("Show Templates"), anyLong(), eq(8));

    log.info("✅ Mixed Weekly and PLE show templates sync verified successfully");
  }

  @Test
  @DisplayName("Should handle mix with some undetermined show types")
  void shouldHandleMixWithSomeUndeterminedShowTypes() {
    // Given
    when(syncProperties.isEntityEnabled("templates")).thenReturn(true);

    // Create a mix including some templates without show types (simulating missing Notion data)
    ShowTemplatePage weeklyTemplate =
        createMockPageWithShowType("raw-1", "Monday Night RAW", "Weekly");
    ShowTemplatePage pleTemplate =
        createMockPageWithShowType("wm-1", "WrestleMania", "Premium Live Event (PLE)");
    ShowTemplatePage noShowTypeTemplate1 =
        createSimpleMockPage("amb-1", "Custom Event Template"); // No show type
    ShowTemplatePage noShowTypeTemplate2 =
        createSimpleMockPage("amb-2", "Generic Show Format"); // No show type
    ShowTemplatePage anotherWeeklyTemplate =
        createMockPageWithShowType("sd-1", "SmackDown", "Weekly");

    List<ShowTemplatePage> mixedTemplates =
        Arrays.asList(
            weeklyTemplate,
            pleTemplate,
            noShowTypeTemplate1,
            noShowTypeTemplate2,
            anotherWeeklyTemplate);
    when(notionHandler.loadAllShowTemplates()).thenReturn(mixedTemplates);

    // When
    BaseSyncService.SyncResult result = syncService.syncShowTemplates("test-operation");

    // Then - Should sync only templates with show types
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(3); // Only 3 should be saved (2 Weekly + 1 PLE)

    // Verify only templates with show types were processed
    verify(showTemplateService, times(3)).save(any());

    // Verify templates without show types were NOT processed (they should be skipped completely)
    verify(showTemplateService, never())
        .createOrUpdateTemplate(eq("Custom Event Template"), anyString(), anyString(), isNull());
    verify(showTemplateService, never())
        .createOrUpdateTemplate(eq("Generic Show Format"), anyString(), anyString(), isNull());

    log.info("✅ Mixed sync with undetermined show types handled correctly");
  }

  private ShowTemplatePage createSimpleMockPage(String id, String name) {
    ShowTemplatePage page = mock(ShowTemplatePage.class);
    when(page.getId()).thenReturn(id);

    Map<String, Object> properties = new HashMap<>();
    properties.put("Name", name);
    properties.put("Description", "Test description for " + name);
    when(page.getRawProperties()).thenReturn(properties);

    return page;
  }

  private ShowTemplatePage createMockPageWithShowType(String id, String name, String showType) {
    ShowTemplatePage page = mock(ShowTemplatePage.class);
    when(page.getId()).thenReturn(id);

    Map<String, Object> properties = new HashMap<>();
    properties.put("Name", name);
    properties.put("Description", "Test description for " + name);
    properties.put("Show Type", showType); // This is the key - providing the show type from Notion
    when(page.getRawProperties()).thenReturn(properties);

    return page;
  }
}
