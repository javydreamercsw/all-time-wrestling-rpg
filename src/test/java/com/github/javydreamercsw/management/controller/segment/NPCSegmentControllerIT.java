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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for NPCSegmentController REST endpoints. */
@DisplayName("NPCSegmentController Integration Tests")
@Transactional
class NPCSegmentControllerIT extends AbstractRestControllerIT {

  @Autowired private NPCSegmentResolutionService npcSegmentResolutionService;
  @Autowired private ShowRepository showRepository;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new NPCSegmentController(
                    npcSegmentResolutionService,
                    wrestlerRepository,
                    segmentTypeRepository,
                    showRepository,
                    segmentRepository))
            .build();
  }

  private Show createTestShow(final String name) {
    ShowType showType = new ShowType();
    showType.setName("NPC IT ShowType " + name);
    showType.setDescription("NPC IT ShowType Description");
    showTypeRepository.saveAndFlush(showType);

    Show show = new Show();
    show.setName(name);
    show.setDescription("NPC IT Show Description");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    show.setUniverse(defaultUniverse);
    return showRepository.saveAndFlush(show);
  }

  @Test
  @DisplayName("POST /api/npc-segments/team with empty team1 IDs returns 400")
  void resolveTeamSegment_EmptyTeam1Ids_Returns400() throws Exception {
    NPCSegmentController.TeamSegmentRequest request =
        new NPCSegmentController.TeamSegmentRequest(
            Collections.emptyList(), "Team A", List.of(999L), "Team B", 1L, 1L, null);

    mockMvc
        .perform(
            post("/api/npc-segments/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  @DisplayName("POST /api/npc-segments/team with non-existent team1 wrestler IDs returns 400")
  void resolveTeamSegment_NonExistentTeam1WrestlerIds_Returns400() throws Exception {
    NPCSegmentController.TeamSegmentRequest request =
        new NPCSegmentController.TeamSegmentRequest(
            List.of(999_998L), "Team A", List.of(999_999L), "Team B", 1L, 1L, null);

    mockMvc
        .perform(
            post("/api/npc-segments/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  @DisplayName("POST /api/npc-segments/team with non-existent team2 wrestler IDs returns 400")
  void resolveTeamSegment_NonExistentTeam2WrestlerIds_Returns400() throws Exception {
    Wrestler wrestler1 = createTestWrestler("NPC Wrestler Alpha", 50_000L);

    NPCSegmentController.TeamSegmentRequest request =
        new NPCSegmentController.TeamSegmentRequest(
            List.of(wrestler1.getId()), "Team A", List.of(999_999L), "Team B", 1L, 1L, null);

    mockMvc
        .perform(
            post("/api/npc-segments/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  @DisplayName("GET /api/npc-segments/npc-generated returns 200")
  void getNpcGeneratedSegments_Returns200() throws Exception {
    mockMvc
        .perform(get("/api/npc-segments/npc-generated"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.npcGeneratedSegments").isArray());
  }

  @Test
  @DisplayName("GET /api/npc-segments/show/{showId} with invalid show returns 400")
  void getSegmentsByShow_InvalidShowId_Returns400() throws Exception {
    mockMvc
        .perform(get("/api/npc-segments/show/{showId}", 999_999L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  @DisplayName("GET /api/npc-segments/show/{showId} with valid show returns 200")
  void getSegmentsByShow_ValidShowId_Returns200() throws Exception {
    Show show = createTestShow("NPC Test Show");

    mockMvc
        .perform(get("/api/npc-segments/show/{showId}", show.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.segmentCount").exists());
  }

  @Test
  @DisplayName("GET /api/npc-segments/wrestler/{wrestlerId}/history with invalid id returns 400")
  void getWrestlerSegmentHistory_InvalidWrestlerId_Returns400() throws Exception {
    mockMvc
        .perform(get("/api/npc-segments/wrestler/{wrestlerId}/history", 999_999L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  @DisplayName(
      "GET /api/npc-segments/wrestler/{wrestlerId}/history with valid wrestler returns 200")
  void getWrestlerSegmentHistory_ValidWrestlerId_Returns200() throws Exception {
    Wrestler wrestler = createTestWrestler("NPC History Wrestler", 50_000L);

    mockMvc
        .perform(get("/api/npc-segments/wrestler/{wrestlerId}/history", wrestler.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.wrestler").value("NPC History Wrestler"))
        .andExpect(jsonPath("$.totalSegments").exists());
  }
}
