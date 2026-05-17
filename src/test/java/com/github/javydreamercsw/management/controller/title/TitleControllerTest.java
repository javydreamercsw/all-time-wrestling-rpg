/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class TitleControllerTest extends AbstractControllerTest {

  @MockitoBean private TitleService titleService;
  @MockitoBean private WrestlerRepository wrestlerRepository;

  // ==================== POST /api/titles ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should create title successfully")
  void createTitle() throws Exception {
    Title title = new Title();
    title.setId(1L);
    title.setName("Test Title");
    title.setTier(WrestlerTier.MIDCARDER);

    when(titleService.titleNameExists(any())).thenReturn(false);
    when(titleService.createTitle(any(), any(), any(), any(), any(), any())).thenReturn(title);

    TitleController.CreateTitleRequest request =
        new TitleController.CreateTitleRequest(
            "Test Title",
            "Description",
            WrestlerTier.MIDCARDER,
            ChampionshipType.SINGLE,
            Gender.MALE,
            1L);

    mockMvc
        .perform(
            post("/api/titles")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Title"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should return 409 when title name already exists")
  void createTitle_conflictWhenNameExists() throws Exception {
    when(titleService.titleNameExists("Existing Title")).thenReturn(true);

    TitleController.CreateTitleRequest request =
        new TitleController.CreateTitleRequest(
            "Existing Title",
            "Description",
            WrestlerTier.MIDCARDER,
            ChampionshipType.SINGLE,
            Gender.MALE,
            1L);

    mockMvc
        .perform(
            post("/api/titles")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Title name already exists: Existing Title"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should return 400 when create request has blank name")
  void createTitle_badRequestWhenNameIsBlank() throws Exception {
    TitleController.CreateTitleRequest request =
        new TitleController.CreateTitleRequest(
            "", "Description", WrestlerTier.MIDCARDER, ChampionshipType.SINGLE, Gender.MALE, 1L);

    mockMvc
        .perform(
            post("/api/titles")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ==================== GET /api/titles ====================

  @Test
  @DisplayName("Should get all titles with pagination")
  void getAllTitles_returnsPagedResults() throws Exception {
    Title title = createSampleTitle();
    Page<Title> page = new PageImpl<>(List.of(title), PageRequest.of(0, 20), 1);
    when(titleService.getAllTitles(any())).thenReturn(page);

    mockMvc
        .perform(get("/api/titles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("World Championship"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("Should get all titles with custom pagination parameters")
  void getAllTitles_withCustomPaginationParams() throws Exception {
    Page<Title> page = new PageImpl<>(List.of(), PageRequest.of(1, 5), 0);
    when(titleService.getAllTitles(any())).thenReturn(page);

    mockMvc
        .perform(get("/api/titles").param("page", "1").param("size", "5").param("sortBy", "tier"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  // ==================== GET /api/titles/{id} ====================

  @Test
  @DisplayName("Should get title by ID")
  void getTitleById_found() throws Exception {
    Title title = createSampleTitle();
    when(titleService.getTitleById(1L)).thenReturn(Optional.of(title));

    mockMvc
        .perform(get("/api/titles/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("World Championship"))
        .andExpect(jsonPath("$.tier").value("MAIN_EVENTER"));
  }

  @Test
  @DisplayName("Should return 404 when title not found by ID")
  void getTitleById_notFound() throws Exception {
    when(titleService.getTitleById(999L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/titles/999")).andExpect(status().isNotFound());
  }

  // ==================== GET /api/titles/active ====================

  @Test
  @DisplayName("Should get active titles")
  void getActiveTitles_returnsActiveTitles() throws Exception {
    Title title = createSampleTitle();
    when(titleService.getActiveTitles()).thenReturn(List.of(title));

    mockMvc
        .perform(get("/api/titles/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("World Championship"));
  }

  @Test
  @DisplayName("Should return empty list when no active titles")
  void getActiveTitles_returnsEmptyList() throws Exception {
    when(titleService.getActiveTitles()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/titles/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // ==================== GET /api/titles/vacant ====================

  @Test
  @DisplayName("Should get vacant titles")
  void getVacantTitles_returnsVacantTitles() throws Exception {
    Title title = createSampleTitle();
    when(titleService.getVacantTitles()).thenReturn(List.of(title));

    mockMvc
        .perform(get("/api/titles/vacant"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1));
  }

  // ==================== GET /api/titles/tier/{tier} ====================

  @Test
  @DisplayName("Should get titles by tier")
  void getTitlesByTier_returnsTitlesForTier() throws Exception {
    Title title = createSampleTitle();
    when(titleService.getTitlesByTier(WrestlerTier.MAIN_EVENTER)).thenReturn(List.of(title));

    mockMvc
        .perform(get("/api/titles/tier/MAIN_EVENTER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("World Championship"));
  }

  @Test
  @DisplayName("Should return empty list when no titles for tier")
  void getTitlesByTier_returnsEmptyListWhenNoneFound() throws Exception {
    when(titleService.getTitlesByTier(WrestlerTier.ROOKIE)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/titles/tier/ROOKIE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // ==================== POST /api/titles/{titleId}/award ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should award title to wrestler successfully")
  void awardTitle_success() throws Exception {
    Title title = createSampleTitle();
    Wrestler wrestler = createSampleWrestler(10L, "The Champion");

    when(titleService.getTitleById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(wrestler));

    mockMvc
        .perform(
            post("/api/titles/1/award")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(10L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("World Championship"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should return 404 when awarding title that does not exist")
  void awardTitle_titleNotFound() throws Exception {
    when(titleService.getTitleById(999L)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/api/titles/999/award")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(10L))))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should award title even when wrestler ID is not found (skipped silently)")
  void awardTitle_wrestlerNotFound_skipsAndSucceeds() throws Exception {
    Title title = createSampleTitle();

    when(titleService.getTitleById(1L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(anyLong())).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/api/titles/1/award")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(999L))))
        .andExpect(status().isOk());
  }

  // ==================== POST /api/titles/{id}/vacate ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should vacate title successfully")
  void vacateTitle_success() throws Exception {
    Title title = createSampleTitle();
    when(titleService.vacateTitle(1L)).thenReturn(Optional.of(title));

    mockMvc
        .perform(post("/api/titles/1/vacate").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("World Championship"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should return 404 when vacating title that does not exist")
  void vacateTitle_notFound() throws Exception {
    when(titleService.vacateTitle(999L)).thenReturn(Optional.empty());

    mockMvc.perform(post("/api/titles/999/vacate").with(csrf())).andExpect(status().isNotFound());
  }

  // ==================== POST /api/titles/{titleId}/challenger/{wrestlerId} ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should add challenger to title successfully")
  void addChallengerToTitle_success() throws Exception {
    TitleService.ChallengeResult result =
        new TitleService.ChallengeResult(true, "John Doe has been added as a challenger");
    when(titleService.addChallengerToTitle(1L, 10L)).thenReturn(result);

    mockMvc
        .perform(post("/api/titles/1/challenger/10").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("John Doe has been added as a challenger"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should return 400 when adding challenger fails eligibility check")
  void addChallengerToTitle_notEligible() throws Exception {
    TitleService.ChallengeResult result =
        new TitleService.ChallengeResult(false, "Wrestler is not eligible for this title.");
    when(titleService.addChallengerToTitle(1L, 10L)).thenReturn(result);

    mockMvc
        .perform(post("/api/titles/1/challenger/10").with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Wrestler is not eligible for this title."));
  }

  // ==================== DELETE /api/titles/{titleId}/challenger/{wrestlerId} ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should remove challenger from title successfully")
  void removeChallengerFromTitle_success() throws Exception {
    TitleService.ChallengeResult result =
        new TitleService.ChallengeResult(true, "John Doe is no longer a challenger");
    when(titleService.removeChallengerFromTitle(1L, 10L)).thenReturn(result);

    mockMvc
        .perform(delete("/api/titles/1/challenger/10").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should return 400 when wrestler is not a challenger for the title")
  void removeChallengerFromTitle_notAChallenger() throws Exception {
    TitleService.ChallengeResult result =
        new TitleService.ChallengeResult(false, "Wrestler is not a challenger for this title.");
    when(titleService.removeChallengerFromTitle(1L, 10L)).thenReturn(result);

    mockMvc
        .perform(delete("/api/titles/1/challenger/10").with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  // ==================== GET /api/titles/{id}/eligible-challengers ====================

  @Test
  @DisplayName("Should get eligible challengers for a title")
  void getEligibleChallengers_returnsChallengers() throws Exception {
    Wrestler wrestler = createSampleWrestler(10L, "Eligible Wrestler");
    when(titleService.getEligibleChallengers(1L)).thenReturn(List.of(wrestler));

    mockMvc
        .perform(get("/api/titles/1/eligible-challengers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Eligible Wrestler"));
  }

  @Test
  @DisplayName("Should return empty list when no eligible challengers")
  void getEligibleChallengers_returnsEmptyList() throws Exception {
    when(titleService.getEligibleChallengers(1L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/titles/1/eligible-challengers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // ==================== GET /api/titles/wrestler/{wrestlerId} ====================

  @Test
  @DisplayName("Should get titles held by a wrestler")
  void getTitlesHeldBy_returnsTitles() throws Exception {
    Title title = createSampleTitle();
    when(titleService.getTitlesHeldBy(10L)).thenReturn(List.of(title));

    mockMvc
        .perform(get("/api/titles/wrestler/10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("World Championship"));
  }

  @Test
  @DisplayName("Should return empty list when wrestler holds no titles")
  void getTitlesHeldBy_returnsEmptyList() throws Exception {
    when(titleService.getTitlesHeldBy(10L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/titles/wrestler/10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // ==================== PUT /api/titles/{id} ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should update title successfully")
  void updateTitle_success() throws Exception {
    Title updated = createSampleTitle();
    updated.setName("Updated Championship");

    when(titleService.updateTitle(eq(1L), any(), any(), any(), any()))
        .thenReturn(Optional.of(updated));

    TitleController.UpdateTitleRequest request =
        new TitleController.UpdateTitleRequest(
            "Updated Championship", "New desc", true, Gender.MALE);

    mockMvc
        .perform(
            put("/api/titles/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Championship"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should return 404 when updating non-existent title")
  void updateTitle_notFound() throws Exception {
    when(titleService.updateTitle(eq(999L), any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    TitleController.UpdateTitleRequest request =
        new TitleController.UpdateTitleRequest("Updated Championship", "New desc", true, null);

    mockMvc
        .perform(
            put("/api/titles/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  // ==================== DELETE /api/titles/{id} ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should delete title successfully")
  void deleteTitle_success() throws Exception {
    when(titleService.deleteTitle(1L)).thenReturn(true);

    mockMvc.perform(delete("/api/titles/1").with(csrf())).andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  @DisplayName("Should return 409 when title cannot be deleted (active or has champion)")
  void deleteTitle_conflict() throws Exception {
    when(titleService.deleteTitle(1L)).thenReturn(false);

    mockMvc.perform(delete("/api/titles/1").with(csrf())).andExpect(status().isConflict());
  }

  // ==================== GET /api/titles/{id}/stats ====================

  @Test
  @DisplayName("Should get title statistics")
  void getTitleStats_found() throws Exception {
    TitleService.TitleStats stats = new TitleService.TitleStats("World Championship", 10, 120L, 1);
    when(titleService.getTitleStats(1L)).thenReturn(stats);

    mockMvc
        .perform(get("/api/titles/1/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.titleName").value("World Championship"))
        .andExpect(jsonPath("$.totalReigns").value(10))
        .andExpect(jsonPath("$.currentReignDays").value(120))
        .andExpect(jsonPath("$.currentChampionsCount").value(1));
  }

  @Test
  @DisplayName("Should return 404 when title stats not found")
  void getTitleStats_notFound() throws Exception {
    when(titleService.getTitleStats(999L)).thenReturn(null);

    mockMvc.perform(get("/api/titles/999/stats")).andExpect(status().isNotFound());
  }

  // ==================== Helper Methods ====================

  private Title createSampleTitle() {
    Title title = new Title();
    title.setId(1L);
    title.setName("World Championship");
    title.setTier(WrestlerTier.MAIN_EVENTER);
    title.setChampionshipType(ChampionshipType.SINGLE);
    title.setIsActive(true);
    return title;
  }

  private Wrestler createSampleWrestler(final long id, final String name) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setId(id);
    wrestler.setName(name);
    return wrestler;
  }
}
