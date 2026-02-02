/*
* Copyright (C) 2026 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.base.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class CampaignNarrationPromptTest {

  private static class TestSegmentNarrationService extends AbstractSegmentNarrationService {
    @Override
    protected String callAIProvider(@NonNull String prompt) {
      return "Mock Response";
    }

    @Override
    public String getProviderName() {
      return "Test";
    }

    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    public String generateText(@NonNull String prompt) {
      return "Mock Text";
    }

    public String getPrompt(SegmentNarrationService.SegmentNarrationContext context) {
      return buildSegmentNarrationPrompt(context);
    }
  }

  @Test
  void testPromptIncludesCampaignContextInstructions() {
    TestSegmentNarrationService service = new TestSegmentNarrationService();
    SegmentNarrationService.SegmentNarrationContext context =
        new SegmentNarrationService.SegmentNarrationContext();
    context.setWrestlers(new ArrayList<>());
    SegmentNarrationService.SegmentTypeContext type =
        new SegmentNarrationService.SegmentTypeContext();
    type.setSegmentType("Match");
    context.setSegmentType(type);

    SegmentNarrationService.CampaignContext campaignContext =
        new SegmentNarrationService.CampaignContext();
    campaignContext.setChapter(1);
    campaignContext.setAlignmentType("HEEL");
    campaignContext.setAlignmentLevel(5);
    campaignContext.setActiveInjuries(List.of("Broken Nose"));

    context.setCampaignContext(campaignContext);

    String prompt = service.getPrompt(context);

    assertThat(prompt).contains("If campaignContext is present");
    assertThat(prompt).contains("HEEL");
    assertThat(prompt).contains("Broken Nose");
  }
}
