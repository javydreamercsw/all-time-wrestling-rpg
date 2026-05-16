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
package com.github.javydreamercsw.management.controller.segment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WithMockUser(authorities = {"ADMIN", "ROLE_ADMIN", "ROLE_BOOKER"})
class NPCSegmentControllerTest extends AbstractControllerTest {

  @MockitoBean private NPCSegmentResolutionService npcSegmentResolutionService;
  @MockitoBean private SegmentRepository segmentRepository;
  @MockitoBean private SegmentTypeRepository segmentTypeRepository;
  @MockitoBean private ShowRepository showRepository;
  @MockitoBean protected WrestlerRepository wrestlerRepository;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Show testShow;
  private SegmentType testSegmentType;
  private Segment testSegment;

  @BeforeEach
  void setUp() {
    wrestler1 = Wrestler.builder().build();
    wrestler1.setId(1L);
    wrestler1.setName("Stone Cold");

    wrestler2 = Wrestler.builder().build();
    wrestler2.setId(2L);
    wrestler2.setName("The Rock");

    testShow = new Show();
    testShow.setId(10L);
    testShow.setName("Raw");

    testSegmentType = new SegmentType();
    testSegmentType.setId(1L);
    testSegmentType.setName("Singles Match");

    testSegment = new Segment();
    testSegment.setId(100L);
    testSegment.setShow(testShow);
    testSegment.setIsNpcGenerated(true);
  }

  @Test
  void resolveTeamSegment_success() throws Exception {
    when(wrestlerRepository.findAllById(List.of(1L))).thenReturn(List.of(wrestler1));
    when(wrestlerRepository.findAllById(List.of(2L))).thenReturn(List.of(wrestler2));
    when(segmentTypeRepository.findById(1L)).thenReturn(Optional.of(testSegmentType));
    when(showRepository.findById(10L)).thenReturn(Optional.of(testShow));
    when(npcSegmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenReturn(testSegment);

    NPCSegmentController.TeamSegmentRequest request =
        new NPCSegmentController.TeamSegmentRequest(
            List.of(1L), "Team Stone Cold", List.of(2L), "Team Rock", 1L, 10L, null);

    mockMvc
        .perform(
            post("/api/npc-segments/team")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.segmentType").exists())
        .andExpect(jsonPath("$.winningTeam").exists());
  }

  @Test
  void resolveTeamSegment_wrestler1NotFound() throws Exception {
    when(wrestlerRepository.findAllById(List.of(99L))).thenReturn(List.of());

    NPCSegmentController.TeamSegmentRequest request =
        new NPCSegmentController.TeamSegmentRequest(
            List.of(99L), "Team 1", List.of(2L), "Team 2", 1L, 10L, null);

    mockMvc
        .perform(
            post("/api/npc-segments/team")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  void resolveTeamSegment_wrestler2NotFound() throws Exception {
    when(wrestlerRepository.findAllById(List.of(1L))).thenReturn(List.of(wrestler1));
    when(wrestlerRepository.findAllById(List.of(99L))).thenReturn(List.of());

    NPCSegmentController.TeamSegmentRequest request =
        new NPCSegmentController.TeamSegmentRequest(
            List.of(1L), "Team 1", List.of(99L), "Team 2", 1L, 10L, null);

    mockMvc
        .perform(
            post("/api/npc-segments/team")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  void resolveTeamSegment_segmentTypeNotFound() throws Exception {
    when(wrestlerRepository.findAllById(List.of(1L))).thenReturn(List.of(wrestler1));
    when(wrestlerRepository.findAllById(List.of(2L))).thenReturn(List.of(wrestler2));
    when(segmentTypeRepository.findById(99L)).thenReturn(Optional.empty());

    NPCSegmentController.TeamSegmentRequest request =
        new NPCSegmentController.TeamSegmentRequest(
            List.of(1L), "Team 1", List.of(2L), "Team 2", 99L, 10L, null);

    mockMvc
        .perform(
            post("/api/npc-segments/team")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Segment type not found"));
  }

  @Test
  void resolveTeamSegment_showNotFound() throws Exception {
    when(wrestlerRepository.findAllById(List.of(1L))).thenReturn(List.of(wrestler1));
    when(wrestlerRepository.findAllById(List.of(2L))).thenReturn(List.of(wrestler2));
    when(segmentTypeRepository.findById(1L)).thenReturn(Optional.of(testSegmentType));
    when(showRepository.findById(99L)).thenReturn(Optional.empty());

    NPCSegmentController.TeamSegmentRequest request =
        new NPCSegmentController.TeamSegmentRequest(
            List.of(1L), "Team 1", List.of(2L), "Team 2", 1L, 99L, null);

    mockMvc
        .perform(
            post("/api/npc-segments/team")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Show not found"));
  }

  @Test
  void getSegmentsByShow_found() throws Exception {
    when(showRepository.findById(10L)).thenReturn(Optional.of(testShow));
    when(segmentRepository.findByShow(testShow)).thenReturn(List.of(testSegment));

    mockMvc
        .perform(get("/api/npc-segments/show/10").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.show").value("Raw"))
        .andExpect(jsonPath("$.segmentCount").value(1));
  }

  @Test
  void getSegmentsByShow_showNotFound() throws Exception {
    when(showRepository.findById(anyLong())).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/npc-segments/show/99").with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Show not found"));
  }

  @Test
  void getWrestlerSegmentHistory_found() throws Exception {
    Page<Segment> page = new PageImpl<>(List.of(testSegment));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(segmentRepository.findByWrestlerParticipation(any(Wrestler.class), any(Pageable.class)))
        .thenReturn(page);
    when(segmentRepository.countSegmentsByWrestler(any(Wrestler.class))).thenReturn(10L);
    when(segmentRepository.countWinsByWrestler(any(Wrestler.class))).thenReturn(7L);

    mockMvc
        .perform(get("/api/npc-segments/wrestler/1/history").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.wrestler").value("Stone Cold"))
        .andExpect(jsonPath("$.totalSegments").value(10))
        .andExpect(jsonPath("$.wins").value(7));
  }

  @Test
  void getWrestlerSegmentHistory_wrestlerNotFound() throws Exception {
    when(wrestlerRepository.findById(anyLong())).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/npc-segments/wrestler/99/history").with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Wrestler not found"));
  }

  @Test
  void getNpcGeneratedSegments_returnsList() throws Exception {
    when(segmentRepository.findByIsNpcGeneratedTrue()).thenReturn(List.of(testSegment));

    mockMvc
        .perform(get("/api/npc-segments/npc-generated").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(1));
  }
}
