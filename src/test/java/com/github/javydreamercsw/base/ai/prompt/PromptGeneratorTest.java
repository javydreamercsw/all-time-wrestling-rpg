package com.github.javydreamercsw.base.ai.prompt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptGeneratorTest {

  @Test
  void testGenerateMatchNarrationPrompt_withManager() {
    // Given
    List<SegmentNarrationService.WrestlerContext> wrestlerContexts = new ArrayList<>();
    SegmentNarrationService.WrestlerContext wrestlerContext =
        new SegmentNarrationService.WrestlerContext();
    wrestlerContext.setName("Roman Reigns");
    wrestlerContext.setManagerName("Paul Heyman");
    wrestlerContexts.add(wrestlerContext);

    SegmentNarrationService.SegmentNarrationContext segmentContext =
        new SegmentNarrationService.SegmentNarrationContext();
    segmentContext.setWrestlers(wrestlerContexts);

    // When
    String prompt = PromptGenerator.generateMatchNarrationPrompt(segmentContext);

    // Then
    assertTrue(prompt.contains("Roman Reigns (with Paul Heyman)"));
  }

  @Test
  void testGenerateSimplifiedMatchNarrationPrompt_withManager() {
    // Given
    List<SegmentNarrationService.WrestlerContext> wrestlerContexts = new ArrayList<>();
    SegmentNarrationService.WrestlerContext wrestlerContext =
        new SegmentNarrationService.WrestlerContext();
    wrestlerContext.setName("Roman Reigns");
    wrestlerContext.setManagerName("Paul Heyman");
    wrestlerContexts.add(wrestlerContext);

    SegmentNarrationService.SegmentNarrationContext segmentContext =
        new SegmentNarrationService.SegmentNarrationContext();
    segmentContext.setWrestlers(wrestlerContexts);

    // When
    String prompt = PromptGenerator.generateSimplifiedMatchNarrationPrompt(segmentContext);

    // Then
    assertTrue(prompt.contains("Roman Reigns (with Paul Heyman)"));
  }
}
