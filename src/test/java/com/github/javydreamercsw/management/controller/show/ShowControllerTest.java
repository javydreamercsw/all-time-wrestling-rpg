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
package com.github.javydreamercsw.management.controller.show;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WithMockUser(authorities = {"ADMIN", "ROLE_ADMIN", "ROLE_BOOKER"})
class ShowControllerTest extends AbstractControllerTest {

  @MockitoBean private ShowService showService;

  private Show show;

  @BeforeEach
  void setUp() {
    show = new Show();
    show.setId(1L);
    show.setName("Test Show");
    show.setShowDate(LocalDate.now());
  }

  @Test
  void createShow() throws Exception {
    when(showService.createShow(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(show);

    ShowController.CreateShowRequest request =
        new ShowController.CreateShowRequest(
            "Test Show", "Description", 1L, LocalDate.now(), 1L, 1L, null, null, null);

    mockMvc
        .perform(
            post("/api/shows")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Show"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void testAdjudicateShow() throws Exception {
    mockMvc.perform(post("/api/shows/1/adjudicate").with(csrf())).andExpect(status().isOk());

    verify(showService, times(1)).adjudicateShow(1L);
  }

  @Test
  void getAllShows_defaultPagination_returnsOk() throws Exception {
    Page<Show> page = new PageImpl<>(List.of(show));
    when(showService.getAllShows(any())).thenReturn(page);

    mockMvc.perform(get("/api/shows").with(csrf())).andExpect(status().isOk());
  }

  @Test
  void getAllShows_withCustomPagination_returnsOk() throws Exception {
    Page<Show> page = new PageImpl<>(List.of(show));
    when(showService.getAllShows(any())).thenReturn(page);

    mockMvc
        .perform(
            get("/api/shows")
                .param("page", "0")
                .param("size", "5")
                .param("sortBy", "name")
                .param("sortDir", "desc")
                .with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void getAllShows_emptyResult_returnsOk() throws Exception {
    Page<Show> empty = new PageImpl<>(Collections.emptyList());
    when(showService.getAllShows(any())).thenReturn(empty);

    mockMvc.perform(get("/api/shows").with(csrf())).andExpect(status().isOk());
  }

  @Test
  void getShowById_found_returnsOk() throws Exception {
    when(showService.getShowById(1L)).thenReturn(Optional.of(show));

    mockMvc
        .perform(get("/api/shows/1").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Show"));
  }

  @Test
  void getShowById_notFound_returns404() throws Exception {
    when(showService.getShowById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/shows/99").with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  void getShowsByDateRange_returnsOk() throws Exception {
    when(showService.getShowsByDateRange(any(), any())).thenReturn(List.of(show));

    mockMvc
        .perform(
            get("/api/shows/calendar")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-12-31")
                .with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void getShowsByDateRange_emptyResult_returnsOk() throws Exception {
    when(showService.getShowsByDateRange(any(), any())).thenReturn(Collections.emptyList());

    mockMvc
        .perform(
            get("/api/shows/calendar")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-12-31")
                .with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void getShowsForMonth_returnsOk() throws Exception {
    when(showService.getShowsForMonth(2025, 6)).thenReturn(List.of(show));

    mockMvc
        .perform(
            get("/api/shows/calendar/month").param("year", "2025").param("month", "6").with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void getShowsForMonth_emptyResult_returnsOk() throws Exception {
    when(showService.getShowsForMonth(2025, 6)).thenReturn(Collections.emptyList());

    mockMvc
        .perform(
            get("/api/shows/calendar/month").param("year", "2025").param("month", "6").with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void getUpcomingShows_defaultLimit_returnsOk() throws Exception {
    when(showService.getUpcomingShows(10)).thenReturn(List.of(show));

    mockMvc.perform(get("/api/shows/upcoming").with(csrf())).andExpect(status().isOk());
  }

  @Test
  void getUpcomingShows_customLimit_returnsOk() throws Exception {
    when(showService.getUpcomingShows(5)).thenReturn(List.of(show));

    mockMvc
        .perform(get("/api/shows/upcoming").param("limit", "5").with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void getUpcomingShows_emptyResult_returnsOk() throws Exception {
    when(showService.getUpcomingShows(10)).thenReturn(Collections.emptyList());

    mockMvc.perform(get("/api/shows/upcoming").with(csrf())).andExpect(status().isOk());
  }

  @Test
  void updateShow_found_returnsOk() throws Exception {
    when(showService.updateShow(
            eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(show));

    ShowController.UpdateShowRequest request =
        new ShowController.UpdateShowRequest(
            "Updated Show", "Updated Desc", 1L, LocalDate.now(), null, null, null, null, null);

    mockMvc
        .perform(
            put("/api/shows/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Show"));
  }

  @Test
  void updateShow_notFound_returns404() throws Exception {
    when(showService.updateShow(
            eq(99L), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    ShowController.UpdateShowRequest request =
        new ShowController.UpdateShowRequest(
            "Updated Show", "Updated Desc", 1L, LocalDate.now(), null, null, null, null, null);

    mockMvc
        .perform(
            put("/api/shows/99")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteShow_deleted_returnsNoContent() throws Exception {
    when(showService.deleteShow(1L)).thenReturn(true);

    mockMvc.perform(delete("/api/shows/1").with(csrf())).andExpect(status().isNoContent());

    verify(showService, times(1)).deleteShow(1L);
  }

  @Test
  void deleteShow_notFound_returns404() throws Exception {
    when(showService.deleteShow(99L)).thenReturn(false);

    mockMvc.perform(delete("/api/shows/99").with(csrf())).andExpect(status().isNotFound());

    verify(showService, times(1)).deleteShow(99L);
  }
}
