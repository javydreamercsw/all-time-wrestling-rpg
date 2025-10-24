package com.github.javydreamercsw.management.controller.title;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.config.TestSecurityConfiguration;
import com.github.javydreamercsw.base.config.WithMockUser;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.title.TitleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TitleController.class)
@Import(TestSecurityConfiguration.class)
@WithMockUser
class TitleControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TitleService titleService;

  @MockitoBean private WrestlerRepository wrestlerRepository;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void createTitle() throws Exception {
    Title title = new Title();
    title.setId(1L);
    title.setName("Test Title");
    title.setTier(WrestlerTier.MIDCARDER);

    when(titleService.titleNameExists(any())).thenReturn(false);
    when(titleService.createTitle(any(), any(), any())).thenReturn(title);

    TitleController.CreateTitleRequest request =
        new TitleController.CreateTitleRequest("Test Title", "Description", WrestlerTier.MIDCARDER);

    mockMvc
        .perform(
            post("/api/titles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Title"));
  }
}
