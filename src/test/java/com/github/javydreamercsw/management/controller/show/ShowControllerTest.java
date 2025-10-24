package com.github.javydreamercsw.management.controller.show;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShowController.class)
class ShowControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShowService showService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void createShow() throws Exception {
    Show show = new Show();
    show.setId(1L);
    show.setName("Test Show");
    show.setShowDate(LocalDate.now());

    when(showService.createShow(any(), any(), any(), any(), any(), any())).thenReturn(show);

    ShowController.CreateShowRequest request =
        new ShowController.CreateShowRequest(
            "Test Show", "Description", 1L, LocalDate.now(), 1L, 1L);

    mockMvc
        .perform(
            post("/api/shows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Show"));
  }
}
