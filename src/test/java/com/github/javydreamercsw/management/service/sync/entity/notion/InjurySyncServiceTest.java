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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.InjuryPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * Unit tests for InjurySyncService covering all major functionality including error handling,
 * validation, and edge cases.
 */
class InjurySyncServiceTest extends AbstractSyncTest {

  @Mock private InjuryTypeService injuryTypeService;

  private InjurySyncService injurySyncService;
  private ObjectMapper realObjectMapper;
  private InjuryPage notionPage1;
  private InjuryPage notionPage2;
  private InjuryPage notionPage3;

  @BeforeEach
  protected void setUp() {
    super.setUp();
    injurySyncService =
        new InjurySyncService(
            objectMapper, syncServiceDependencies, injuryTypeService, notionApiExecutor);
    realObjectMapper = new ObjectMapper();
    loadNotionPages();
  }

  private void loadNotionPages() {
    File samplesDir = new File("src/test/resources/notion-samples");
    try {
      notionPage1 =
          realObjectMapper.readValue(new File(samplesDir, "real-injury-1.json"), InjuryPage.class);
      notionPage2 =
          realObjectMapper.readValue(new File(samplesDir, "real-injury-2.json"), InjuryPage.class);
      notionPage3 =
          realObjectMapper.readValue(new File(samplesDir, "real-injury-3.json"), InjuryPage.class);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load Notion sample injury pages", e);
    }
  }

  @Test
  void syncInjuryTypes_WhenDisabled_ShouldReturnSuccessWithoutSync() {
    // Given
    when(syncProperties.isEntityEnabled(SyncEntityType.INJURIES.getKey())).thenReturn(false);

    // When
    SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals(SyncEntityType.INJURIES.getKey(), result.getEntityType());
    assertEquals(0, result.getSyncedCount());
    verify(notionHandler, never()).loadAllInjuries();
  }

  @Test
  void syncInjuryTypes_WhenAlreadySyncedInSession_ShouldSkip() {
    // Given - Use reflection to mark as already synced since the method is protected
    when(syncServiceDependencies
            .getSyncSessionManager()
            .isAlreadySyncedInSession(SyncEntityType.INJURIES.getKey()))
        .thenReturn(true);

    // When
    SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals(0, result.getSyncedCount());
    verify(notionHandler, never()).loadAllInjuries();
  }

  @Test
  @DisplayName("Should sync injury types from Notion sample data successfully")
  void shouldSyncInjuryTypesFromNotionSampleDataSuccessfully() throws Exception {
    List<InjuryPage> sampleInjuries = loadSampleInjuries();
    when(syncProperties.isEntityEnabled(SyncEntityType.INJURIES.getKey())).thenReturn(true);
    when(syncServiceDependencies.getNotionHandler().loadAllInjuries()).thenReturn(sampleInjuries);

    when(notionPageDataExtractor.extractNameFromNotionPage(any(InjuryPage.class)))
        .thenAnswer(
            invocation -> {
              InjuryPage page = invocation.getArgument(0);
              Map<String, Object> props = page.getRawProperties();
              if (props != null && props.containsKey("Name")) {
                Map<String, Object> nameProp = (Map<String, Object>) props.get("Name");
                if (nameProp != null && nameProp.containsKey("title")) {
                  List<Map<String, Object>> titleList =
                      (List<Map<String, Object>>) nameProp.get("title");
                  if (titleList != null && !titleList.isEmpty()) {
                    Map<String, Object> titleMap = titleList.get(0);
                    if (titleMap != null && titleMap.containsKey("text")) {
                      Map<String, Object> textMap = (Map<String, Object>) titleMap.get("text");
                      if (textMap != null && textMap.containsKey("content")) {
                        return textMap.get("content").toString();
                      }
                    }
                  }
                }
              }
              return "Default Name"; // Fallback
            });
    when(injuryTypeRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
    when(injuryTypeService.createInjuryType(anyString(), anyInt(), anyInt(), anyInt(), anyString()))
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
    when(injuryTypeService.updateInjuryType(any(InjuryType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::isNotionTokenAvailable)
        .thenReturn(false);
    mockedEnvironmentVariableUtil.when(EnvironmentVariableUtil::getNotionToken).thenReturn(null);
    when(injurySyncService.isNotionHandlerAvailable()).thenReturn(true);

    BaseSyncService.SyncResult result = injurySyncService.syncInjuryTypes("test-operation-id");

    assertThat(result).isNotNull();
    assertThat(result.isSuccess())
        .withFailMessage(result.getErrorMessage() == null ? "" : result.getErrorMessage())
        .isTrue();
    assertThat(result.getEntityType()).isEqualTo(SyncEntityType.INJURIES.getKey());
    assertThat(result.getSyncedCount()).isEqualTo(3);
    assertThat(result.getErrorCount()).isEqualTo(0);

    verify(notionHandler).loadAllInjuries();
    verify(injuryTypeService, times(3))
        .createInjuryType(anyString(), anyInt(), anyInt(), anyInt(), anyString());
    verify(injuryTypeService, times(3)).updateInjuryType(any(InjuryType.class));
  }

  @Test
  void syncInjuryTypes_WhenNoInjuriesFound_ShouldReturnSuccessWithZeroCount() {
    // Given
    when(syncProperties.isEntityEnabled(SyncEntityType.INJURIES.getKey())).thenReturn(true);
    when(notionHandler.loadAllInjuries()).thenReturn(Collections.emptyList());

    // When
    SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

    // Then
    assertTrue(result.isSuccess(), result.getErrorMessage());
    assertEquals(0, result.getSyncedCount());
    verify(progressTracker)
        .completeOperation(eq("test-operation"), eq(true), eq("No injuries to sync"), eq(0));
  }

  @Test
  void syncInjuryTypes_WhenDuplicateInjuries_ShouldSkipExisting() {
    // Given
    when(syncProperties.isEntityEnabled(SyncEntityType.INJURIES.getKey())).thenReturn(true);

    List<InjuryPage> mockPages = createMockInjuryPages();

    when(notionHandler.loadAllInjuries()).thenReturn(mockPages);

    when(notionPageDataExtractor.extractNameFromNotionPage(mockPages.get(0)))
        .thenReturn("Head Injury");

    when(notionPageDataExtractor.extractNameFromNotionPage(mockPages.get(1)))
        .thenReturn("Back Injury");

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

  @Test
  void syncInjuryTypes_WhenNotionHandlerThrowsException_ShouldReturnFailure() {
    // Given
    when(syncProperties.isEntityEnabled(SyncEntityType.INJURIES.getKey())).thenReturn(true);
    when(notionHandler.loadAllInjuries()).thenThrow(new RuntimeException("Notion API error"));

    // When
    SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

    // Then
    assertFalse(result.isSuccess());
    assertEquals(SyncEntityType.INJURIES.getKey(), result.getEntityType());
    assertTrue(result.getErrorMessage().contains("Notion API error"));

    // Verify error handling
    verify(progressTracker).failOperation(eq("test-operation"), anyString());
    verify(healthMonitor).recordFailure(eq(SyncEntityType.INJURIES.getKey()), anyString());
  }

  @Test
  void syncInjuryTypes_WhenServiceThrowsException_ShouldContinueWithOtherInjuries() {
    // Given
    when(syncProperties.isEntityEnabled(SyncEntityType.INJURIES.getKey())).thenReturn(true);

    List<InjuryPage> mockPages = createMockInjuryPages();

    lenient().when(notionHandler.loadAllInjuries()).thenReturn(mockPages);

    when(notionPageDataExtractor.extractNameFromNotionPage(mockPages.get(0)))
        .thenReturn("Head Injury");

    when(notionPageDataExtractor.extractNameFromNotionPage(mockPages.get(1)))
        .thenReturn("Back Injury");
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

  @Test
  void syncInjuryTypes_WhenValidationFails_ShouldReturnFailure() {
    // Given
    when(syncProperties.isEntityEnabled(SyncEntityType.INJURIES.getKey())).thenReturn(true);

    List<InjuryPage> mockPages = createMockInjuryPages();
    when(notionHandler.loadAllInjuries()).thenReturn(mockPages);

    // When
    SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

    // Then
    assertFalse(result.isSuccess());
    assertTrue(result.getErrorMessage().contains("validation failed"), result.getErrorMessage());
  }

  @Test
  void syncInjuryTypes_WhenNotionTokenNotAvailable_ShouldReturnFailure() {
    // Given
    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::isNotionTokenAvailable)
        .thenReturn(false);
    mockedEnvironmentVariableUtil.when(EnvironmentVariableUtil::getNotionToken).thenReturn(null);
    when(syncProperties.isEntityEnabled(SyncEntityType.INJURIES.getKey())).thenReturn(true);
    when(injurySyncService.isNotionHandlerAvailable()).thenReturn(false);

    // When
    SyncResult result = injurySyncService.syncInjuryTypes("test-operation");

    // Then
    assertFalse(result.isSuccess());
    assertEquals(SyncEntityType.INJURIES.getKey(), result.getEntityType());
    assertTrue(
        result.getErrorMessage().contains("NotionHandler is not available for injuries sync"),
        result.getErrorMessage());
    verify(notionHandler, never()).loadAllInjuries();
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
    lenient().when(page.getRawProperties()).thenReturn(properties);

    // Create a Notion-like title property structure
    Map<String, Object> textContent = new HashMap<>();
    textContent.put("content", name);
    Map<String, Object> textMap = new HashMap<>();
    textMap.put("text", textContent);
    textMap.put("type", "text"); // Mimic Notion's type for text object
    List<Map<String, Object>> titleList = Collections.singletonList(textMap);
    Map<String, Object> titleProperty = new HashMap<>();
    titleProperty.put("title", titleList);
    titleProperty.put("type", "title"); // Mimic Notion's type for title property

    properties.put("Name", titleProperty);
    properties.put("Health Effect", health);
    properties.put("Stamina Effect", stamina);
    properties.put("Card Effect", card);
    properties.put("Special Effects", special);

    return page;
  }

  private com.github.javydreamercsw.management.domain.injury.InjuryType createMockInjuryType() {
    com.github.javydreamercsw.management.domain.injury.InjuryType injuryType =
        new com.github.javydreamercsw.management.domain.injury.InjuryType();
    injuryType.setId(1L);
    injuryType.setInjuryName("Test Injury");
    return injuryType;
  }

  private List<InjuryPage> loadSampleInjuries() {
    return Arrays.asList(notionPage1, notionPage2, notionPage3);
  }
}
