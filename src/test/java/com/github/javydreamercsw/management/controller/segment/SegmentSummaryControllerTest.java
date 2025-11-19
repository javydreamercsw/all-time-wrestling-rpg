package com.github.javydreamercsw.management.controller.segment;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.service.segment.SegmentSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SegmentSummaryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SegmentSummaryService segmentSummaryService;

  @Test
  void testSummarizeSegment() throws Exception {
    Long segmentId = 1L;
    Segment mockSegment = new Segment();
    mockSegment.setId(segmentId);
    mockSegment.setSummary("Test summary");
    when(segmentSummaryService.summarizeSegment(segmentId)).thenReturn(mockSegment);

    mockMvc
        .perform(post("/api/segments/{segmentId}/summarize", segmentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary", is("Test summary")));

    verify(segmentSummaryService, times(1)).summarizeSegment(segmentId);
  }

  @Test
  void testSummarizeSegmentNotFound() throws Exception {
    Long segmentId = 999L;
    when(segmentSummaryService.summarizeSegment(segmentId))
        .thenThrow(new IllegalArgumentException("Invalid segment Id:" + segmentId));

    mockMvc
        .perform(post("/api/segments/{segmentId}/summarize", segmentId))
        .andExpect(status().isBadRequest()); // or isNotFound() if you map to 404

    verify(segmentSummaryService, times(1)).summarizeSegment(segmentId);
  }
}
