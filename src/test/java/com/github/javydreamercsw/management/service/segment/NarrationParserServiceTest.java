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
package com.github.javydreamercsw.management.service.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.dto.segment.NarrationLineDTO;
import java.util.List;
import org.junit.jupiter.api.Test;

class NarrationParserServiceTest {

  private final NarrationParserService parser = new NarrationParserService();

  @Test
  void testParseDialogue() {
    String raw =
        "Dara Hoshiko: Welcome to the show!\n"
            + "Lord Bastian Von Crowe: It's a dreadful night for some, Dara.";

    List<NarrationLineDTO> result = parser.parse(raw);

    assertEquals(2, result.size());
    assertEquals("Dara Hoshiko", result.get(0).getCommentatorName());
    assertEquals("Welcome to the show!", result.get(0).getContent());
    assertEquals("Lord Bastian Von Crowe", result.get(1).getCommentatorName());
    assertEquals("It's a dreadful night for some, Dara.", result.get(1).getContent());
  }

  @Test
  void testParseFallback() {
    String raw = "Just a plain old block of text with no tags.";

    List<NarrationLineDTO> result = parser.parse(raw);

    assertEquals(1, result.size());
    assertEquals("Announcer", result.get(0).getCommentatorName());
    assertEquals(raw, result.get(0).getContent());
  }

  @Test
  void testParseMixed() {
    String raw = "Some intro text.\n" + "Dara: Action!\n" + "Some outro text.";

    List<NarrationLineDTO> result = parser.parse(raw);

    assertEquals(1, result.size());
    assertEquals("Dara", result.get(0).getCommentatorName());
    assertEquals("Action!", result.get(0).getContent());
  }

  @Test
  void testEmpty() {
    assertTrue(parser.parse("").isEmpty());
    assertTrue(parser.parse(null).isEmpty());
  }
}
