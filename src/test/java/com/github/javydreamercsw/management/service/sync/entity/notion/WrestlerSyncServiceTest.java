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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.SyncSessionManager;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/**
 * Unit tests for WrestlerSyncService covering wrestler synchronization including stats,
 * relationships, and error handling.
 */
class WrestlerSyncServiceTest extends AbstractSyncTest {

  private WrestlerSyncService wrestlerSyncService;
  @Mock private NotionHandler notionHandler;
  @Mock private SyncServiceDependencies syncServiceDependencies;
  @Mock private SyncSessionManager syncSessionManager;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerNotionSyncService wrestlerNotionSyncService;
  @Mock private TierRecalculationService tierRecalculationService;
  @Mock private WrestlerAlignmentRepository wrestlerAlignmentRepository;
  @Mock private FactionRepository factionRepository;
  @Mock private NpcRepository npcRepository;
  @Mock private InjuryRepository injuryRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TitleReignRepository titleReignRepository;

  @BeforeEach
  @Override
  protected void setUp() {
    super.setUp(); // Call parent setup first
    lenient().when(notionApiExecutor.getSyncProperties()).thenReturn(syncProperties);
    lenient().when(notionApiExecutor.getNotionHandler()).thenReturn(notionHandler);
    lenient()
        .when(notionApiExecutor.executeWithRateLimit(any()))
        .thenAnswer(
            invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());
    lenient().when(syncServiceDependencies.getNotionHandler()).thenReturn(notionHandler);
    lenient().when(syncServiceDependencies.getSyncSessionManager()).thenReturn(syncSessionManager);
    lenient().when(syncServiceDependencies.getProgressTracker()).thenReturn(progressTracker);
    lenient().when(syncServiceDependencies.getHealthMonitor()).thenReturn(healthMonitor);
    lenient().when(syncServiceDependencies.getNotionSyncProperties()).thenReturn(syncProperties);
    lenient().when(syncServiceDependencies.getRateLimitService()).thenReturn(rateLimitService);
    lenient()
        .when(syncServiceDependencies.getNotionPageDataExtractor())
        .thenReturn(notionPageDataExtractor);
    lenient().when(syncProperties.isEntityEnabled("wrestlers")).thenReturn(true);
    lenient()
        .when(wrestlerService.save(any(Wrestler.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient()
        .when(wrestlerRepository.saveAndFlush(any(Wrestler.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Mock Name extraction
    lenient()
        .when(notionPageDataExtractor.extractNameFromNotionPage(any(WrestlerPage.class)))
        .thenAnswer(
            invocation -> {
              WrestlerPage page = invocation.getArgument(0);
              Map<String, Object> props = page.getRawProperties();
              if (props != null && props.containsKey("Name")) {
                Object nameObj = props.get("Name");
                if (nameObj instanceof String) return (String) nameObj;
                if (nameObj instanceof Map) {
                  Map<String, Object> nameMap = (Map<String, Object>) nameObj;
                  if (nameMap.containsKey("title")) {
                    List<Map<String, Object>> titleList =
                        (List<Map<String, Object>>) nameMap.get("title");
                    if (!titleList.isEmpty()) {
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
            wrestlerNotionSyncService,
            tierRecalculationService,
            wrestlerAlignmentRepository,
            factionRepository,
            npcRepository,
            injuryRepository,
            teamRepository,
            titleReignRepository);
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
    verify(healthMonitor).recordSuccess(eq("Wrestlers"), anyLong(), anyInt());
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
    properties.put("Drive", 5);
    properties.put("Resilience", 6);
    properties.put("Charisma", 6);
    properties.put("Brawl", 5);
    properties.put("Heritage Tag", "Texas");
    properties.put("Fans", 1000000L);
    properties.put("Starting Health", 20);
    properties.put("Starting Stamina", 20);

    lenient().when(page.getRawProperties()).thenReturn(properties);
    when(notionHandler.loadAllWrestlers()).thenReturn(List.of(page));
    when(wrestlerAlignmentRepository.findByWrestler(any())).thenReturn(Optional.empty());

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
    lenient().when(page.getId()).thenReturn(id);

    Map<String, Object> properties = new HashMap<>();
    properties.put("Name", Map.of("title", List.of(Map.of("text", Map.of("content", name)))));
    properties.put("Health", health);
    properties.put("Stamina", stamina);
    properties.put("Charisma", charisma);
    lenient().when(page.getRawProperties()).thenReturn(properties);

    return page;
  }
}
