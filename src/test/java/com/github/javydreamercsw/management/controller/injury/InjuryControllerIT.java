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
import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for InjuryController. Tests the complete REST API functionality for injury
 * management.
 */
@DisplayName("InjuryController Integration Tests")
@Transactional
class InjuryControllerIT extends AbstractRestControllerIT {

  @Autowired private InjuryService injuryService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private InjuryRepository injuryRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private WrestlerService wrestlerService;

  @BeforeEach
  public void setUp() {
    // Manually build MockMvc for this controller to bypass Vaadin servlet issues
    mockMvc =
        MockMvcBuilders.standaloneSetup(new InjuryController(injuryService, wrestlerRepository))
            .build();
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should create new injury successfully")
  void shouldCreateNewInjurySuccessfully() throws Exception {
    Wrestler wrestler = createTestWrestler("Test Wrestler");

    InjuryController.CreateInjuryRequest request =
        new InjuryController.CreateInjuryRequest(
            wrestler.getId(),
            defaultUniverse.getId(),
            "Broken Arm",
            "Severe arm injury",
            InjurySeverity.MODERATE,
            "Needs rest");

    mockMvc
        .perform(
            post("/api/injuries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Broken Arm"))
        .andExpect(jsonPath("$.isActive").value(true));
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
            post("/api/injuries/from-bumps/{wrestlerId}", wrestler.getId())
                .param("universeId", defaultUniverse.getId().toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.isActive").value(true));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should return 400 when wrestler has less than 3 bumps")
  void shouldReturn400WhenWrestlerHasLessThan3Bumps() throws Exception {
    Wrestler wrestler = createTestWrestler("Lucky Wrestler", 10_000L);
    WrestlerState state =
        wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());
    state.setBumps(2); // Less than 3 bumps
    wrestlerStateRepository.saveAndFlush(state);

    mockMvc
        .perform(
            post("/api/injuries/from-bumps/{wrestlerId}", wrestler.getId())
                .param("universeId", defaultUniverse.getId().toString()))
        .andExpect(status().isBadRequest());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("Should return 404 when creating injury from bumps for non-existent wrestler")
  void shouldReturn404WhenCreatingInjuryFromBumpsForNonExistentWrestler() throws Exception {
    mockMvc
        .perform(
            post("/api/injuries/from-bumps/{wrestlerId}", 999L)
                .param("universeId", defaultUniverse.getId().toString()))
        .andExpect(status().isNotFound());
  }

  public Wrestler createTestWrestler(@NonNull String name) {
    return createTestWrestler(name, 0L);
  }

  public Wrestler createTestWrestler(@NonNull String name, @NonNull Long fans) {
    Wrestler wrestler = new Wrestler();
    wrestler.setName(name);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(15);
    wrestler.setCurrentHealth(15);
    wrestler.setLowHealth(5);
    wrestler.setLowStamina(5);
    wrestler.setDeckSize(40);
    wrestler.setBumps(0);
    wrestler.setIsPlayer(true);
    wrestler.setTier(WrestlerTier.ROOKIE);
    Wrestler savedWrestler = wrestlerRepository.save(wrestler);

    WrestlerState state =
        WrestlerState.builder()
            .wrestler(savedWrestler)
            .universe(defaultUniverse)
            .fans(fans)
            .tier(WrestlerTier.fromFanCount(fans))
            .currentHealth(15)
            .bumps(0)
            .morale(100)
            .build();
    wrestlerStateRepository.save(state);

    return savedWrestler;
  }
}
