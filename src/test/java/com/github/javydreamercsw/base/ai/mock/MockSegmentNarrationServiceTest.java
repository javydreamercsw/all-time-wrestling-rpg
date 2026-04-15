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
package com.github.javydreamercsw.base.ai.mock;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MockSegmentNarrationServiceTest {

  @Test
  void testGenerateMockTextNarrationFormat() {
    MockSegmentNarrationService service = new MockSegmentNarrationService();
    String prompt =
        "Generate a compelling wrestling narration for a Match. Here is the JSON context:\n\n"
            + "{\"wrestlers\": [{\"name\": \"Wrestler A\", \"alignment\": \"FACE\"}, {\"name\":"
            + " \"Wrestler B\", \"alignment\": \"HEEL\"}], \"commentators\": [{\"name\": \"Dara"
            + " Hoshiko\"}, {\"name\": \"Lord Bastian Von Crowe\"}], \"venue\": {\"name\": \"Test"
            + " Arena\"}}";

    String result = service.generateText(prompt);

    assertTrue(result.contains("Dara Hoshiko:"), "Output should contain Dara's speaker tag");
    assertTrue(
        result.contains("Lord Bastian Von Crowe:"),
        "Output should contain Lord Bastian's speaker tag");
    assertTrue(result.contains("Narrator:"), "Output should contain Narrator tag");
    assertTrue(result.contains("Test Arena"), "Output should contain the venue name");
  }

  @Test
  void testGenerateMockStorylineArc() {
    MockSegmentNarrationService service = new MockSegmentNarrationService();
    String result = service.generateText("generate a structured Storyline Arc");
    assertTrue(result.contains("Mock AI Arc"));
    assertTrue(result.contains("milestones"));
  }

  @Test
  void testGenerateMockMonthlyAnalysis() {
    MockSegmentNarrationService service = new MockSegmentNarrationService();
    String result = service.generateText("Lead Analyst for the Wrestling World");
    assertTrue(result.contains("MONTHLY RECAP"));
    assertTrue(result.contains("ANALYSIS"));
  }

  @Test
  void testGenerateMockBackstageSituation() {
    MockSegmentNarrationService service = new MockSegmentNarrationService();
    String result = service.generateText("Backstage Situation");
    assertTrue(result.contains("Mock Situation"));
    assertTrue(result.contains("choices"));
  }

  @Test
  void testGenerateMockCampaignEncounter() {
    MockSegmentNarrationService service = new MockSegmentNarrationService();
    String result =
        service.generateText(
            "Generate a professional wrestling narrative segment appropriate for chapter 1");
    assertTrue(result.contains("Mock narrative"));
    assertTrue(result.contains("choices"));
  }

  @Test
  void testGenerateMockPromoContext() {
    MockSegmentNarrationService service = new MockSegmentNarrationService();
    String result = service.generateText("Rhetorical Hooks");
    assertTrue(result.contains("opener"));
    assertTrue(result.contains("hooks"));
  }

  @Test
  void testGenerateMockPromoOutcome() {
    MockSegmentNarrationService service = new MockSegmentNarrationService();
    String result = service.generateText("CHOSEN HOOK: Insult the City");
    assertTrue(result.contains("retort"));
    assertTrue(result.contains("alignmentShift"));
  }

  @Test
  void testGenerateMockNews() {
    MockSegmentNarrationService service = new MockSegmentNarrationService();
    String result = service.generateText("professional wrestling sports journalist TITLE match");
    assertTrue(result.contains("NEW CHAMPION CROWNED!"));

    result = service.generateText("professional wrestling sports journalist RUMOR");
    assertTrue(result.contains("Backstage Gossip"));
  }

  @Test
  void testGenerateMockNarration() {
    MockSegmentNarrationService service = new MockSegmentNarrationService();
    String prompt =
        "professional wrestling show planner. 2 matches and 1 promos. \"wrestlers\": [{\"name\":"
            + " \"Wrestler A\"}]";
    String result = service.generateText(prompt);
    assertTrue(result.contains("Mock description"));
    assertTrue(result.contains("segmentId"));
  }

  @Test
  void testOtherPrompts() {
    MockSegmentNarrationService service = new MockSegmentNarrationService();
    String result = service.generateText("Summarize the following segment narration");
    assertTrue(result.contains("mock summary"));

    result = service.generateText("Respond directly to them with a short, impactful retort");
    assertTrue(result.contains("retired legends"));

    result = service.generateText("Unknown prompt");
    assertTrue(result.contains("mix of confusion and respect"));
  }
}
