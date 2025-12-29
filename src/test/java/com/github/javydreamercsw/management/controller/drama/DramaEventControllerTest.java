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
package com.github.javydreamercsw.management.controller.drama;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.base.config.TestSecurityConfig;
import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(DramaEventController.class)
@Import(TestSecurityConfig.class)
class DramaEventControllerTest extends AbstractControllerTest {

  @MockitoBean private DramaEventService dramaEventService;

  @Test
  @WithMockUser(roles = "BOOKER")
  void createDramaEvent() throws Exception {
    DramaEvent event = new DramaEvent();
    event.setId(1L);
    event.setTitle("Test Event");
    event.setSeverity(DramaEventSeverity.NEUTRAL);
    event.setEventType(DramaEventType.BACKSTAGE_INCIDENT);

    when(dramaEventService.createDramaEvent(
            eq(1L),
            isNull(),
            eq(DramaEventType.BACKSTAGE_INCIDENT),
            eq(DramaEventSeverity.NEUTRAL),
            eq("Test Event"),
            eq("Description")))
        .thenReturn(Optional.of(event));

    DramaEventController.CreateDramaEventRequest request =
        new DramaEventController.CreateDramaEventRequest(
            1L,
            null,
            DramaEventType.BACKSTAGE_INCIDENT,
            DramaEventSeverity.NEUTRAL,
            "Test Event",
            "Description");

    mockMvc
        .perform(
            post("/api/drama-events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Test Event"));
  }
}
