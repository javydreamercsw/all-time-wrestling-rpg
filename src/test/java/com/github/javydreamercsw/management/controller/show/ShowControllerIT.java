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
package com.github.javydreamercsw.management.controller.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for ShowController. Tests the complete REST API functionality for show
 * management.
 */
@DisplayName("ShowController Integration Tests")
@Transactional
class ShowControllerIT extends AbstractRestControllerIT {

  private ShowType testShowType;

  @BeforeEach
  public void setUp() {
    // Manually build MockMvc to bypass Vaadin servlet issues
    mockMvc = MockMvcBuilders.standaloneSetup(new ShowController(showService)).build();

    showRepository.deleteAll();
    showRepository.flush();

    testShowType = showTypeService.createOrUpdateShowType("Weekly IT Show", "Weekly Show", 5, 2);
  }

  @Test
  @DisplayName("Should return paginated list of shows with 200")
  void shouldReturnPaginatedListOfShows() throws Exception {
    saveShow("Raw", LocalDate.now());
    saveShow("SmackDown", LocalDate.now().plusDays(3));

    mockMvc
        .perform(get("/api/shows"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  @DisplayName("Should return show by ID with 200")
  void shouldReturnShowByIdWith200() throws Exception {
    Show show = saveShow("Raw", LocalDate.now());

    mockMvc
        .perform(get("/api/shows/{id}", show.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Raw"));
  }

  @Test
  @DisplayName("Should return 404 when show not found by ID")
  void shouldReturn404WhenShowNotFoundById() throws Exception {
    mockMvc.perform(get("/api/shows/{id}", 999999L)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should create show and return 201")
  void shouldCreateShowAndReturn201() throws Exception {
    ShowController.CreateShowRequest request =
        new ShowController.CreateShowRequest(
            "WrestleMania",
            "The showcase of the immortals",
            testShowType.getId(),
            LocalDate.now().plusDays(30),
            null,
            null,
            null,
            null,
            null);

    mockMvc
        .perform(
            post("/api/shows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("WrestleMania"))
        .andExpect(jsonPath("$.description").value("The showcase of the immortals"));
  }

  @Test
  @DisplayName("Should return 400 when name is missing on create")
  void shouldReturn400WhenNameIsMissingOnCreate() throws Exception {
    ShowController.CreateShowRequest request =
        new ShowController.CreateShowRequest(
            null,
            "Some description",
            testShowType.getId(),
            LocalDate.now(),
            null,
            null,
            null,
            null,
            null);

    mockMvc
        .perform(
            post("/api/shows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when description is missing on create")
  void shouldReturn400WhenDescriptionIsMissingOnCreate() throws Exception {
    ShowController.CreateShowRequest request =
        new ShowController.CreateShowRequest(
            "Some Show", null, testShowType.getId(), LocalDate.now(), null, null, null, null, null);

    mockMvc
        .perform(
            post("/api/shows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should update show and return 200")
  void shouldUpdateShowAndReturn200() throws Exception {
    Show show = saveShow("Old Name", LocalDate.now());

    ShowController.UpdateShowRequest updateRequest =
        new ShowController.UpdateShowRequest(
            "New Name",
            "Updated description",
            testShowType.getId(),
            LocalDate.now().plusDays(7),
            null,
            null,
            null,
            null,
            null);

    mockMvc
        .perform(
            put("/api/shows/{id}", show.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Name"))
        .andExpect(jsonPath("$.description").value("Updated description"));

    assertThat(showRepository.findById(show.getId()))
        .isPresent()
        .hasValueSatisfying(updated -> assertThat(updated.getName()).isEqualTo("New Name"));
  }

  @Test
  @DisplayName("Should return 404 when updating non-existent show")
  void shouldReturn404WhenUpdatingNonExistentShow() throws Exception {
    ShowController.UpdateShowRequest updateRequest =
        new ShowController.UpdateShowRequest(
            "New Name",
            "Updated description",
            testShowType.getId(),
            LocalDate.now(),
            null,
            null,
            null,
            null,
            null);

    mockMvc
        .perform(
            put("/api/shows/{id}", 999999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should delete show and return 204")
  void shouldDeleteShowAndReturn204() throws Exception {
    Show show = saveShow("Show to Delete", LocalDate.now());
    Long showId = show.getId();

    mockMvc.perform(delete("/api/shows/{id}", showId)).andExpect(status().isNoContent());

    assertThat(showRepository.findById(showId)).isEmpty();
  }

  @Test
  @DisplayName("Should return shows in date range from calendar endpoint")
  void shouldReturnShowsInDateRangeFromCalendarEndpoint() throws Exception {
    LocalDate today = LocalDate.now();
    saveShow("Show in Range", today);
    saveShow("Show Out of Range", today.plusMonths(2));

    mockMvc
        .perform(
            get("/api/shows/calendar")
                .param("startDate", today.minusDays(1).toString())
                .param("endDate", today.plusDays(7).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Show in Range"));
  }

  @Test
  @DisplayName("Should return upcoming shows")
  void shouldReturnUpcomingShows() throws Exception {
    LocalDate future = LocalDate.now().plusDays(10);
    saveShow("Future Show", future);

    mockMvc
        .perform(get("/api/shows/upcoming").param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  private Show saveShow(final String name, final LocalDate showDate) {
    Show show = new Show();
    show.setName(name);
    show.setDescription("Test show description");
    show.setShowDate(showDate);
    show.setType(testShowType);
    return showRepository.saveAndFlush(show);
  }
}
