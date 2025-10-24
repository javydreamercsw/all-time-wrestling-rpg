package com.github.javydreamercsw.management.controller.injury;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.config.TestSecurityConfiguration;
import com.github.javydreamercsw.base.config.WithMockUser;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InjuryController.class)
@Import(TestSecurityConfiguration.class)
@WithMockUser
class InjuryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private InjuryService injuryService;

  @MockitoBean private WrestlerRepository wrestlerRepository;

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
