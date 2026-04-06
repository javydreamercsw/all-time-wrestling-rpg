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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.VenueContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class AbstractSegmentNarrationServiceTest {

  private final AbstractSegmentNarrationService service =
      new AbstractSegmentNarrationService() {
        @Override
        protected String callAIProvider(@NonNull String prompt) {
          return "Mock response";
        }

        @Override
        public String getProviderName() {
          return "TestProvider";
        }

        @Override
        public boolean isAvailable() {
          return true;
        }
      };

  @Test
  void testBuildSegmentNarrationPrompt() {
    SegmentNarrationContext context = new SegmentNarrationContext();
    SegmentTypeContext type = new SegmentTypeContext();
    type.setSegmentType("Match");
    context.setSegmentType(type);

    WrestlerContext w1 = new WrestlerContext();
    w1.setName("Rob Van Dam");
    w1.setHailingFrom("Battle Creek, Michigan");
    context.setWrestlers(List.of(w1));

    VenueContext venue = new VenueContext();
    venue.setName("The Pit");
    venue.setAtmosphere("Gritty");
    venue.setAlignmentBias("Heel");
    venue.setEnvironmentalTraits(List.of("Smoky"));
    venue.setCulturalTags(List.of("Underground"));
    context.setVenue(venue);

    String prompt = service.buildSegmentNarrationPrompt(context);

    assertTrue(
        prompt.contains("Use the 'hailingFrom' information"),
        "Prompt should contain instructions for hailingFrom");
    assertTrue(
        prompt.contains("If venue context is present, use the name"),
        "Prompt should contain instructions for venue fields");
    assertTrue(
        prompt.contains("\"hailingFrom\" : \"Battle Creek, Michigan\""),
        "Prompt should contain hailingFrom value");
    assertTrue(prompt.contains("\"atmosphere\" : \"Gritty\""), "Prompt should contain atmosphere");
    assertTrue(
        prompt.contains("\"alignmentBias\" : \"Heel\""), "Prompt should contain alignmentBias");
    assertTrue(
        prompt.contains("\"environmentalTraits\" : [ \"Smoky\" ]"),
        "Prompt should contain environmentalTraits");
    assertTrue(
        prompt.contains("\"culturalTags\" : [ \"Underground\" ]"),
        "Prompt should contain culturalTags");
  }
}
