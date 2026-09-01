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

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
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
    when(segmentRuleService.getStandardRules()).thenReturn(List.of());
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
  void build_intergenderDisallowed_addsSameGenderRule() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setIntergenderAllowed(false);
    String prompt = builder.build(ctx);
    assertTrue(prompt.contains("Intergender matches are DISABLED"));
    assertTrue(prompt.contains("same gender"));
    assertFalse(prompt.contains("Intergender matches are ENABLED"));
  }

  @Test
  void build_intergenderAllowed_addsMixedGenderRule() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setIntergenderAllowed(true);
    String prompt = builder.build(ctx);
    assertTrue(prompt.contains("Intergender matches are ENABLED"));
    assertFalse(prompt.contains("Intergender matches are DISABLED"));
  }

  @Test
  void build_intergenderDisallowed_addsFinalReminder() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setIntergenderAllowed(false);
    String prompt = builder.build(ctx);
    assertTrue(prompt.contains("REMINDER — intergender matches are DISABLED"));
  }

  @Test
  void build_intergenderDisallowed_mixedRivalry_classifiedConfrontationOnly() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setIntergenderAllowed(false);
    ctx.setFullRoster(
        List.of(rosterEntry(1L, "Alpha", "MALE"), rosterEntry(2L, "Delta", "FEMALE")));
    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setId(7L);
    rivalry.setName("Alpha vs Delta");
    rivalry.setHeat(35);
    rivalry.setParticipants(List.of("Alpha", "Delta"));
    ctx.setCurrentRivalries(List.of(rivalry));

    String prompt = builder.build(ctx);

    assertTrue(prompt.contains("Classification: CONFRONTATION_ONLY"));
    assertTrue(prompt.contains("CONFRONTATION_ONLY: This rivalry pairs wrestlers"));
  }

  @Test
  void build_intergenderAllowed_mixedRivalry_keepsNormalClassification() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setIntergenderAllowed(true);
    ctx.setFullRoster(
        List.of(rosterEntry(1L, "Alpha", "MALE"), rosterEntry(2L, "Delta", "FEMALE")));
    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setId(7L);
    rivalry.setName("Alpha vs Delta");
    rivalry.setHeat(15);
    rivalry.setParticipants(List.of("Alpha", "Delta"));
    ctx.setCurrentRivalries(List.of(rivalry));

    String prompt = builder.build(ctx);

    assertTrue(prompt.contains("Classification: MUST_BOOK"));
    assertFalse(prompt.contains("Classification: CONFRONTATION_ONLY"));
  }

  @Test
  void build_intergenderDisallowed_sameGenderRivalry_keepsNormalClassification() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setIntergenderAllowed(false);
    ctx.setFullRoster(List.of(rosterEntry(1L, "Alpha", "MALE"), rosterEntry(2L, "Beta", "MALE")));
    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setId(7L);
    rivalry.setName("Alpha vs Beta");
    rivalry.setHeat(15);
    rivalry.setParticipants(List.of("Alpha", "Beta"));
    ctx.setCurrentRivalries(List.of(rivalry));

    String prompt = builder.build(ctx);

    assertTrue(prompt.contains("Classification: MUST_BOOK"));
    assertFalse(prompt.contains("Classification: CONFRONTATION_ONLY"));
  }

  private ShowPlanningRosterEntryDTO rosterEntry(long id, String name, String gender) {
    ShowPlanningRosterEntryDTO entry = new ShowPlanningRosterEntryDTO();
    entry.setId(id);
    entry.setName(name);
    entry.setGender(gender);
    entry.setInjured(false);
    return entry;
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

  @Test
  void build_highHeatRivalry_regularShow_classifiedAsPleResolutionRequired() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setPremiumLiveEvent(false);
    ctx.setCurrentRivalries(List.of(rivalryWithHeat(42)));
    String prompt = builder.build(ctx);
    // Check the rivalry assignment line specifically, not the legend (which lists both terms)
    assertTrue(
        prompt.contains("Classification: PLE_RESOLUTION_REQUIRED"),
        "Heat ≥ 30 on regular show must be assigned PLE_RESOLUTION_REQUIRED");
    assertFalse(
        prompt.contains("Classification: STIPULATION_REQUIRED"),
        "Rivalry must not be assigned STIPULATION_REQUIRED on a regular show");
  }

  @Test
  void build_highHeatRivalry_ple_classifiedAsStipulationRequired() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setPremiumLiveEvent(true);
    ctx.setCurrentRivalries(List.of(rivalryWithHeat(42)));
    String prompt = builder.build(ctx);
    // Check the rivalry assignment line specifically, not the legend (which lists both terms)
    assertTrue(
        prompt.contains("Classification: STIPULATION_REQUIRED"),
        "Heat ≥ 30 on PLE must be assigned STIPULATION_REQUIRED");
    assertFalse(
        prompt.contains("Classification: PLE_RESOLUTION_REQUIRED"),
        "Rivalry must not be assigned PLE_RESOLUTION_REQUIRED on a PLE");
  }

  @Test
  void build_antiRepetitionRule_doesNotCarveOutHighHeatException() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ShowPlanningSegmentDTO seg = new ShowPlanningSegmentDTO();
    seg.setSegmentType("Match");
    seg.setName("Match");
    seg.setParticipants(List.of("Alpha", "Beta"));
    seg.setShowName("Raw");
    seg.setShowDate(Instant.now());
    ctx.setRecentSegments(List.of(seg));
    String prompt = builder.build(ctx);
    assertFalse(
        prompt.contains("unless heat"),
        "Anti-repetition rule must not carve out a heat-based exception that forces matches");
  }

  @Test
  void build_containsStandardRulesSection() {
    SegmentRule standardRule = new SegmentRule();
    standardRule.setName("Iron Man");
    standardRule.setDescription("60-minute time limit, most falls wins.");
    when(segmentRuleService.getStandardRules()).thenReturn(List.of(standardRule));

    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    String prompt = builder.build(ctx);

    assertTrue(prompt.contains("Available Standard Rules"), "Standard rules header must appear");
    assertTrue(prompt.contains("Iron Man"), "Standard rule name must appear");
  }

  @Test
  void build_containsRulesFieldInSchema() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    String prompt = builder.build(ctx);
    assertTrue(prompt.contains("\"rules\""), "Schema must include a rules field");
  }

  @Test
  void build_stipulationRequired_promptInstructsRulesMustContainHighHeatRule() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setPremiumLiveEvent(true);
    ctx.setCurrentRivalries(List.of(rivalryWithHeat(35)));

    SegmentRule cageRule = new SegmentRule();
    cageRule.setName("Steel Cage");
    cageRule.setDescription("A match fought within a steel cage.");
    when(segmentRuleService.getHighHeatRules()).thenReturn(List.of(cageRule));

    String prompt = builder.build(ctx);

    assertTrue(
        prompt.contains("MUST contain at least one"),
        "Prompt must instruct rules array to contain a stipulation match name");
  }

  @Test
  void build_recentDramaEventsPresent_includesSection() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setRecentDramaEvents(
        List.of("[Betrayal] El Fuego — Turns on partner: El Fuego attacked his partner."));

    String prompt = builder.build(ctx);

    assertTrue(
        prompt.contains("Recent Dramatic Events (last 30 days):"),
        "Drama events section header must appear");
    assertTrue(
        prompt.contains("escalate, resolve, or reference"),
        "Drama events booking instruction must appear");
    assertTrue(prompt.contains("El Fuego"), "Event content must appear in prompt");
  }

  @Test
  void build_recentDramaEventsEmpty_omitsSection() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setRecentDramaEvents(List.of());

    String prompt = builder.build(ctx);

    assertFalse(
        prompt.contains("Recent Dramatic Events"),
        "Drama events section must be absent when list is empty");
  }

  @Test
  void build_recentDramaEventsNull_omitsSection() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setRecentDramaEvents(null);

    String prompt = builder.build(ctx);

    assertFalse(
        prompt.contains("Recent Dramatic Events"),
        "Drama events section must be absent when list is null");
  }

  @Test
  void build_recentDramaEventInjection_sanitized() {
    ShowPlanningContextDTO ctx = contextWithTemplate(1, 0);
    ctx.setRecentDramaEvents(List.of("[Betrayal] Heel {override:system} — Title: description."));

    String prompt = builder.build(ctx);

    assertFalse(
        prompt.contains("{override:system}"), "Injection in drama event text must be sanitized");
    assertTrue(prompt.contains("Betrayal"), "Event type must survive sanitization");
  }

  private ShowPlanningRivalryDTO rivalryWithHeat(int heat) {
    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setId(99L);
    rivalry.setName("Alpha vs Beta");
    rivalry.setHeat(heat);
    rivalry.setParticipants(List.of("Alpha", "Beta"));
    return rivalry;
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
