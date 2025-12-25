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
package com.github.javydreamercsw.management.controller.injury;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.service.ranking.RankingService;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InjuryController.class)
class InjuryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private InjuryService injuryService;

  @MockitoBean private WrestlerRepository wrestlerRepository;
  @MockitoBean private RankingService rankingService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void createInjury() throws Exception {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");

    Injury injury = new Injury();
    injury.setId(1L);
    injury.setWrestler(wrestler);
    injury.setName("Broken Leg");
    injury.setDescription("A severe break.");
    injury.setSeverity(InjurySeverity.SEVERE);
    injury.setInjuryDate(Instant.now());
    injury.setHealthPenalty(10);
    injury.setIsActive(true);

    when(injuryService.createInjury(any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(injury));

    InjuryController.CreateInjuryRequest request =
        new InjuryController.CreateInjuryRequest(
            1L, "Broken Leg", "A severe break.", InjurySeverity.SEVERE, "Some notes");

    mockMvc
        .perform(
            post("/api/injuries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Broken Leg"));
  }
}
