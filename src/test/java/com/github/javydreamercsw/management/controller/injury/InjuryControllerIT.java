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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for InjuryController. Tests the complete REST API functionality for injury
 * management.
 */
@SpringBootTest
@WithMockUser(authorities = {"ADMIN", "ROLE_ADMIN", "ROLE_BOOKER"})
@DisplayName("InjuryController Integration Tests")
class InjuryControllerIT extends AbstractIntegrationTest {

  @Autowired private WebApplicationContext context;
  private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private InjuryRepository injuryRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private WrestlerService wrestlerService;

  private Universe defaultUniverse;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Delete in correct order to avoid foreign key constraint violations
    injuryRepository.deleteAll();
    deckRepository.deleteAll();
    wrestlerStateRepository.deleteAll();
    wrestlerRepository.deleteAll();
    universeRepository.deleteAll();

    defaultUniverse =
        universeRepository.save(Universe.builder().id(1L).name("Default Universe").build());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should create new injury successfully")
  void shouldCreateNewInjurySuccessfully() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);

    InjuryController.CreateInjuryRequest request =
        new InjuryController.CreateInjuryRequest(
            wrestler.getId(),
            defaultUniverse.getId(),
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
            999L,
            defaultUniverse.getId(),
            "Broken Arm",
            "Test description",
            InjurySeverity.MINOR,
            "Notes");

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
        new InjuryController.CreateInjuryRequest(
            null, defaultUniverse.getId(), "", null, null, null);

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
    WrestlerState state =
        wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());
    state.setBumps(5); // More than 3 bumps required
    wrestlerStateRepository.saveAndFlush(state);

    mockMvc
        .perform(
            post(
                "/api/injuries/from-bumps/{wrestlerId}/{universeId}",
                wrestler.getId(),
                defaultUniverse.getId()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.isActive").value(true));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should return 400 when wrestler has less than 3 bumps")
  void shouldReturn400WhenWrestlerHasLessThan3Bumps() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler", 50_000L);
    WrestlerState state =
        wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());
    state.setBumps(2);
    wrestlerStateRepository.saveAndFlush(state);

    mockMvc
        .perform(
            post(
                "/api/injuries/from-bumps/{wrestlerId}/{universeId}",
                wrestler.getId(),
                defaultUniverse.getId()))
        .andExpect(status().isBadRequest());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should return 404 when creating injury from bumps for non-existent wrestler")
  void shouldReturn404WhenCreatingInjuryFromBumpsForNonExistentWrestler() throws Exception {
    mockMvc
        .perform(
            post(
                "/api/injuries/from-bumps/{wrestlerId}/{universeId}",
                999L,
                defaultUniverse.getId()))
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

  private Wrestler createTestWrestler(String name, Long fans) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setName(name);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(15);
    wrestler.setCurrentHealth(15);
    wrestler.setLowHealth(5);
    wrestler.setLowStamina(5);
    wrestler.setDeckSize(40);
    wrestler.setIsPlayer(true);
    wrestler = wrestlerRepository.save(wrestler);

    WrestlerState state =
        wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());
    state.setFans(fans);
    state.setTier(WrestlerTier.ROOKIE);
    state.setBumps(0);
    wrestlerStateRepository.saveAndFlush(state);

    return wrestler;
  }

  private Injury createTestInjury(Wrestler wrestler, String name, InjurySeverity severity) {
    Injury injury = new Injury();
    injury.setWrestler(wrestler);
    injury.setUniverse(defaultUniverse);
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
