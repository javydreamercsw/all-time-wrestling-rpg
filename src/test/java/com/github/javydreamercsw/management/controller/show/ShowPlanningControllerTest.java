package com.github.javydreamercsw.management.controller.show;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.config.TestSecurityConfiguration;
import com.github.javydreamercsw.base.config.WithMockUser;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ProposedShow;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningAiService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ShowPlanningController.class,
    excludeAutoConfiguration = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
@TestPropertySource(properties = "spring.flyway.enabled=false")
@Import(TestSecurityConfiguration.class)
@WithMockUser
class ShowPlanningControllerTest {

  @MockitoBean private CommandLineRunner commandLineRunner;

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ShowPlanningService showPlanningService;

  @MockitoBean private ShowPlanningAiService showPlanningAiService;

  @MockitoBean private ShowService showService;

  @Test
  void getShowPlanningContext() throws Exception {
    // Given
    Show show = new Show();
    show.setId(1L);
    when(showService.getShowById(1L)).thenReturn(Optional.of(show));
    when(showPlanningService.getShowPlanningContext(show)).thenReturn(new ShowPlanningContextDTO());

    // When & Then
    mockMvc.perform(get("/api/show-planning/context/1")).andExpect(status().isOk());
  }

  @Test
  void planShow() throws Exception {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    when(showPlanningAiService.planShow(context)).thenReturn(new ProposedShow());

    // When & Then
    mockMvc
        .perform(
            post("/api/show-planning/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
        .andExpect(status().isOk());
  }
}
