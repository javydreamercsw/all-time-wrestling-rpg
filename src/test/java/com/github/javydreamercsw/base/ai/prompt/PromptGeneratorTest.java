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

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.VenueContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
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

    WrestlerContext w1 = new WrestlerContext();
    w1.setName("Rob Van Dam");
    w1.setHailingFrom("Battle Creek, Michigan");
    context.setWrestlers(List.of(w1));

    VenueContext venue = new VenueContext();
    venue.setName("Neon Serpent Dome");
    venue.setLocation("Neo-Tokyo");
    venue.setAtmosphere("Electric");
    venue.setAlignmentBias("Anarchic");
    venue.setEnvironmentalTraits(List.of("Dust Storms"));
    venue.setCulturalTags(List.of("Cyberpunk"));
    context.setVenue(venue);

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
        prompt.contains("\"hailingFrom\" : \"Battle Creek, Michigan\""),
        "Prompt should contain hailing from information in JSON");
    assertTrue(
        prompt.contains("\"atmosphere\" : \"Electric\""),
        "Prompt should contain atmosphere in JSON");
    assertTrue(
        prompt.contains("\"alignmentBias\" : \"Anarchic\""),
        "Prompt should contain alignmentBias in JSON");
    assertTrue(
        prompt.contains("\"environmentalTraits\" : [ \"Dust Storms\" ]"),
        "Prompt should contain environmentalTraits in JSON");
    assertTrue(
        prompt.contains("\"culturalTags\" : [ \"Cyberpunk\" ]"),
        "Prompt should contain culturalTags in JSON");
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

    VenueContext venue = new VenueContext();
    venue.setName("Neon Serpent Dome");
    venue.setLocation("Neo-Tokyo");
    context.setVenue(venue);

    String prompt = generator.generateSimplifiedMatchNarrationPrompt(context);

    assertTrue(
        prompt.contains("VENUE: Neon Serpent Dome in Neo-Tokyo"),
        "Simplified prompt should contain venue information");
    assertTrue(
        prompt.contains("Each line MUST start with '[SPEAKER:Commentator Name]:'"),
        "Simplified prompt should contain dialogue instructions");
  }
}
