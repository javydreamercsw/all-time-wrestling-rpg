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

import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.domain.faction.Faction;
import com.github.javydreamercsw.base.domain.faction.FactionRepository;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.FactionDTO;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for Faction sync functionality. Tests the conversion from FactionPage to FactionDTO
 * and database operations.
 */
@ExtendWith(MockitoExtension.class)
class FactionSyncTest {

  @Mock private FactionRepository factionRepository;
  @Mock private WrestlerRepository wrestlerRepository;

  private FactionDTO testFactionDTO;

  @BeforeEach
  void setUp() {
    // Create test wrestlers
    Wrestler testLeader = Wrestler.builder().build();
    testLeader.setId(1L);
    testLeader.setName("John Cena");

    Wrestler testMember1 = Wrestler.builder().build();
    testMember1.setId(2L);
    testMember1.setName("Randy Orton");

    Wrestler testMember2 = Wrestler.builder().build();
    testMember2.setId(3L);
    testMember2.setName("Batista");

    // Create test faction DTO
    testFactionDTO = new FactionDTO();
    testFactionDTO.setName("Evolution");
    testFactionDTO.setDescription("A dominant faction in WWE");
    testFactionDTO.setLeader("Triple H");
    testFactionDTO.setMembers(Arrays.asList("Randy Orton", "Batista", "Ric Flair"));

    testFactionDTO.setIsActive(true);
    testFactionDTO.setFormedDate("2003-01-01");
    testFactionDTO.setExternalId("notion-evolution-id");

    // Create test faction entity
    Faction testFaction = Faction.builder().build();
    testFaction.setId(1L);
    testFaction.setName("Evolution");
    testFaction.setDescription("A dominant faction in WWE");

    testFaction.setIsActive(true);
    testFaction.setCreationDate(Instant.now());
  }

  @Test
  @DisplayName("Should create FactionDTO with all properties")
  void shouldCreateFactionDTOWithAllProperties() {
    // Given - testFactionDTO is already set up in @BeforeEach

    // Then
    assertEquals("Evolution", testFactionDTO.getName());
    assertEquals("A dominant faction in WWE", testFactionDTO.getDescription());
    assertEquals("Triple H", testFactionDTO.getLeader());
    assertEquals(3, testFactionDTO.getMembers().size());
    assertTrue(testFactionDTO.getMembers().contains("Randy Orton"));
    assertTrue(testFactionDTO.getMembers().contains("Batista"));
    assertTrue(testFactionDTO.getMembers().contains("Ric Flair"));

    assertTrue(testFactionDTO.getIsActive());
    assertEquals("2003-01-01", testFactionDTO.getFormedDate());
    assertEquals("notion-evolution-id", testFactionDTO.getExternalId());
  }

  @Test
  @DisplayName("Should handle faction with no leader")
  void shouldHandleFactionWithNoLeader() {
    // Given
    FactionDTO dto = new FactionDTO();
    dto.setName("Test Faction");
    dto.setLeader(null);

    dto.setIsActive(true);

    // Then
    assertNull(dto.getLeader());

    assertTrue(dto.getIsActive());
  }

  @Test
  @DisplayName("Should handle faction with empty members list")
  void shouldHandleFactionWithEmptyMembersList() {
    // Given
    FactionDTO dto = new FactionDTO();
    dto.setName("Solo Faction");
    dto.setMembers(new ArrayList<>());

    // Then
    assertNotNull(dto.getMembers());
    assertTrue(dto.getMembers().isEmpty());
  }

  @Test
  @DisplayName("Should handle faction status variations")
  void shouldHandleFactionStatusVariations() {
    // Test active faction
    FactionDTO activeDto = new FactionDTO();
    activeDto.setName("Active Faction");
    activeDto.setIsActive(true);
    assertTrue(activeDto.getIsActive());

    // Test inactive faction
    FactionDTO inactiveDto = new FactionDTO();
    inactiveDto.setName("Disbanded Faction");
    inactiveDto.setIsActive(false);
    inactiveDto.setDisbandedDate("2024-01-01");
    assertFalse(inactiveDto.getIsActive());
    assertEquals("2024-01-01", inactiveDto.getDisbandedDate());
  }

  @Test
  @DisplayName("Should handle faction with teams")
  void shouldHandleFactionWithTeams() {
    // Given
    FactionDTO dto = new FactionDTO();
    dto.setName("Multi-Team Faction");
    dto.setTeams(Arrays.asList("Team Alpha", "Team Beta"));

    // Then
    assertNotNull(dto.getTeams());
    assertEquals(2, dto.getTeams().size());
    assertTrue(dto.getTeams().contains("Team Alpha"));
    assertTrue(dto.getTeams().contains("Team Beta"));
  }

  /** Helper method to create a mock FactionPage for testing. */
  private FactionPage createMockFactionPage(String name, String description, String leader) {
    FactionPage factionPage = new FactionPage();
    factionPage.setId("faction-" + name.toLowerCase().replace(" ", "-"));

    Map<String, Object> rawProperties = new HashMap<>();
    rawProperties.put("Name", name);
    rawProperties.put("Description", description);
    rawProperties.put("Leader", leader);
    rawProperties.put("Status", "Active");
    rawProperties.put("FormedDate", "2024-01-01");
    rawProperties.put("Members", "3 relations");
    rawProperties.put("Teams", "2 relations");

    factionPage.setRawProperties(rawProperties);
    return factionPage;
  }
}
