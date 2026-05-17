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
package com.github.javydreamercsw.management.controller.ranking;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedTeamDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

class RankingControllerTest extends AbstractControllerTest {

  @Test
  @WithMockUser(roles = "PLAYER")
  void getChampionships() throws Exception {
    ChampionshipDTO championship =
        ChampionshipDTO.builder()
            .id(1L)
            .name("World Championship")
            .imageUrl("world-championship.png")
            .build();

    when(rankingService.getChampionships()).thenReturn(List.of(championship));

    mockMvc
        .perform(get("/api/rankings/championships"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("World Championship"));
  }

  @Test
  @WithMockUser(roles = "PLAYER")
  void getRankedContenders() throws Exception {
    RankedWrestlerDTO contender =
        RankedWrestlerDTO.builder().id(1L).name("Contender 1").fans(1000L).rank(1).build();

    when(rankingService.getRankedContenders(anyLong()))
        .thenAnswer(invocation -> List.of(contender));

    mockMvc
        .perform(get("/api/rankings/championships/1/contenders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Contender 1"));
  }

  @Test
  @WithMockUser(roles = "PLAYER")
  void getRankedTeamContenders() throws Exception {
    RankedTeamDTO contender =
        RankedTeamDTO.builder().id(1L).name("Contender Team 1").fans(1000L).rank(1).build();

    when(rankingService.getRankedContenders(anyLong()))
        .thenAnswer(invocation -> List.of(contender));

    mockMvc
        .perform(get("/api/rankings/championships/1/contenders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Contender Team 1"));
  }

  @Test
  @WithMockUser(roles = "PLAYER")
  void getCurrentChampions() throws Exception {
    ChampionDTO champion =
        ChampionDTO.builder().id(1L).name("Champion 1").fans(2000L).reignDays(100L).build();

    when(rankingService.getCurrentChampions(anyLong())).thenReturn(List.of(champion));

    mockMvc
        .perform(get("/api/rankings/championships/1/champion"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Champion 1"));
  }

  @Test
  @WithMockUser(roles = "PLAYER")
  void getChampionships_emptyList_returnsOkWithEmptyArray() throws Exception {
    when(rankingService.getChampionships()).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/api/rankings/championships"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  @WithMockUser(roles = "PLAYER")
  void getRankedContenders_emptyList_returnsOkWithEmptyArray() throws Exception {
    when(rankingService.getRankedContenders(anyLong())).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/api/rankings/championships/1/contenders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  @WithMockUser(roles = "PLAYER")
  void getCurrentChampions_emptyList_returns404() throws Exception {
    // Controller returns 404 when champion list is empty.
    when(rankingService.getCurrentChampions(anyLong())).thenReturn(Collections.emptyList());

    mockMvc.perform(get("/api/rankings/championships/1/champion")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "PLAYER")
  void getChampionships_multipleEntries_returnsAll() throws Exception {
    ChampionshipDTO c1 =
        ChampionshipDTO.builder().id(1L).name("World Title").imageUrl("world.png").build();
    ChampionshipDTO c2 =
        ChampionshipDTO.builder().id(2L).name("Tag Titles").imageUrl("tag.png").build();

    when(rankingService.getChampionships()).thenReturn(List.of(c1, c2));

    mockMvc
        .perform(get("/api/rankings/championships"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("World Title"))
        .andExpect(jsonPath("$[1].name").value("Tag Titles"));
  }

  @Test
  @WithMockUser(roles = "PLAYER")
  void getCurrentChampions_multipleCoChampions_returnsAll() throws Exception {
    ChampionDTO c1 = ChampionDTO.builder().id(1L).name("Co-Champ A").fans(1000L).build();
    ChampionDTO c2 = ChampionDTO.builder().id(2L).name("Co-Champ B").fans(900L).build();

    when(rankingService.getCurrentChampions(anyLong())).thenReturn(List.of(c1, c2));

    mockMvc
        .perform(get("/api/rankings/championships/1/champion"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Co-Champ A"))
        .andExpect(jsonPath("$[1].name").value("Co-Champ B"));
  }
}
