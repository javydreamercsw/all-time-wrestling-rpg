package com.github.javydreamercsw.management.controller.rivalry;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.controller.rivalry.RivalryController.AddHeatRequest;
import com.github.javydreamercsw.management.controller.rivalry.RivalryController.CreateRivalryRequest;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for RivalryController. Tests the complete REST API functionality for rivalry
 * management.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("RivalryController Integration Tests")
class RivalryControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private DeckRepository deckRepository;

  @BeforeEach
  void setUp() {
    // Delete in correct order to avoid foreign key constraint violations
    rivalryRepository.deleteAll();
    deckRepository.deleteAll();
    wrestlerRepository.deleteAll();
  }

  @Test
  @DisplayName("Should create new rivalry successfully")
  void shouldCreateNewRivalrySuccessfully() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50000L);

    CreateRivalryRequest request =
        new CreateRivalryRequest(wrestler1.getId(), wrestler2.getId(), "Test storyline");

    mockMvc
        .perform(
            post("/api/rivalries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.wrestler1.name").value("Wrestler 1"))
        .andExpect(jsonPath("$.wrestler2.name").value("Wrestler 2"))
        .andExpect(jsonPath("$.heat").value(0))
        .andExpect(jsonPath("$.isActive").value(true))
        .andExpect(jsonPath("$.storylineNotes").value("Test storyline"));
  }

  @Test
  @DisplayName("Should return existing rivalry if already exists")
  void shouldReturnExistingRivalryIfAlreadyExists() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50000L);

    // Create existing rivalry
    Rivalry existingRivalry = createTestRivalry(wrestler1, wrestler2, 10);

    CreateRivalryRequest request =
        new CreateRivalryRequest(wrestler1.getId(), wrestler2.getId(), "New storyline");

    mockMvc
        .perform(
            post("/api/rivalries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk()) // Should return existing rivalry
        .andExpect(jsonPath("$.heat").value(10)); // Should have existing heat
  }

  @Test
  @DisplayName("Should add heat to rivalry")
  void shouldAddHeatToRivalry() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50000L);
    Rivalry rivalry = createTestRivalry(wrestler1, wrestler2, 5);

    AddHeatRequest request = new AddHeatRequest(3, "Backstage confrontation");

    mockMvc
        .perform(
            post("/api/rivalries/{id}/heat", rivalry.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.heat").value(8)); // 5 + 3
  }

  @Test
  @DisplayName("Should get all rivalries with pagination")
  void shouldGetAllRivalriesWithPagination() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50000L);
    Wrestler wrestler3 = createTestWrestler("Wrestler 3", 50000L);

    createTestRivalry(wrestler1, wrestler2, 10);
    createTestRivalry(wrestler1, wrestler3, 15);

    mockMvc
        .perform(get("/api/rivalries").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  @DisplayName("Should get rivalry by ID")
  void shouldGetRivalryById() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50000L);
    Rivalry rivalry = createTestRivalry(wrestler1, wrestler2, 15);

    mockMvc
        .perform(get("/api/rivalries/{id}", rivalry.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.heat").value(15))
        .andExpect(jsonPath("$.wrestler1.name").value("Wrestler 1"))
        .andExpect(jsonPath("$.wrestler2.name").value("Wrestler 2"));
  }

  @Test
  @DisplayName("Should get active rivalries")
  void shouldGetActiveRivalries() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50000L);
    Wrestler wrestler3 = createTestWrestler("Wrestler 3", 50000L);

    Rivalry activeRivalry = createTestRivalry(wrestler1, wrestler2, 10);
    Rivalry inactiveRivalry = createTestRivalry(wrestler1, wrestler3, 5);
    inactiveRivalry.setIsActive(false);
    rivalryRepository.save(inactiveRivalry);

    mockMvc
        .perform(get("/api/rivalries/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].heat").value(10));
  }

  @Test
  @DisplayName("Should get rivalries for wrestler")
  void shouldGetRivalriesForWrestler() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50000L);
    Wrestler wrestler3 = createTestWrestler("Wrestler 3", 50000L);

    createTestRivalry(wrestler1, wrestler2, 10);
    createTestRivalry(wrestler1, wrestler3, 15);
    createTestRivalry(wrestler2, wrestler3, 5); // Should not be included

    mockMvc
        .perform(get("/api/rivalries/wrestler/{wrestlerId}", wrestler1.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("Should get rivalries requiring matches")
  void shouldGetRivalriesRequiringMatches() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50000L);
    Wrestler wrestler3 = createTestWrestler("Wrestler 3", 50000L);

    createTestRivalry(wrestler1, wrestler2, 12); // Requires match (10+ heat)
    createTestRivalry(wrestler1, wrestler3, 5); // Doesn't require match

    mockMvc
        .perform(get("/api/rivalries/requiring-matches"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].heat").value(12));
  }

  @Test
  @DisplayName("Should validate required fields when creating rivalry")
  void shouldValidateRequiredFieldsWhenCreatingRivalry() throws Exception {
    CreateRivalryRequest request =
        new CreateRivalryRequest(
            null, // Invalid null wrestler1Id
            null, // Invalid null wrestler2Id
            null);

    mockMvc
        .perform(
            post("/api/rivalries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should validate heat gain limits")
  void shouldValidateHeatGainLimits() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50000L);
    Rivalry rivalry = createTestRivalry(wrestler1, wrestler2, 5);

    AddHeatRequest request = new AddHeatRequest(100, "Too much heat"); // Exceeds max of 50

    mockMvc
        .perform(
            post("/api/rivalries/{id}/heat", rivalry.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 404 for non-existent rivalry")
  void shouldReturn404ForNonExistentRivalry() throws Exception {
    mockMvc.perform(get("/api/rivalries/{id}", 999L)).andExpect(status().isNotFound());
  }

  private Wrestler createTestWrestler(String name, Long fans) {
    Wrestler wrestler = new Wrestler();
    wrestler.setName(name);
    wrestler.setFans(fans);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(15);
    wrestler.setCurrentHealth(15);
    wrestler.setLowHealth(5);
    wrestler.setLowStamina(5);
    wrestler.setDeckSize(40);
    wrestler.setBumps(0);
    wrestler.setIsPlayer(true);
    wrestler.updateTier();
    return wrestlerRepository.save(wrestler);
  }

  private Rivalry createTestRivalry(Wrestler wrestler1, Wrestler wrestler2, int heat) {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(heat);
    rivalry.setIsActive(true);
    rivalry.setStorylineNotes("Test storyline");
    return rivalryRepository.save(rivalry);
  }
}
