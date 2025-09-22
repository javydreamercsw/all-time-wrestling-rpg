package com.github.javydreamercsw.management.controller.title;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.controller.title.TitleController.CreateTitleRequest;
import com.github.javydreamercsw.management.controller.title.TitleController.UpdateTitleRequest;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for TitleController. Tests the complete REST API functionality for title
 * management.
 */
@DisplayName("TitleController Integration Tests")
class TitleControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TitleRepository titleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private DeckRepository deckRepository;

  @Test
  @DisplayName("Should create new title successfully")
  void shouldCreateNewTitleSuccessfully() throws Exception {
    CreateTitleRequest request =
        new CreateTitleRequest(
            "World Championship", "The top championship", WrestlerTier.MAIN_EVENTER);

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
    createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    createTestTitle("Extreme Championship", WrestlerTier.ROOKIE);

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
    Title inactiveTitle = createTestTitle("Inactive Championship", WrestlerTier.ROOKIE);
    inactiveTitle.setIsActive(false);
    titleRepository.save(inactiveTitle);

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
    createTestTitle("Vacant Championship", WrestlerTier.MAIN_EVENTER);
    Title heldTitle = createTestTitle("Held Championship", WrestlerTier.ROOKIE);

    // Award the second title to a wrestler
    Wrestler wrestler = createTestWrestler("Champion", 120000L);
    heldTitle.awardTitleTo(java.util.List.of(wrestler), Instant.now());
    titleRepository.save(heldTitle);

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
    createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    createTestTitle("Extreme Championship", WrestlerTier.ROOKIE);
    createTestTitle("Another World Title", WrestlerTier.MAIN_EVENTER);

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

    mockMvc
        .perform(post("/api/titles/{titleId}/award/{wrestlerId}", title.getId(), wrestler.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isVacant").value(false))
        .andExpect(jsonPath("$.currentChampions[0].name").value("Champion"));
  }

  @Test
  @DisplayName("Should not award title to ineligible wrestler")
  void shouldNotAwardTitleToIneligibleWrestler() throws Exception {
    Title title = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createTestWrestler("Rookie", 50000L); // Not eligible for World title

    mockMvc
        .perform(post("/api/titles/{titleId}/award/{wrestlerId}", title.getId(), wrestler.getId()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  @DisplayName("Should vacate title")
  void shouldVacateTitle() throws Exception {
    Title title = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler wrestler = createTestWrestler("Champion", 120000L);
    title.awardTitleTo(java.util.List.of(wrestler), Instant.now());
    titleRepository.save(title);

    mockMvc
        .perform(post("/api/titles/{id}/vacate", title.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isVacant").value(true))
        .andExpect(jsonPath("$.currentChampions").isEmpty());
  }

  @Test
  @DisplayName("Should handle successful title challenge")
  void shouldHandleSuccessfulTitleChallenge() throws Exception {
    Title title = createTestTitle("World Championship", WrestlerTier.MAIN_EVENTER);
    Wrestler challenger = createTestWrestler("Challenger", 120000L); // Has enough fans

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
    createTestWrestler("Eligible", 120000L);
    createTestWrestler("Ineligible", 50000L);

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
    titleRepository.save(title1);
    titleRepository.save(title2);

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
    titleRepository.save(title);

    mockMvc.perform(delete("/api/titles/{id}", title.getId())).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should not delete active title")
  void shouldNotDeleteActiveTitle() throws Exception {
    Title title = createTestTitle("Active Title", WrestlerTier.MAIN_EVENTER);

    mockMvc.perform(delete("/api/titles/{id}", title.getId())).andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Should get title statistics")
  void shouldGetTitleStatistics() throws Exception {
    Title title = createTestTitle("Test Championship", WrestlerTier.MAIN_EVENTER);

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
    title.setName(name);
    title.setDescription("Test description");
    title.setTier(tier);
    title.setIsActive(true);
    return titleRepository.save(title);
  }

  private Wrestler createTestWrestler(String name, Long fans) {
    Wrestler wrestler = new Wrestler();
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
    return wrestlerRepository.save(wrestler);
  }
}
