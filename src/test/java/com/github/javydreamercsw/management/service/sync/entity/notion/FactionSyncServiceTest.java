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
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
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
 * Unit tests for FactionSyncService covering faction synchronization scenarios including
 * relationship handling and error conditions.
 */
class FactionSyncServiceTest extends AbstractSyncTest {

  @Mock private FactionService factionService;

  private FactionSyncService factionSyncService;

  @BeforeEach
  @Override
  protected void setUp() {
    super.setUp(); // Calls AbstractSyncTest's setUp which initializes syncServiceDependencies

    factionSyncService =
        new FactionSyncService(
            objectMapper, syncServiceDependencies, factionService, notionApiExecutor);
  }

  @Test
  void syncFactions_WhenDisabled_ShouldReturnSuccessWithoutSync() {
    // Given
    when(syncProperties.isEntityEnabled(SyncEntityType.FACTIONS.getKey())).thenReturn(false);

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals(SyncEntityType.FACTIONS.getKey(), result.getEntityType());
    verify(notionHandler, never()).loadAllFactions();
  }

  @Test
  void syncFactions_WithAllProperties_ShouldSyncCorrectly() {
    // Given
    FactionPage page = mock(FactionPage.class);
    when(page.getId()).thenReturn("faction-1");

    Map<String, Object> properties = new HashMap<>();
    properties.put(
        "Name", Map.of("title", List.of(Map.of("text", Map.of("content", "Desolation's Smile")))));
    properties.put("Status", true);
    properties.put("Description", "Primal chaos faction.");
    properties.put("Alignment", "HEEL");
    properties.put("Leader", List.of(Map.of("id", "leader-id")));
    properties.put("Manager", List.of(Map.of("id", "manager-id")));
    properties.put("Members", List.of(Map.of("id", "member-1"), Map.of("id", "member-2")));
    properties.put("Teams", List.of(Map.of("id", "team-1")));
    properties.put("Formed Date", "2025-01-01T00:00:00Z");

    when(page.getRawProperties()).thenReturn(properties);
    when(notionHandler.loadAllFactions()).thenReturn(List.of(page));

    // Mock Name extraction
    lenient()
        .when(notionPageDataExtractor.extractNameFromNotionPage(any(FactionPage.class)))
        .thenReturn("Desolation's Smile");

    // Mock Description extraction
    lenient()
        .when(notionPageDataExtractor.extractDescriptionFromNotionPage(any(FactionPage.class)))
        .thenReturn("Primal chaos faction.");

    // Mock dependencies
    Wrestler leader = Wrestler.builder().build();
    leader.setExternalId("leader-id");
    when(wrestlerRepository.findByExternalId("leader-id")).thenReturn(Optional.of(leader));

    Npc manager = new Npc();
    manager.setExternalId("manager-id");
    when(npcRepository.findByExternalId("manager-id")).thenReturn(Optional.of(manager));

    Wrestler member1 = Wrestler.builder().build();
    member1.setExternalId("member-1");
    Wrestler member2 = Wrestler.builder().build();
    member2.setExternalId("member-2");
    when(wrestlerRepository.findByExternalId("member-1")).thenReturn(Optional.of(member1));
    when(wrestlerRepository.findByExternalId("member-2")).thenReturn(Optional.of(member2));

    Team team1 = new Team();
    team1.setExternalId("team-1");
    when(teamRepository.findByExternalId("team-1")).thenReturn(Optional.of(team1));

    when(factionService.findByExternalId("faction-1")).thenReturn(Optional.empty());
    when(factionService.getFactionByName("Desolation's Smile")).thenReturn(Optional.empty());

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertTrue(result.isSuccess());
    ArgumentCaptor<Faction> factionCaptor = ArgumentCaptor.forClass(Faction.class);
    verify(factionService).save(factionCaptor.capture());

    Faction savedFaction = factionCaptor.getValue();
    assertEquals("Desolation's Smile", savedFaction.getName());
    assertEquals("Primal chaos faction.", savedFaction.getDescription());
    assertTrue(savedFaction.isActive());
    assertEquals("HEEL", savedFaction.getAlignment());
    assertEquals(leader, savedFaction.getLeader());
    assertEquals(manager, savedFaction.getManager());
    assertTrue(savedFaction.getMembers().contains(member1));
    assertTrue(savedFaction.getMembers().contains(member2));
    assertTrue(savedFaction.getTeams().contains(team1));
  }

  @Test
  void syncFactions_WhenNoFactionsFound_ShouldReturnSuccessWithZeroCount() {
    // Given
    when(notionHandler.loadAllFactions()).thenReturn(Collections.emptyList());

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertTrue(result.isSuccess());
    verify(factionService, never()).save(any(Faction.class));
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
}
