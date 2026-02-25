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
package com.github.javydreamercsw.base.ai.prompt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.ArenaContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.LocationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptGeneratorTest {

  private final PromptGenerator generator = new PromptGenerator();

  @Test
  void testGenerateMatchNarrationPromptFormat() {
    SegmentNarrationContext context = new SegmentNarrationContext();
    SegmentTypeContext type = new SegmentTypeContext();
    type.setSegmentType("Singles Match");
    context.setSegmentType(type);
    context.setWrestlers(List.of());

    ArenaContext arena = new ArenaContext();
    arena.setName("Neon Serpent Dome");
    context.setArenaContext(arena);

    LocationContext location = new LocationContext();
    location.setName("Neo-Tokyo");
    context.setLocationContext(location);

    String prompt = generator.generateMatchNarrationPrompt(context);

    assertTrue(
        prompt.contains("[SPEAKER:Commentator Name]"),
        "Prompt should contain instructions for SPEAKER tags");
    assertTrue(
        prompt.contains("VENUE: The match takes place at 'Neon Serpent Dome'"),
        "Prompt should contain arena information");
    assertTrue(
        prompt.contains("Location: Neo-Tokyo"), "Prompt should contain location information");
    assertTrue(
        prompt.contains("DO NOT include any text that is not part of a tagged dialogue line"),
        "Prompt should contain explicit exclusion instructions");
  }

  @Test
  void testGenerateSimplifiedMatchNarrationPromptFormat() {
    SegmentNarrationContext context = new SegmentNarrationContext();
    SegmentTypeContext type = new SegmentTypeContext();
    type.setSegmentType("Singles Match");
    context.setSegmentType(type);

    ArenaContext arena = new ArenaContext();
    arena.setName("Neon Serpent Dome");
    context.setArenaContext(arena);

    LocationContext location = new LocationContext();
    location.setName("Neo-Tokyo");
    context.setLocationContext(location);

    String prompt = generator.generateSimplifiedMatchNarrationPrompt(context);

    assertTrue(
        prompt.contains("VENUE: Neon Serpent Dome in Neo-Tokyo"),
        "Simplified prompt should contain venue information");
    assertTrue(
        prompt.contains("Each line MUST start with '[SPEAKER:Commentator Name]:'"),
        "Simplified prompt should contain dialogue instructions");
  }
}
