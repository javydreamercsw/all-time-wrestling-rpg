package com.github.javydreamercsw.management.controller.drama;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.config.TestSecurityConfiguration;
import com.github.javydreamercsw.base.config.WithMockUser;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DramaEventController.class)
@Import(TestSecurityConfiguration.class)
@WithMockUser
class DramaEventControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DramaEventService dramaEventService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void createDramaEvent() throws Exception {
    DramaEvent event = new DramaEvent();
    event.setId(1L);
    event.setTitle("Test Event");
    event.setSeverity(DramaEventSeverity.NEUTRAL);
    event.setEventType(DramaEventType.BACKSTAGE_INCIDENT);

    when(dramaEventService.createDramaEvent(any(), any(), any(), any(), any(), any()))
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Test Event"));
  }
}
