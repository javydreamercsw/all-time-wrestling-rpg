package com.github.javydreamercsw.base.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.service.segment.SegmentOutcomeProvider;
import com.github.javydreamercsw.base.test.BaseControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = SegmentNarrationController.class,
    excludeAutoConfiguration = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
class MatchNarrationControllerTest extends BaseControllerTest {

  @MockBean private CommandLineRunner commandLineRunner;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SegmentNarrationServiceFactory serviceFactory;
  @MockitoBean private SegmentNarrationConfig config;
  @MockitoBean private SegmentOutcomeProvider matchOutcomeService;

  @Test
  void testNarrateMatch() throws Exception {
    // Given
    SegmentNarrationService service = mock(SegmentNarrationService.class);
    when(serviceFactory.getBestAvailableService()).thenReturn(service);
    when(service.narrateSegment(any(SegmentNarrationContext.class))).thenReturn("Test narration");
    when(matchOutcomeService.determineOutcomeIfNeeded(any(SegmentNarrationContext.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    SegmentNarrationContext context = new SegmentNarrationContext();

    // When & Then
    mockMvc
        .perform(
            post("/api/segment-narration/narrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(context)))
        .andExpect(status().isOk());
  }
}
