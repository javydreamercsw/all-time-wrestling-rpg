package com.github.javydreamercsw.management.controller.season;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.config.TestSecurityConfiguration;
import com.github.javydreamercsw.base.config.WithMockUser;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.service.season.SeasonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SeasonController.class)
@Import(TestSecurityConfiguration.class)
@WithMockUser
class SeasonControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SeasonService seasonService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void createSeason() throws Exception {
    Season season = new Season();
    season.setId(1L);
    season.setName("Test Season");
    season.setStartDate(java.time.Instant.now());

    when(seasonService.createSeason(any(), any(), any())).thenReturn(season);

    SeasonController.CreateSeasonRequest request =
        new SeasonController.CreateSeasonRequest("Test Season", "Description", 5);

    mockMvc
        .perform(
            post("/api/seasons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Season"));
  }
}
