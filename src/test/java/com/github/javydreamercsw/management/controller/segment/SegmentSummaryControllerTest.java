package com.github.javydreamercsw.management.controller.segment;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.service.segment.SegmentSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SegmentSummaryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SegmentSummaryService segmentSummaryService;

  @Test
  void testSummarizeSegment() throws Exception {
    Long segmentId = 1L;
    mockMvc
        .perform(post("/api/segments/{segmentId}/summarize", segmentId))
        .andExpect(status().isOk());

    verify(segmentSummaryService, times(1)).summarizeSegment(segmentId);
  }
}
