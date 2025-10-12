package com.github.javydreamercsw.management.controller.export;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DataExportController.class)
class DataExportControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean
  private ShowService showService;

  @MockitoBean private ShowTemplateService showTemplateService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void exportShows() throws Exception {
    when(showService.findAllWithRelationships()).thenReturn(new ArrayList<>());

    mockMvc
        .perform(post("/api/export/shows").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  void exportShowTemplates() throws Exception {
    when(showTemplateService.findAll()).thenReturn(new ArrayList<>());

    mockMvc
        .perform(post("/api/export/show-templates").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  void exportAll() throws Exception {
    when(showService.findAllWithRelationships()).thenReturn(new ArrayList<>());
    when(showTemplateService.findAll()).thenReturn(new ArrayList<>());

    mockMvc
        .perform(post("/api/export/all").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }
}
