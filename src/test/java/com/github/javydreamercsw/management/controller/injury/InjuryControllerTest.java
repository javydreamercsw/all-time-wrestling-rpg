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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(InjuryController.class)
class InjuryControllerTest extends AbstractControllerTest {

  @MockitoBean private InjuryService injuryService;

  @Test
  @WithMockUser(roles = "BOOKER")
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
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Broken Leg"));
  }
}
