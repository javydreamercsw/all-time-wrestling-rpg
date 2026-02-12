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
        "Generate a compelling wrestling narration for a Match. \"wrestlers\": [{\"name\":"
            + " \"Wrestler A\"}, {\"name\": \"Wrestler B\"}], \"commentators\": [{\"name\": \"Dara"
            + " Hoshiko\"}, {\"name\": \"Lord Bastian Von Crowe\"}]";

    String result = service.generateText(prompt);

    assertTrue(result.contains("Dara Hoshiko:"), "Output should contain Dara's speaker tag");
    assertTrue(
        result.contains("Lord Bastian Von Crowe:"),
        "Output should contain Lord Bastian's speaker tag");
    assertTrue(result.contains("Narrator:"), "Output should contain Narrator tag");

    String[] lines = result.split("\n");
    for (String line : lines) {
      if (!line.trim().isEmpty()) {
        assertTrue(
            line.contains(": "),
            "Each non-empty line should follow Name: Text format. Line: " + line);
      }
    }
  }
}
