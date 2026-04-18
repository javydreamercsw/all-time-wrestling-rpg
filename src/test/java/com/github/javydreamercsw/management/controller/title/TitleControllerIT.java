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
package com.github.javydreamercsw.management.controller.title;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.controller.title.TitleController.CreateTitleRequest;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.util.ArrayList;
import lombok.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration tests for TitleController. Tests the complete REST API functionality for title
 * management.
 */
@DisplayName("TitleController Integration Tests")
class TitleControllerIT extends AbstractControllerTest {

  @MockitoBean private TitleService titleService;
  @MockitoBean private WrestlerService wrestlerService;

  @Test
  @DisplayName("Should create new title successfully")
  void shouldCreateNewTitleSuccessfully() throws Exception {
    CreateTitleRequest request =
        new CreateTitleRequest(
            "World Championship",
            "The top championship",
            WrestlerTier.MAIN_EVENTER,
            ChampionshipType.SINGLE,
            Gender.MALE,
            1L);

    Title createdTitle = new Title();
    createdTitle.setId(1L);
    createdTitle.setName(request.name());
    createdTitle.setDescription(request.description());
    createdTitle.setTier(request.tier());
    createdTitle.setGender(request.gender());
    createdTitle.setIsActive(true);

    when(titleService.createTitle(
            anyString(),
            anyString(),
            any(WrestlerTier.class),
            any(ChampionshipType.class),
            any(Gender.class),
            anyLong()))
        .thenReturn(createdTitle);

    mockMvc
        .perform(
            post("/api/titles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("World Championship"))
        .andExpect(jsonPath("$.description").value("The top championship"))
        .andExpect(jsonPath("$.tier").value("MAIN_EVENTER"))
        .andExpect(jsonPath("$.isActive").value(true))
        .andExpect(jsonPath("$.isVacant").value(true));
  }

  @Test
  @DisplayName("Should prevent duplicate title names")
  void shouldPreventDuplicateTitleNames() throws Exception {
    // Create first title
    createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    when(titleService.titleNameExists("World Championship")).thenReturn(true);

    // Try to create duplicate
    CreateTitleRequest request =
        new CreateTitleRequest(
            "World Championship",
            "Duplicate title",
            WrestlerTier.ROOKIE,
            ChampionshipType.SINGLE,
            Gender.MALE,
            1L);

    mockMvc
        .perform(
            post("/api/titles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Title name already exists: World Championship"));
  }

  @Test
  @DisplayName("Should validate required fields when creating title")
  void shouldValidateRequiredFieldsWhenCreatingTitle() throws Exception {
    CreateTitleRequest request = new CreateTitleRequest("", null, null, null, null, 1L);

    mockMvc
        .perform(
            post("/api/titles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should handle successful title challenge")
  void shouldHandleSuccessfulTitleChallenge() throws Exception {
    Title title = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createTestWrestler("Challenger", 120000L); // Has enough fans

    Assertions.assertNotNull(title.getId());
    Assertions.assertNotNull(challenger.getId());
    when(titleService.addChallengerToTitle(title.getId(), challenger.getId()))
        .thenReturn(new TitleService.ChallengeResult(true, "Challenge accepted"));

    mockMvc
        .perform(
            post(
                "/api/titles/{titleId}/challenger/{wrestlerId}", title.getId(), challenger.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Challenge accepted"));
  }

  private static long nextTitleId = 1L;

  private Title createTestTitle(@NonNull String name, @NonNull WrestlerTier tier) {
    Title title = new Title();
    title.setId(nextTitleId++); // Assign a unique ID for the mock
    title.setName(name);
    title.setDescription("Test description");
    title.setTier(tier);
    title.setIsActive(true);
    title.setCreationDate(Instant.now());
    title.setChampion(new ArrayList<>());
    title.setChallengers(new ArrayList<>());
    title.setChampionshipType(ChampionshipType.SINGLE);
    return title;
  }

  private Wrestler createTestWrestler(@NonNull String name, @NonNull Long fans) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setId(1L); // Assign a dummy ID for the mock
    wrestler.setName(name);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(15);
    wrestler.setCurrentHealth(15);
    wrestler.setLowHealth(5); // Set required low health
    wrestler.setLowStamina(5); // Set required low stamina
    wrestler.setDeckSize(40); // Set required deck size
    wrestler.setIsPlayer(true);

    WrestlerState state = WrestlerState.builder().wrestler(wrestler).fans(fans).build();
    lenient().when(wrestlerService.getOrCreateState(eq(1L), anyLong())).thenReturn(state);

    return wrestler;
  }
}
