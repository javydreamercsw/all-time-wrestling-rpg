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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.test.BaseControllerTest;
import com.github.javydreamercsw.management.controller.title.TitleController.CreateTitleRequest;
import com.github.javydreamercsw.management.controller.title.TitleController.UpdateTitleRequest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for TitleController. Tests the complete REST API functionality for title
 * management.
 */
@WebMvcTest(
    controllers = TitleController.class,
    excludeAutoConfiguration = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
@DisplayName("TitleController Integration Tests")
class TitleControllerIntegrationTest extends BaseControllerTest {

  @Autowired private WebApplicationContext webApplicationContext;
  private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private TitleService titleService;
  @MockitoBean private WrestlerService wrestlerService;

  @MockitoBean
  private com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository
      wrestlerRepository;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
  }

  @Test
  @DisplayName("Should create new title successfully")
  void shouldCreateNewTitleSuccessfully() throws Exception {
    CreateTitleRequest request =
        new CreateTitleRequest(
            "World Championship", "The top championship", WrestlerTier.MAIN_EVENTER);

    Title createdTitle = new Title();
    createdTitle.setId(1L);
    createdTitle.setName(request.name());
    createdTitle.setDescription(request.description());
    createdTitle.setTier(request.tier());
    createdTitle.setIsActive(true);

    when(titleService.createTitle(anyString(), anyString(), any(WrestlerTier.class)))
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
    Title existingTitle = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    when(titleService.titleNameExists("World Championship")).thenReturn(true);

    // Try to create duplicate
    CreateTitleRequest request =
        new CreateTitleRequest("World Championship", "Duplicate title", WrestlerTier.ROOKIE);

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
    CreateTitleRequest request =
        new CreateTitleRequest(
            "", // Invalid empty name
            null,
            null // Invalid null tier
            );

    mockMvc
        .perform(
            post("/api/titles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should get all titles with pagination")
  void shouldGetAllTitlesWithPagination() throws Exception {
    // Create test titles
    Title title1 = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Title title2 = createTestTitle("Extreme Championship", WrestlerTier.ROOKIE);
    when(titleService.getAllTitles(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(title1, title2)));

    mockMvc
        .perform(get("/api/titles").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  @DisplayName("Should get title by ID")
  void shouldGetTitleById() throws Exception {
    Title title = createTestTitle("Test Championship", WrestlerTier.MAIN_EVENTER);
    when(titleService.getTitleById(anyLong())).thenReturn(Optional.of(title));

    mockMvc
        .perform(get("/api/titles/{id}", title.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Championship"))
        .andExpect(jsonPath("$.tier").value("MAIN_EVENTER"));
  }

  @Test
  @DisplayName("Should get active titles")
  void shouldGetActiveTitles() throws Exception {
    Title activeTitle = createTestTitle("Active Championship", WrestlerTier.MAIN_EVENTER);
    activeTitle.setIsActive(true);
    // No need to save to repository, as createTestTitle already sets up the mock for save

    Title inactiveTitle = createTestTitle("Inactive Championship", WrestlerTier.ROOKIE);
    inactiveTitle.setIsActive(false);
    // No need to save to repository, as createTestTitle already sets up the mock for save

    when(titleService.getActiveTitles()).thenReturn(List.of(activeTitle));

    mockMvc
        .perform(get("/api/titles/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Active Championship"));
  }

  @Test
  @DisplayName("Should get vacant titles")
  void shouldGetVacantTitles() throws Exception {
    Title vacantTitle = createTestTitle("Vacant Championship", WrestlerTier.MAIN_EVENTER);
    Title heldTitle = createTestTitle("Held Championship", WrestlerTier.ROOKIE);

    // Award the second title to a wrestler
    Wrestler wrestler = createTestWrestler("Champion", 120000L); // Eligible for World title
    heldTitle.awardTitleTo(java.util.List.of(wrestler), Instant.now());
    // Mock the behavior of titleService.save when heldTitle is saved after being awarded
    when(titleService.save(heldTitle)).thenReturn(heldTitle);

    when(titleService.getVacantTitles()).thenReturn(List.of(vacantTitle));

    mockMvc
        .perform(get("/api/titles/vacant"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Vacant Championship"));
  }

  @Test
  @DisplayName("Should get titles by tier")
  void shouldGetTitlesByTier() throws Exception {
    Title title1 = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Title title2 = createTestTitle("Extreme Championship", WrestlerTier.ROOKIE);
    Title title3 = createTestTitle("Another World Title", WrestlerTier.MAIN_EVENTER);

    when(titleService.getTitlesByTier(WrestlerTier.MAIN_EVENTER))
        .thenReturn(List.of(title1, title3));

    mockMvc
        .perform(get("/api/titles/tier/{tier}", WrestlerTier.MAIN_EVENTER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("Should award title to eligible wrestler")
  void shouldAwardTitleToEligibleWrestler() throws Exception {
    Title title = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createTestWrestler("Champion", 120000L); // Eligible for World title

    when(titleService.getTitleById(title.getId())).thenReturn(Optional.of(title));
    when(wrestlerRepository.findAllById(List.of(wrestler.getId()))).thenReturn(List.of(wrestler));
    doAnswer(
            invocation -> {
              Title titleToAward = invocation.getArgument(0);
              List<Wrestler> wrestlers = invocation.getArgument(1);
              titleToAward.awardTitleTo(wrestlers, Instant.now());
              return null;
            })
        .when(titleService)
        .awardTitleTo(any(Title.class), anyList());

    mockMvc
        .perform(
            post("/api/titles/{titleId}/award", title.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(wrestler.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isVacant").value(false))
        .andExpect(jsonPath("$.currentChampions[0].name").value("Champion"));
  }

  @Test
  @DisplayName("Should not award title to ineligible wrestler")
  void shouldNotAwardTitleToIneligibleWrestler() throws Exception {
    Title title = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createTestWrestler("Rookie", 50000L); // Not eligible for World title

    when(titleService.getTitleById(title.getId())).thenReturn(Optional.of(title));
    when(wrestlerRepository.findAllById(List.of(wrestler.getId()))).thenReturn(List.of(wrestler));
    doThrow(new IllegalArgumentException("Wrestler not eligible"))
        .when(titleService)
        .awardTitleTo(any(Title.class), anyList());

    mockMvc
        .perform(
            post("/api/titles/{titleId}/award", title.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(wrestler.getId()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  @DisplayName("Should vacate title")
  void shouldVacateTitle() throws Exception {
    Title awardedTitle = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createTestWrestler("Champion", 120000L);
    awardedTitle.awardTitleTo(List.of(wrestler), Instant.now());

    Title vacatedTitle = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    vacatedTitle.vacateTitle();

    when(titleService.getTitleById(awardedTitle.getId())).thenReturn(Optional.of(awardedTitle));
    when(titleService.vacateTitle(awardedTitle.getId())).thenReturn(Optional.of(vacatedTitle));

    mockMvc
        .perform(post("/api/titles/{id}/vacate", awardedTitle.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isVacant").value(true))
        .andExpect(jsonPath("$.currentChampions").isEmpty());
  }

  @Test
  @DisplayName("Should handle successful title challenge")
  void shouldHandleSuccessfulTitleChallenge() throws Exception {
    Title title = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createTestWrestler("Challenger", 120000L); // Has enough fans

    when(titleService.challengeForTitle(challenger.getId(), title.getId()))
        .thenReturn(
            new TitleService.ChallengeResult(true, "Challenge accepted", title, challenger));

    mockMvc
        .perform(
            post("/api/titles/{titleId}/challenge/{wrestlerId}", title.getId(), challenger.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Challenge accepted"));
  }

  @Test
  @DisplayName("Should handle failed title challenge")
  void shouldHandleFailedTitleChallenge() throws Exception {
    Title title = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createTestWrestler("Poor Challenger", 50000L); // Not eligible

    when(titleService.challengeForTitle(challenger.getId(), title.getId()))
        .thenReturn(
            new TitleService.ChallengeResult(false, "Wrestler not eligible", title, challenger));

    mockMvc
        .perform(
            post("/api/titles/{titleId}/challenge/{wrestlerId}", title.getId(), challenger.getId()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  @DisplayName("Should get eligible challengers")
  void shouldGetEligibleChallengers() throws Exception {
    Title title = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler eligibleWrestler = createTestWrestler("Eligible", 120000L);
    createTestWrestler("Ineligible", 50000L);

    when(titleService.getEligibleChallengers(title.getId())).thenReturn(List.of(eligibleWrestler));

    mockMvc
        .perform(get("/api/titles/{id}/eligible-challengers", title.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Eligible"));
  }

  @Test
  @DisplayName("Should get titles held by wrestler")
  void shouldGetTitlesHeldByWrestler() throws Exception {
    Wrestler wrestler = createTestWrestler("Champion", 120000L);
    Title title1 = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Title title2 = createTestTitle("Extreme Championship", WrestlerTier.ROOKIE);

    title1.awardTitleTo(java.util.List.of(wrestler), Instant.now());
    title2.awardTitleTo(java.util.List.of(wrestler), Instant.now());
    when(titleService.save(title1)).thenReturn(title1);
    when(titleService.save(title2)).thenReturn(title2);

    when(titleService.getTitlesHeldBy(wrestler.getId())).thenReturn(List.of(title1, title2));

    mockMvc
        .perform(get("/api/titles/wrestler/{wrestlerId}", wrestler.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("Should update title")
  void shouldUpdateTitle() throws Exception {
    Title title = createTestTitle("Original Name", WrestlerTier.MAIN_EVENTER);

    UpdateTitleRequest request =
        new UpdateTitleRequest("Updated Name", "Updated description", false);

    Title updatedTitle = new Title();
    updatedTitle.setId(title.getId());
    updatedTitle.setName(request.name());
    updatedTitle.setDescription(request.description());
    updatedTitle.setIsActive(request.isActive());
    updatedTitle.setTier(title.getTier());

    when(titleService.updateTitle(anyLong(), anyString(), anyString(), anyBoolean()))
        .thenReturn(Optional.of(updatedTitle));

    mockMvc
        .perform(
            put("/api/titles/{id}", title.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.description").value("Updated description"))
        .andExpect(jsonPath("$.isActive").value(false));
  }

  @Test
  @DisplayName("Should delete inactive vacant title")
  void shouldDeleteInactiveVacantTitle() throws Exception {
    Title title = createTestTitle("Test Title", WrestlerTier.MAIN_EVENTER);
    title.setIsActive(false);
    // No need to save to repository, as createTestTitle already sets up the mock for save

    when(titleService.deleteTitle(title.getId())).thenReturn(true);

    mockMvc.perform(delete("/api/titles/{id}", title.getId())).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should not delete active title")
  void shouldNotDeleteActiveTitle() throws Exception {
    Title title = createTestTitle("Active Title", WrestlerTier.MAIN_EVENTER);

    when(titleService.deleteTitle(title.getId())).thenReturn(false);

    mockMvc.perform(delete("/api/titles/{id}", title.getId())).andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Should get title statistics")
  void shouldGetTitleStatistics() throws Exception {
    Title title = createTestTitle("Test Championship", WrestlerTier.MAIN_EVENTER);

    TitleService.TitleStats stats =
        new TitleService.TitleStats(
            title.getId(),
            title.getName(),
            title.getTier(),
            title.isVacant(),
            null, // currentChampion (assuming vacant for this test setup)
            0L, // currentReignDays
            0, // totalReigns
            title.getIsActive());

    when(titleService.getTitleStats(title.getId())).thenReturn(stats);

    mockMvc
        .perform(get("/api/titles/{id}/stats", title.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.titleId").value(title.getId()))
        .andExpect(jsonPath("$.name").value("Test Championship"))
        .andExpect(jsonPath("$.tier").value("MAIN_EVENTER"))
        .andExpect(jsonPath("$.isVacant").value(true))
        .andExpect(jsonPath("$.isActive").value(true));
  }

  private Title createTestTitle(String name, WrestlerTier tier) {
    Title title = new Title();
    title.setId(1L); // Assign a dummy ID for the mock
    title.setName(name);
    title.setDescription("Test description");
    title.setTier(tier);
    title.setIsActive(true);
    when(titleService.save(any(Title.class))).thenReturn(title);
    return title;
  }

  private Wrestler createTestWrestler(String name, Long fans) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setId(1L); // Assign a dummy ID for the mock
    wrestler.setName(name);
    wrestler.setFans(fans);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(15);
    wrestler.setCurrentHealth(15);
    wrestler.setLowHealth(5); // Set required low health
    wrestler.setLowStamina(5); // Set required low stamina
    wrestler.setDeckSize(40); // Set required deck size
    wrestler.setBumps(0);
    wrestler.setIsPlayer(true);
    wrestler.updateTier();
    when(wrestlerService.save(any(Wrestler.class))).thenReturn(wrestler);
    return wrestler;
  }
}
