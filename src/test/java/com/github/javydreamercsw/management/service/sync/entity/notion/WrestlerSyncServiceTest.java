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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/**
 * Unit tests for WrestlerSyncService covering wrestler synchronization including stats,
 * relationships, and error handling.
 */
class WrestlerSyncServiceTest extends AbstractSyncTest {

  private WrestlerSyncService wrestlerSyncService;

  @Mock private WrestlerNotionSyncService wrestlerNotionSyncService;
  @Mock private WrestlerStateRepository wrestlerStateRepository;

  @BeforeEach
  protected void setUp() {
    super.setUp();

    // Mock Name extraction
    lenient()
        .when(notionPageDataExtractor.extractNameFromNotionPage(any(WrestlerPage.class)))
        .thenAnswer(
            invocation -> {
              WrestlerPage page = invocation.getArgument(0);
              Map<String, Object> props = page.getRawProperties();
              if (props != null && props.containsKey("Name")) {
                Object nameObj = props.get("Name");
                if (nameObj instanceof Map) {
                  Map<String, Object> nameMap = (Map<String, Object>) nameObj;
                  if (nameMap.containsKey("title")) {
                    List<Map<String, Object>> titleList =
                        (List<Map<String, Object>>) nameMap.get("title");
                    if (titleList != null && !titleList.isEmpty()) {
                      Map<String, Object> titleMap = titleList.get(0);
                      if (titleMap.containsKey("text")) {
                        Map<String, Object> textMap = (Map<String, Object>) titleMap.get("text");
                        if (textMap.containsKey("content")) {
                          return (String) textMap.get("content");
                        }
                      }
                    }
                  }
                }
              }
              return "Unknown";
            });

    wrestlerSyncService =
        new WrestlerSyncService(
            objectMapper,
            syncServiceDependencies,
            notionApiExecutor,
            wrestlerService,
            wrestlerRepository,
            wrestlerStateRepository,
            wrestlerNotionSyncService,
            tierRecalculationService,
            wrestlerAlignmentRepository,
            factionRepository,
            npcRepository,
            injuryRepository);
  }

  @Test
  void syncWrestlers_WhenSuccessful_ShouldReturnCorrectResult() {
    // Given
    List<WrestlerPage> mockPages = createMockWrestlerPages();
    when(notionHandler.loadAllWrestlers()).thenReturn(mockPages);

    // When
    SyncResult result = wrestlerSyncService.syncWrestlers("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals("Wrestlers", result.getEntityType());
    verify(wrestlerService, times(2)).save(any(Wrestler.class));
  }

  @Test
  void syncWrestlers_WithNewProperties_ShouldSyncCorrectly() {
    // Given
    WrestlerPage page = mock(WrestlerPage.class);
    lenient().when(page.getId()).thenReturn("wrestler-1");

    Map<String, Object> properties = new HashMap<>();
    properties.put(
        "Name",
        Map.of("title", List.of(Map.of("text", Map.of("content", "Stone Cold Steve Austin")))));
    properties.put("Alignment", "FACE");
    properties.put("Drive", 5.0);
    properties.put("Resilience", 6.0);
    properties.put("Charisma", 6.0);
    properties.put("Brawl", 5.0);
    properties.put("Heritage Tag", "Texas");
    properties.put("Fans", 1000000.0);
    properties.put("Starting Health", 20.0);
    properties.put("Starting Stamina", 20.0);

    lenient().when(page.getRawProperties()).thenReturn(properties);
    when(notionHandler.loadAllWrestlers()).thenReturn(List.of(page));

    // When
    SyncResult result = wrestlerSyncService.syncWrestlers("test-operation");

    // Then
    assertTrue(result.isSuccess());
    ArgumentCaptor<Wrestler> wrestlerCaptor = ArgumentCaptor.forClass(Wrestler.class);
    verify(wrestlerService).save(wrestlerCaptor.capture());

    Wrestler savedWrestler = wrestlerCaptor.getValue();
    assertEquals("Stone Cold Steve Austin", savedWrestler.getName());
    assertEquals(5, savedWrestler.getDrive());
    assertEquals(6, savedWrestler.getResilience());
    assertEquals(6, savedWrestler.getCharisma());
    assertEquals(5, savedWrestler.getBrawl());
    assertEquals("Texas", savedWrestler.getHeritageTag());
    assertNotNull(savedWrestler.getAlignment());
    assertEquals(AlignmentType.FACE, savedWrestler.getAlignment().getAlignmentType());
  }

  @Test
  void syncWrestlers_WhenDisabled_ShouldSkipSync() {
    // Given
    lenient().when(syncProperties.isEnabled()).thenReturn(false);

    // When
    SyncResult result = wrestlerSyncService.syncWrestlers("test-operation");

    // Then
    assertTrue(result.isSuccess());
    verify(notionHandler, never()).loadAllWrestlers();
  }

  @Test
  void syncWrestlers_WhenNoWrestlersFound_ShouldReturnSuccess() {
    // Given
    lenient().when(notionHandler.loadAllWrestlers()).thenReturn(Collections.emptyList());

    // When
    SyncResult result = wrestlerSyncService.syncWrestlers("test-operation");

    // Then
    assertTrue(result.isSuccess());
    verify(wrestlerService, never()).save(any(Wrestler.class));
  }

  private List<WrestlerPage> createMockWrestlerPages() {
    WrestlerPage wrestler1 = createMockWrestlerPage("wrestler-1", "John Cena", 85, 90, 95);
    WrestlerPage wrestler2 = createMockWrestlerPage("wrestler-2", "The Rock", 90, 95, 85);
    return Arrays.asList(wrestler1, wrestler2);
  }

  private WrestlerPage createMockWrestlerPage(
      String id, String name, int health, int stamina, int charisma) {
    WrestlerPage page = mock(WrestlerPage.class);
    lenient().when(page.getId()).thenReturn(id);

    Map<String, Object> properties = new HashMap<>();
    properties.put("Name", Map.of("title", List.of(Map.of("text", Map.of("content", name)))));
    properties.put("Starting Health", (double) health);
    properties.put("Starting Stamina", (double) stamina);
    properties.put("Charisma", (double) charisma);
    properties.put("Tier", "Main Event");
    properties.put("Fans", 1000.0);
    lenient().when(page.getRawProperties()).thenReturn(properties);

    return page;
  }
}
