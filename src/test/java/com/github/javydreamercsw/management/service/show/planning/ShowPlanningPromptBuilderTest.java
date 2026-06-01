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
package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.HolidayService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRivalryDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRosterEntryDTO;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowPlanningPromptBuilderTest {

  private ShowPlanningPromptBuilder builder;
  private SegmentTypeService segmentTypeService;
  private SegmentRuleService segmentRuleService;
  private HolidayService holidayService;

  @BeforeEach
  void setUp() {
    segmentTypeService = mock(SegmentTypeService.class);
    segmentRuleService = mock(SegmentRuleService.class);
    holidayService = mock(HolidayService.class);
    builder = new ShowPlanningPromptBuilder(segmentTypeService, segmentRuleService, holidayService);

    SegmentType type = new SegmentType();
    type.setName("Match");
    type.setDescription("A wrestling match.");
    when(segmentTypeService.findAll()).thenReturn(List.of(type));
    when(holidayService.getHolidayTheme(any(Instant.class))).thenReturn(Optional.empty());
    when(segmentRuleService.getHighHeatRules()).thenReturn(List.of());
  }

  @Test
  void sanitize_null_returnsEmpty() {
    assertEquals("", ShowPlanningPromptBuilder.sanitize(null));
  }

  @Test
  void sanitize_normalText_unchanged() {
    assertEquals("John Cena", ShowPlanningPromptBuilder.sanitize("John Cena"));
  }

  @Test
  void sanitize_stripsBrackets() {
    assertEquals("ignore this", ShowPlanningPromptBuilder.sanitize("[ignore this]"));
  }

  @Test
  void sanitize_stripsCurlyBraces() {
    assertEquals("evil prompt", ShowPlanningPromptBuilder.sanitize("{evil prompt}"));
  }

  @Test
  void sanitize_stripsPipe() {
    assertEquals("A  B", ShowPlanningPromptBuilder.sanitize("A | B"));
  }

  @Test
  void sanitize_stripsBacktick() {
    assertEquals("cmd", ShowPlanningPromptBuilder.sanitize("`cmd`"));
  }

  @Test
  void sanitize_stripsBackslash() {
    assertEquals("pathto", ShowPlanningPromptBuilder.sanitize("path\\to"));
  }

  @Test
  void build_containsBookingRules() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    String prompt = builder.build(ctx);
    assertTrue(prompt.contains("Booking Rules & Participation Goal"));
  }

  @Test
  void build_pleFlag_addsPleSection() {
    ShowPlanningContextDTO ctx = contextWithTemplate(2, 0);
    ctx.setPremiumLiveEvent(true);
    String prompt = builder.build(ctx);
    assertTrue(prompt.contains("THIS IS A PREMIUM LIVE EVENT (PLE)"));
    assertTrue(prompt.contains("PLE-Specific Booking Rules"));
  }

  @Test
  void build_noPleFlag_omitsPleSection() {
    ShowPlanningContextDTO ctx = contextWithTemplate(2, 0);
    ctx.setPremiumLiveEvent(false);
    String prompt = builder.build(ctx);
    assertFalse(prompt.contains("THIS IS A PREMIUM LIVE EVENT (PLE)"));
  }

  @Test
  void build_injectionInWrestlerName_sanitized() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ShowPlanningRosterEntryDTO wrestler = new ShowPlanningRosterEntryDTO();
    wrestler.setId(1L);
    wrestler.setName("John[INJECT]Cena");
    wrestler.setInjured(false);
    ctx.setFullRoster(List.of(wrestler));
    String prompt = builder.build(ctx);
    assertFalse(prompt.contains("[INJECT]"), "Prompt must not contain raw injection brackets");
    assertTrue(prompt.contains("JohnINJECTCena"), "Sanitized name should appear without brackets");
  }

  @Test
  void build_injectionInRivalryName_sanitized() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setId(1L);
    rivalry.setName("Feud{override:system}");
    rivalry.setHeat(15);
    rivalry.setParticipants(List.of("Alpha", "Beta"));
    ctx.setCurrentRivalries(List.of(rivalry));
    String prompt = builder.build(ctx);
    assertFalse(prompt.contains("{override:system}"));
    assertTrue(prompt.contains("Feudoverride:system"));
  }

  private ShowPlanningContextDTO contextWithTemplate(int matches, int promos) {
    ShowPlanningContextDTO ctx = new ShowPlanningContextDTO();
    ShowTemplate template = new ShowTemplate();
    template.setExpectedMatches(matches);
    template.setExpectedPromos(promos);
    ctx.setShowTemplate(template);
    ctx.setShowDate(Instant.now());
    return ctx;
  }
}
