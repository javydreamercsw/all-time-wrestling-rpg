package com.github.javydreamercsw.management.controller.injury;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.controller.injury.InjuryController.CreateInjuryRequest;
import com.github.javydreamercsw.management.controller.injury.InjuryController.HealingAttemptRequest;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
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
 * Integration tests for InjuryController. Tests the complete REST API functionality for injury
 * management.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("InjuryController Integration Tests")
class InjuryControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private InjuryRepository injuryRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private DeckRepository deckRepository;

  @BeforeEach
  void setUp() {
    // Delete in correct order to avoid foreign key constraint violations
    injuryRepository.deleteAll();
    deckRepository.deleteAll();
    wrestlerRepository.deleteAll();
  }

  @Test
  @DisplayName("Should create new injury successfully")
  void shouldCreateNewInjurySuccessfully() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50000L);

    CreateInjuryRequest request =
        new CreateInjuryRequest(
            wrestler.getId(),
            "Knee Injury",
            "Torn ACL",
            InjurySeverity.SEVERE,
            "Occurred during segment");

    mockMvc
        .perform(
            post("/api/injuries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Knee Injury"))
        .andExpect(jsonPath("$.description").value("Torn ACL"))
        .andExpect(jsonPath("$.severity").value("SEVERE"))
        .andExpect(jsonPath("$.wrestler.name").value("Test Wrestler"))
        .andExpect(jsonPath("$.isActive").value(true));
  }

  @Test
  @DisplayName("Should create injury from bumps when wrestler has 3+ bumps")
  void shouldCreateInjuryFromBumpsWhenWrestlerHas3PlusBumps() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50000L);
    wrestler.setBumps(3);
    wrestlerRepository.save(wrestler);

    mockMvc
        .perform(post("/api/injuries/from-bumps/{wrestlerId}", wrestler.getId()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.wrestler.name").value("Test Wrestler"))
        .andExpect(jsonPath("$.isActive").value(true))
        .andExpect(
            jsonPath("$.injuryNotes").value("Generated from bump accumulation (tier: CONTENDER)"));
  }

  @Test
  @DisplayName("Should not create injury from bumps when wrestler has less than 3 bumps")
  void shouldNotCreateInjuryFromBumpsWhenWrestlerHasLessThan3Bumps() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50000L);
    wrestler.setBumps(2);
    wrestlerRepository.save(wrestler);

    mockMvc
        .perform(post("/api/injuries/from-bumps/{wrestlerId}", wrestler.getId()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Wrestler has less than 3 bumps (2)"));
  }

  @Test
  @DisplayName("Should get all injuries with pagination")
  void shouldGetAllInjuriesWithPagination() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50000L);
    createTestInjury(wrestler, "Injury 1", InjurySeverity.MINOR);
    createTestInjury(wrestler, "Injury 2", InjurySeverity.MODERATE);

    mockMvc
        .perform(get("/api/injuries").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  @DisplayName("Should get injury by ID")
  void shouldGetInjuryById() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50000L);
    Injury injury = createTestInjury(wrestler, "Test Injury", InjurySeverity.SEVERE);

    mockMvc
        .perform(get("/api/injuries/{id}", injury.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Injury"))
        .andExpect(jsonPath("$.severity").value("SEVERE"));
  }

  @Test
  @DisplayName("Should get active injuries for wrestler")
  void shouldGetActiveInjuriesForWrestler() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50000L);
    Injury activeInjury = createTestInjury(wrestler, "Active Injury", InjurySeverity.MINOR);
    Injury healedInjury = createTestInjury(wrestler, "Healed Injury", InjurySeverity.MODERATE);
    healedInjury.heal();
    injuryRepository.save(healedInjury);

    mockMvc
        .perform(get("/api/injuries/wrestler/{wrestlerId}/active", wrestler.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Active Injury"));
  }

  @Test
  @DisplayName("Should get injuries by severity")
  void shouldGetInjuriesBySeverity() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50000L);
    createTestInjury(wrestler, "Severe Injury 1", InjurySeverity.SEVERE);
    createTestInjury(wrestler, "Severe Injury 2", InjurySeverity.SEVERE);
    createTestInjury(wrestler, "Minor Injury", InjurySeverity.MINOR);

    mockMvc
        .perform(get("/api/injuries/severity/{severity}", InjurySeverity.SEVERE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("Should attempt healing with successful dice roll")
  void shouldAttemptHealingWithSuccessfulDiceRoll() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50000L);
    Injury injury = createTestInjury(wrestler, "Minor Injury", InjurySeverity.MINOR);

    HealingAttemptRequest request = new HealingAttemptRequest(6); // Should succeed for MINOR

    mockMvc
        .perform(
            post("/api/injuries/{id}/heal", injury.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.diceRoll").value(6))
        .andExpect(jsonPath("$.fansSpent").value(true));
  }

  @Test
  @DisplayName("Should attempt healing with failed dice roll")
  void shouldAttemptHealingWithFailedDiceRoll() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50000L);
    Injury injury = createTestInjury(wrestler, "Minor Injury", InjurySeverity.MINOR);

    HealingAttemptRequest request = new HealingAttemptRequest(2); // Should fail for MINOR

    mockMvc
        .perform(
            post("/api/injuries/{id}/heal", injury.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.diceRoll").value(2))
        .andExpect(jsonPath("$.fansSpent").value(true));
  }

  @Test
  @DisplayName("Should get total health penalty for wrestler")
  void shouldGetTotalHealthPenaltyForWrestler() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50000L);
    createTestInjury(wrestler, "Injury 1", InjurySeverity.MINOR);
    createTestInjury(wrestler, "Injury 2", InjurySeverity.MODERATE);

    mockMvc
        .perform(get("/api/injuries/wrestler/{wrestlerId}/health-penalty", wrestler.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalHealthPenalty").isNumber());
  }

  @Test
  @DisplayName("Should validate required fields when creating injury")
  void shouldValidateRequiredFieldsWhenCreatingInjury() throws Exception {
    CreateInjuryRequest request =
        new CreateInjuryRequest(
            null, // Invalid null wrestlerId
            "", // Invalid empty name
            null, null, // Invalid null severity
            null);

    mockMvc
        .perform(
            post("/api/injuries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should validate dice roll range for healing")
  void shouldValidateDiceRollRangeForHealing() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50000L);
    Injury injury = createTestInjury(wrestler, "Test Injury", InjurySeverity.MINOR);

    HealingAttemptRequest request = new HealingAttemptRequest(7); // Invalid dice roll > 6

    mockMvc
        .perform(
            post("/api/injuries/{id}/heal", injury.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 404 for non-existent injury")
  void shouldReturn404ForNonExistentInjury() throws Exception {
    mockMvc.perform(get("/api/injuries/{id}", 999L)).andExpect(status().isNotFound());
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

  private Injury createTestInjury(Wrestler wrestler, String name, InjurySeverity severity) {
    Injury injury = new Injury();
    injury.setWrestler(wrestler);
    injury.setName(name);
    injury.setDescription("Test injury description");
    injury.setSeverity(severity);
    injury.setHealthPenalty(severity.getRandomHealthPenalty());
    injury.setHealingCost(severity.getBaseHealingCost());
    injury.setIsActive(true);
    injury.setInjuryNotes("Test injury notes");
    return injuryRepository.save(injury);
  }
}
