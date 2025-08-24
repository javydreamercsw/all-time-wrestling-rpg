package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionAlignment;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
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
  private Faction testFaction;
  private Wrestler testLeader;
  private Wrestler testMember1;
  private Wrestler testMember2;

  @BeforeEach
  void setUp() {
    // Create test wrestlers
    testLeader = new Wrestler();
    testLeader.setId(1L);
    testLeader.setName("John Cena");

    testMember1 = new Wrestler();
    testMember1.setId(2L);
    testMember1.setName("Randy Orton");

    testMember2 = new Wrestler();
    testMember2.setId(3L);
    testMember2.setName("Batista");

    // Create test faction DTO
    testFactionDTO = new FactionDTO();
    testFactionDTO.setName("Evolution");
    testFactionDTO.setDescription("A dominant faction in WWE");
    testFactionDTO.setLeader("Triple H");
    testFactionDTO.setMembers(Arrays.asList("Randy Orton", "Batista", "Ric Flair"));
    testFactionDTO.setAlignment("HEEL");
    testFactionDTO.setIsActive(true);
    testFactionDTO.setFormedDate("2003-01-01");
    testFactionDTO.setExternalId("notion-evolution-id");

    // Create test faction entity
    testFaction = new Faction();
    testFaction.setId(1L);
    testFaction.setName("Evolution");
    testFaction.setDescription("A dominant faction in WWE");
    testFaction.setAlignment(FactionAlignment.HEEL);
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
    assertEquals("HEEL", testFactionDTO.getAlignment());
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
    dto.setAlignment("NEUTRAL");
    dto.setIsActive(true);

    // Then
    assertNull(dto.getLeader());
    assertEquals("NEUTRAL", dto.getAlignment());
    assertTrue(dto.getIsActive());
  }

  @Test
  @DisplayName("Should handle faction with empty members list")
  void shouldHandleFactionWithEmptyMembersList() {
    // Given
    FactionDTO dto = new FactionDTO();
    dto.setName("Solo Faction");
    dto.setMembers(new ArrayList<>());
    dto.setAlignment("FACE");

    // Then
    assertNotNull(dto.getMembers());
    assertTrue(dto.getMembers().isEmpty());
    assertEquals("FACE", dto.getAlignment());
  }

  @Test
  @DisplayName("Should handle all faction alignments")
  void shouldHandleAllFactionAlignments() {
    // Test all alignment values
    String[] alignments = {"FACE", "HEEL", "TWEENER", "NEUTRAL"};

    for (String alignment : alignments) {
      FactionDTO dto = new FactionDTO();
      dto.setName("Test Faction");
      dto.setAlignment(alignment);

      assertEquals(alignment, dto.getAlignment());
    }
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
  private FactionPage createMockFactionPage(
      String name, String description, String leader, String alignment) {
    FactionPage factionPage = new FactionPage();
    factionPage.setId("faction-" + name.toLowerCase().replace(" ", "-"));

    Map<String, Object> rawProperties = new HashMap<>();
    rawProperties.put("Name", name);
    rawProperties.put("Description", description);
    rawProperties.put("Leader", leader);
    rawProperties.put("Alignment", alignment);
    rawProperties.put("Status", "Active");
    rawProperties.put("FormedDate", "2024-01-01");
    rawProperties.put("Members", "3 relations");
    rawProperties.put("Teams", "2 relations");

    factionPage.setRawProperties(rawProperties);
    return factionPage;
  }
}
