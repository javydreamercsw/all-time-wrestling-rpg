package com.github.javydreamercsw.base.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.MatchNarrationService.MatchNarrationContext;
import com.github.javydreamercsw.base.service.match.MatchOutcomeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MatchNarrationController.class)
class MatchNarrationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MatchNarrationServiceFactory serviceFactory;
  @MockitoBean private MatchNarrationConfig config;
  @MockitoBean private MatchOutcomeProvider matchOutcomeService;

  @Test
  void testNarrateMatch() throws Exception {
    // Given
    MatchNarrationService service = mock(MatchNarrationService.class);
    when(serviceFactory.getBestAvailableService()).thenReturn(service);
    when(service.narrateMatch(any(MatchNarrationContext.class))).thenReturn("Test narration");
    when(matchOutcomeService.determineOutcomeIfNeeded(any(MatchNarrationContext.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    MatchNarrationContext context = new MatchNarrationContext();

    // When & Then
    mockMvc
        .perform(
            post("/api/match-narration/narrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(context)))
        .andExpect(status().isOk());
  }
}
