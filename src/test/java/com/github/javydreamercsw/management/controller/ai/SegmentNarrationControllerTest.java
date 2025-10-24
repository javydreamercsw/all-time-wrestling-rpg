package com.github.javydreamercsw.management.controller.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.config.WithMockUser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class SegmentNarrationControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void testNarrateMatch() throws Exception {
    SegmentNarrationService.SegmentNarrationContext context =
        new SegmentNarrationService.SegmentNarrationContext();
    SegmentNarrationService.SegmentTypeContext segmentType =
        new SegmentNarrationService.SegmentTypeContext();
    segmentType.setSegmentType("test");
    context.setSegmentType(segmentType);
    List<SegmentNarrationService.WrestlerContext> wrestlers = new ArrayList<>();
    SegmentNarrationService.WrestlerContext wrestler =
        new SegmentNarrationService.WrestlerContext();
    wrestler.setName("Tester");
    wrestlers.add(wrestler);
    context.setWrestlers(wrestlers);

    mockMvc
        .perform(
            post("/api/segment-narration/narrate/Mock AI")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
        .andExpect(status().isOk());
  }
}
