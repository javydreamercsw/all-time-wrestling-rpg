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
package com.github.javydreamercsw.management.controller.rivalry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.base.service.ranking.RankingService;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.dto.rivalry.RivalryDTO;
import com.github.javydreamercsw.management.dto.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.mapper.RivalryMapper;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RivalryController.class)
class RivalryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RivalryService rivalryService;

  @MockitoBean private RivalryMapper rivalryMapper;
  @MockitoBean private RankingService rankingService;
  @MockitoBean private WrestlerRepository wrestlerRepository;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void createRivalry() throws Exception {
    Wrestler wrestler1 = Wrestler.builder().build();
    wrestler1.setId(1L);
    wrestler1.setName("Test Wrestler 1");

    Wrestler wrestler2 = Wrestler.builder().build();
    wrestler2.setId(2L);
    wrestler2.setName("Test Wrestler 2");

    Rivalry rivalry = new Rivalry();
    rivalry.setId(1L);
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(0);
    rivalry.setStartedDate(Instant.now());

    WrestlerDTO wrestlerDTO1 = new WrestlerDTO();
    wrestlerDTO1.setId(1L);
    wrestlerDTO1.setName("Test Wrestler 1");

    WrestlerDTO wrestlerDTO2 = new WrestlerDTO();
    wrestlerDTO2.setId(2L);
    wrestlerDTO2.setName("Test Wrestler 2");

    RivalryDTO rivalryDTO = new RivalryDTO();
    rivalryDTO.setId(1L);
    rivalryDTO.setWrestler1(wrestlerDTO1);
    rivalryDTO.setWrestler2(wrestlerDTO2);
    rivalryDTO.setHeat(0);
    rivalryDTO.setIsActive(true);

    when(rivalryService.createRivalry(any(), any(), any())).thenReturn(Optional.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    RivalryController.CreateRivalryRequest request =
        new RivalryController.CreateRivalryRequest(1L, 2L, "Some notes");

    mockMvc
        .perform(
            post("/api/rivalries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.wrestler1.name").value("Test Wrestler 1"));
  }
}
