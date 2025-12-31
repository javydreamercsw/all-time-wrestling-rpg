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
import com.github.javydreamercsw.management.service.ranking.RankingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(RankingController.class)
class RankingControllerTest extends AbstractControllerTest {

  @MockitoBean private RankingService rankingService;

  @Test
  @WithMockUser(roles = "PLAYER")
  void getChampionships() throws Exception {
    ChampionshipDTO championship =
        ChampionshipDTO.builder()
            .id(1L)
            .name("World Championship")
            .imageName("world-championship.png")
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
}
