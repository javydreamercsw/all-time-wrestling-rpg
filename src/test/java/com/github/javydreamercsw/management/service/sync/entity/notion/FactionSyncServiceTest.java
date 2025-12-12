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

import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
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
 * Unit tests for FactionSyncService covering faction synchronization scenarios including
 * relationship handling and error conditions.
 */
@ExtendWith(MockitoExtension.class)
class FactionSyncServiceTest extends AbstractSyncTest {

  @Mock private FactionRepository factionRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private FactionService factionService;

  private FactionSyncService factionSyncService;

  @BeforeEach
  protected void setUp() {
    super.setUp(); // Calls AbstractSyncTest's setUp which initializes syncServiceDependencies

    factionSyncService =
        new FactionSyncService(
            objectMapper, syncServiceDependencies, factionService, notionApiExecutor);
  }

  @Test
  void syncFactions_WhenDisabled_ShouldReturnSuccessWithoutSync() {
    // Given
    when(syncProperties.isEntityEnabled("factions")).thenReturn(false);

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals("Factions", result.getEntityType());
    verify(notionHandler, never()).loadAllFactions();
  }

  @Test
  void syncFactions_WhenSuccessful_ShouldReturnCorrectResult() {
    // Given
    List<FactionPage> mockPages = createMockFactionPages();
    when(notionHandler.loadAllFactions()).thenReturn(mockPages);
    when(factionService.findByExternalId(anyString())).thenReturn(Optional.empty());
    lenient()
        .when(factionService.save(any(Faction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals("Factions", result.getEntityType());
    verify(factionService, times(2)).save(any(Faction.class));
    verify(healthMonitor).recordSuccess(eq("Factions"), anyLong(), anyInt());
  }

  @Test
  void syncFactions_WhenNoFactionsFound_ShouldReturnSuccessWithZeroCount() {
    // Given
    when(notionHandler.loadAllFactions()).thenReturn(Collections.emptyList());

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertTrue(result.isSuccess());
    verify(factionRepository, never()).saveAndFlush(any(Faction.class));
  }

  @Test
  void syncFactions_WhenDuplicateFactionsExist_ShouldUpdateExisting() {
    // Given
    List<FactionPage> mockPages = createMockFactionPages();
    Faction existingFaction = Faction.builder().build();
    existingFaction.setId(1L);
    existingFaction.setName("The Shield");

    when(notionHandler.loadAllFactions()).thenReturn(mockPages);
    when(factionService.findByExternalId("faction-1")).thenReturn(Optional.of(existingFaction));
    when(factionService.save(any(Faction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertTrue(result.isSuccess());
    verify(factionService, atLeast(1)).save(any(Faction.class));
  }

  @Test
  void syncFactions_WhenNotionHandlerThrowsException_ShouldReturnFailure() {
    // Given
    when(notionHandler.loadAllFactions()).thenThrow(new RuntimeException("Notion API error"));

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertFalse(result.isSuccess());
    assertTrue(result.getErrorMessage().contains("Notion API error"));
    verify(progressTracker).failOperation(eq("test-operation"), anyString());
    verify(healthMonitor).recordFailure(eq("Factions"), anyString());
  }

  @Test
  void syncFactions_WhenRepositoryThrowsException_ShouldReturnFailure() {
    // Given
    List<FactionPage> mockPages = createMockFactionPages();
    when(notionHandler.loadAllFactions()).thenReturn(mockPages);
    when(factionService.findByExternalId(anyString())).thenReturn(Optional.empty());
    lenient()
        .when(factionService.save(any(Faction.class)))
        .thenThrow(new RuntimeException("Database error"));

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertFalse(result.isSuccess());
    assertTrue(result.getErrorMessage().contains("Some factions failed to sync"));
  }

  private List<FactionPage> createMockFactionPages() {
    FactionPage faction1 = createMockFactionPage("faction-1", "The Shield", "Active", "2012-11-18");
    FactionPage faction2 = createMockFactionPage("faction-2", "DX", "Disbanded", "1997-08-11");
    return Arrays.asList(faction1, faction2);
  }

  private FactionPage createMockFactionPage(
      String id, String name, String status, String formedDate) {
    FactionPage page = mock(FactionPage.class);
    when(page.getId()).thenReturn(id);

    // Mock raw properties
    Map<String, Object> properties = new HashMap<>();
    properties.put("Name", Map.of("title", List.of(Map.of("text", Map.of("content", name)))));
    properties.put("Status", Map.of("select", Map.of("name", status)));
    properties.put("FormedDate", Map.of("date", Map.of("start", formedDate)));
    when(page.getRawProperties()).thenReturn(properties);

    return page;
  }
}
