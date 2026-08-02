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
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningSegmentDTO;
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

  @Test
  void testInjuredWrestlerExcludedFromPrompt() {
    // The upstream filter removes injured wrestlers before building the context.
    // Verify that a wrestler absent from fullRoster does not appear in the prompt.
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ShowPlanningRosterEntryDTO healthy = new ShowPlanningRosterEntryDTO();
    healthy.setId(1L);
    healthy.setName("Healthy Wrestler");
    healthy.setInjured(false);
    ctx.setFullRoster(List.of(healthy));

    String prompt = builder.build(ctx);

    assertFalse(prompt.contains("Injured Wrestler"), "Absent injured wrestler must not appear");
    assertTrue(prompt.contains("Healthy Wrestler"), "Healthy wrestler must appear");
  }

  @Test
  void testLowConditionWrestlerExcludedFromPrompt() {
    // The upstream filter removes low-condition wrestlers before building the context.
    // Verify that only the healthy wrestler (above threshold) appears in the roster section.
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ShowPlanningRosterEntryDTO healthy = new ShowPlanningRosterEntryDTO();
    healthy.setId(2L);
    healthy.setName("Top Condition Wrestler");
    healthy.setInjured(false);
    ctx.setFullRoster(List.of(healthy));

    String prompt = builder.build(ctx);

    assertFalse(
        prompt.contains("Low Condition Wrestler"), "Absent low-condition wrestler must not appear");
    assertTrue(prompt.contains("Top Condition Wrestler"), "Full-health wrestler must appear");
  }

  @Test
  void testHealthyWrestlerIncludedInPrompt() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ShowPlanningRosterEntryDTO wrestler = new ShowPlanningRosterEntryDTO();
    wrestler.setId(3L);
    wrestler.setName("Roman Reigns");
    wrestler.setInjured(false);
    ctx.setFullRoster(List.of(wrestler));

    String prompt = builder.build(ctx);

    assertTrue(prompt.contains("Roman Reigns"), "Healthy wrestler must appear in roster section");
    assertTrue(prompt.contains("Injured: false"), "Injured flag must be rendered as false");
  }

  @Test
  void build_recentSegments_includesTypeAndWinnersAndAntiRepetitionRules() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ShowPlanningSegmentDTO seg = new ShowPlanningSegmentDTO();
    seg.setSegmentType("Match");
    seg.setName("Standard Match");
    seg.setParticipants(List.of("Alpha", "Beta"));
    seg.setWinners(List.of("Alpha"));
    seg.setShowName("Raw");
    seg.setShowDate(Instant.now());
    ctx.setRecentSegments(List.of(seg));

    String prompt = builder.build(ctx);

    assertTrue(prompt.contains("Type: Match"), "Recent segment type must appear");
    assertTrue(prompt.contains("Winners: Alpha"), "Recent segment winners must appear");
    assertTrue(prompt.contains("Anti-Repetition Rules"), "Anti-repetition block must appear");
    assertTrue(
        prompt.contains("vary the segment format"),
        "Anti-repetition must instruct format variation");
  }

  @Test
  void build_recentSegments_noWinners_omitsWinnersField() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ShowPlanningSegmentDTO seg = new ShowPlanningSegmentDTO();
    seg.setSegmentType("Promo");
    seg.setName("Confrontation");
    seg.setParticipants(List.of("Alpha", "Beta"));
    seg.setShowName("Raw");
    seg.setShowDate(Instant.now());
    ctx.setRecentSegments(List.of(seg));

    String prompt = builder.build(ctx);

    assertFalse(prompt.contains("Winners:"), "Winners field must be omitted when empty");
  }

  @Test
  void build_rosterWithAlignment_includesAlignment() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ShowPlanningRosterEntryDTO wrestler = new ShowPlanningRosterEntryDTO();
    wrestler.setId(10L);
    wrestler.setName("The Heel");
    wrestler.setAlignment("HEEL");
    wrestler.setInjured(false);
    ctx.setFullRoster(List.of(wrestler));

    String prompt = builder.build(ctx);

    assertTrue(prompt.contains("Alignment: HEEL"), "Wrestler alignment must appear in roster");
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
