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
package com.github.javydreamercsw.management.controller.injury;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

/**
 * Integration tests for InjuryController. Tests the complete REST API functionality for injury
 * management.
 */
@SpringBootTest
@DisplayName("InjuryController Integration Tests")
class InjuryControllerIT extends AbstractControllerTest {

  @Autowired private InjuryRepository injuryRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private DeckRepository deckRepository;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    // Delete in correct order to avoid foreign key constraint violations
    injuryRepository.deleteAll();
    deckRepository.deleteAll();
    wrestlerRepository.deleteAll();
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should create new injury successfully")
  void shouldCreateNewInjurySuccessfully() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);

    InjuryController.CreateInjuryRequest request =
        new InjuryController.CreateInjuryRequest(
            wrestler.getId(),
            "Broken Arm",
            "Severe fracture during match",
            InjurySeverity.SEVERE,
            "Out for 4 weeks");

    mockMvc
        .perform(
            post("/api/injuries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Broken Arm"))
        .andExpect(jsonPath("$.description").value("Severe fracture during match"))
        .andExpect(jsonPath("$.severity").value("SEVERE"))
        .andExpect(jsonPath("$.isActive").value(true));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should return 400 when creating injury for non-existent wrestler")
  void shouldReturn400WhenCreatingInjuryForNonExistentWrestler() throws Exception {
    InjuryController.CreateInjuryRequest request =
        new InjuryController.CreateInjuryRequest(
            999L, "Broken Arm", "Test description", InjurySeverity.MINOR, "Notes");

    mockMvc
        .perform(
            post("/api/injuries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should validate required fields when creating injury")
  void shouldValidateRequiredFieldsWhenCreatingInjury() throws Exception {
    InjuryController.CreateInjuryRequest request =
        new InjuryController.CreateInjuryRequest(null, "", null, null, null);

    mockMvc
        .perform(
            post("/api/injuries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should create injury from bumps successfully")
  void shouldCreateInjuryFromBumpsSuccessfully() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);
    wrestler.setBumps(5); // More than 3 bumps required
    wrestlerRepository.save(wrestler);

    mockMvc
        .perform(post("/api/injuries/from-bumps/{wrestlerId}", wrestler.getId()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.isActive").value(true));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should return 400 when wrestler has less than 3 bumps")
  void shouldReturn400WhenWrestlerHasLessThan3Bumps() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);
    wrestler.setBumps(2);
    wrestlerRepository.save(wrestler);

    mockMvc
        .perform(post("/api/injuries/from-bumps/{wrestlerId}", wrestler.getId()))
        .andExpect(status().isBadRequest());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should return 404 when creating injury from bumps for non-existent wrestler")
  void shouldReturn404WhenCreatingInjuryFromBumpsForNonExistentWrestler() throws Exception {
    mockMvc
        .perform(post("/api/injuries/from-bumps/{wrestlerId}", 999L))
        .andExpect(status().isNotFound());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should get all injuries with pagination")
  void shouldGetAllInjuriesWithPagination() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50_000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50_000L);

    createTestInjury(wrestler1, "Injury 1", InjurySeverity.MINOR);
    createTestInjury(wrestler2, "Injury 2", InjurySeverity.SEVERE);

    mockMvc
        .perform(get("/api/injuries").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should get injury by ID")
  void shouldGetInjuryById() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);
    Injury injury = createTestInjury(wrestler, "Test Injury", InjurySeverity.MODERATE);

    mockMvc
        .perform(get("/api/injuries/{id}", injury.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Injury"))
        .andExpect(jsonPath("$.severity").value("MODERATE"));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should return 404 for non-existent injury")
  void shouldReturn404ForNonExistentInjury() throws Exception {
    mockMvc.perform(get("/api/injuries/{id}", 999L)).andExpect(status().isNotFound());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should get active injuries for wrestler")
  void shouldGetActiveInjuriesForWrestler() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);

    createTestInjury(wrestler, "Active Injury", InjurySeverity.MINOR);
    Injury healedInjury = createTestInjury(wrestler, "Healed Injury", InjurySeverity.MINOR);
    healedInjury.heal();
    injuryRepository.save(healedInjury);

    mockMvc
        .perform(get("/api/injuries/wrestler/{wrestlerId}/active", wrestler.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Active Injury"));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should get all injuries for wrestler")
  void shouldGetAllInjuriesForWrestler() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);
    Wrestler otherWrestler = createTestWrestler("Other Wrestler", 30_000L);

    createTestInjury(wrestler, "Injury 1", InjurySeverity.MINOR);
    createTestInjury(wrestler, "Injury 2", InjurySeverity.SEVERE);
    createTestInjury(otherWrestler, "Other Injury", InjurySeverity.MODERATE);

    mockMvc
        .perform(get("/api/injuries/wrestler/{wrestlerId}", wrestler.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should get injuries by severity")
  void shouldGetInjuriesBySeverity() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50_000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50_000L);

    createTestInjury(wrestler1, "Severe Injury 1", InjurySeverity.SEVERE);
    createTestInjury(wrestler2, "Severe Injury 2", InjurySeverity.SEVERE);
    createTestInjury(wrestler1, "Minor Injury", InjurySeverity.MINOR);

    mockMvc
        .perform(get("/api/injuries/severity/{severity}", "SEVERE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should get all active injuries")
  void shouldGetAllActiveInjuries() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Wrestler 1", 50_000L);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2", 50_000L);

    createTestInjury(wrestler1, "Active Injury 1", InjurySeverity.MINOR);
    createTestInjury(wrestler2, "Active Injury 2", InjurySeverity.SEVERE);
    Injury healedInjury = createTestInjury(wrestler1, "Healed Injury", InjurySeverity.MODERATE);
    healedInjury.heal();
    injuryRepository.save(healedInjury);

    mockMvc
        .perform(get("/api/injuries/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should get wrestlers with active injuries")
  void shouldGetWrestlersWithActiveInjuries() throws Exception {
    Wrestler injuredWrestler1 = createTestWrestler("Injured Wrestler 1", 50_000L);
    Wrestler injuredWrestler2 = createTestWrestler("Injured Wrestler 2", 50_000L);
    createTestWrestler("Healthy Wrestler", 50_000L);

    createTestInjury(injuredWrestler1, "Injury 1", InjurySeverity.MINOR);
    createTestInjury(injuredWrestler2, "Injury 2", InjurySeverity.SEVERE);

    mockMvc
        .perform(get("/api/injuries/wrestlers-with-injuries"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should get healable injuries")
  void shouldGetHealableInjuries() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);

    createTestInjury(wrestler, "Healable Injury", InjurySeverity.MINOR);

    mockMvc
        .perform(get("/api/injuries/healable"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should attempt healing successfully")
  void shouldAttemptHealingSuccessfully() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 100_000L);
    Injury injury = createTestInjury(wrestler, "Test Injury", InjurySeverity.MINOR);

    InjuryController.HealingAttemptRequest request =
        new InjuryController.HealingAttemptRequest(6); // Best dice roll

    mockMvc
        .perform(
            post("/api/injuries/{id}/heal", injury.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").isBoolean())
        .andExpect(jsonPath("$.message").isString());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should validate dice roll when attempting healing")
  void shouldValidateDiceRollWhenAttemptingHealing() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 100_000L);
    Injury injury = createTestInjury(wrestler, "Test Injury", InjurySeverity.MINOR);

    // Intentionally invalid dice roll to test validation
    @SuppressWarnings("DataFlowIssue")
    InjuryController.HealingAttemptRequest request =
        new InjuryController.HealingAttemptRequest(7); // Invalid dice roll (must be 1-6)

    mockMvc
        .perform(
            post("/api/injuries/{id}/heal", injury.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should get total health penalty for wrestler")
  void shouldGetTotalHealthPenaltyForWrestler() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);

    createTestInjury(wrestler, "Injury 1", InjurySeverity.MINOR);
    createTestInjury(wrestler, "Injury 2", InjurySeverity.SEVERE);

    mockMvc
        .perform(get("/api/injuries/wrestler/{wrestlerId}/health-penalty", wrestler.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalHealthPenalty").isNumber());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should update injury successfully")
  void shouldUpdateInjurySuccessfully() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);
    Injury injury = createTestInjury(wrestler, "Original Name", InjurySeverity.MINOR);

    InjuryController.UpdateInjuryRequest request =
        new InjuryController.UpdateInjuryRequest(
            "Updated Name", "Updated description", "Updated notes");

    mockMvc
        .perform(
            put("/api/injuries/{id}", injury.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.description").value("Updated description"));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should return 404 when updating non-existent injury")
  void shouldReturn404WhenUpdatingNonExistentInjury() throws Exception {
    InjuryController.UpdateInjuryRequest request =
        new InjuryController.UpdateInjuryRequest("Name", "Description", "Notes");

    mockMvc
        .perform(
            put("/api/injuries/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should get injury statistics for wrestler")
  void shouldGetInjuryStatisticsForWrestler() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);

    createTestInjury(wrestler, "Injury 1", InjurySeverity.MINOR);
    createTestInjury(wrestler, "Injury 2", InjurySeverity.SEVERE);

    mockMvc
        .perform(get("/api/injuries/wrestler/{wrestlerId}/stats", wrestler.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.wrestlerId").value(wrestler.getId()))
        .andExpect(jsonPath("$.wrestlerName").value("Test Wrestler"))
        .andExpect(jsonPath("$.activeInjuries").value(2))
        .andExpect(jsonPath("$.healedInjuries").value(0))
        .andExpect(jsonPath("$.totalHealthPenalty").isNumber())
        .andExpect(jsonPath("$.effectiveHealth").isNumber())
        .andExpect(jsonPath("$.totalHealingCost").isNumber());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should return 404 for statistics of non-existent wrestler")
  void shouldReturn404ForStatisticsOfNonExistentWrestler() throws Exception {
    mockMvc
        .perform(get("/api/injuries/wrestler/{wrestlerId}/stats", 999L))
        .andExpect(status().isNotFound());
  }

  private Wrestler createTestWrestler(String name, Long fans) {
    Wrestler wrestler = Wrestler.builder().build();
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
    wrestler.setTier(WrestlerTier.ROOKIE);
    return wrestlerRepository.save(wrestler);
  }

  private Injury createTestInjury(Wrestler wrestler, String name, InjurySeverity severity) {
    Injury injury = new Injury();
    injury.setWrestler(wrestler);
    injury.setName(name);
    injury.setDescription("Test description for " + name);
    injury.setSeverity(severity);
    injury.setHealthPenalty(
        severity == InjurySeverity.SEVERE ? 10 : severity == InjurySeverity.MODERATE ? 5 : 2);
    injury.setIsActive(true);
    injury.setInjuryDate(java.time.Instant.now());
    injury.setHealingCost(10000L);
    injury.setCreationDate(java.time.Instant.now());
    return injuryRepository.save(injury);
  }
}
