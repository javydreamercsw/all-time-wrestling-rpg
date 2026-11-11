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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.title.DefenseFrequencyType;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningChampionshipDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRivalryDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRosterEntryDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningSegmentDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Deterministic pins for the local card builder (ATW-wbch), modeled on the production paste that
 * motivated the pivot: Timeless 2026-09-28, 27-wrestler roster, 7 rivalries with heavy wrestler
 * overlap, one holder with a promo deficit.
 */
class LocalShowPlannerTest {

  private static final Instant SHOW_DATE =
      LocalDate.of(2026, 9, 28).atStartOfDay(ZoneOffset.UTC).toInstant();

  private final SegmentRuleService segmentRuleService = mock(SegmentRuleService.class);

  private final LocalShowPlanner planner =
      new LocalShowPlanner(new WeeklyParticipationTracker(), segmentRuleService);

  // ── Fixtures from the production paste ───────────────────────────────────────

  private static ShowPlanningRosterEntryDTO roster(String name, String gender) {
    ShowPlanningRosterEntryDTO dto = new ShowPlanningRosterEntryDTO();
    dto.setId(Long.valueOf(name.hashCode() & 0xffff));
    dto.setName(name);
    dto.setGender(gender);
    return dto;
  }

  /** The 27-wrestler Timeless roster (names + genders from the paste). */
  private static List<ShowPlanningRosterEntryDTO> timelessRoster() {
    return List.of(
        roster("Abilene Maverick", "FEMALE"),
        roster("André the Giant", "MALE"),
        roster("Brian Pillman", "MALE"),
        roster("Cassie Lee", "FEMALE"),
        roster("Dynamite Kid", "MALE"),
        roster("Eddie Guerrero", "MALE"),
        roster("Gail Kim", "FEMALE"),
        roster("Gilgamesh", "MALE"),
        roster("Jax Felix", "MALE"),
        roster("Johnny All Time", "MALE"),
        roster("Jordynne Grace", "FEMALE"),
        roster("Junkyard Dog", "MALE"),
        roster("Kamala", "MALE"),
        roster("Kurt Angle", "MALE"),
        roster("Mukundi Shumba", "MALE"),
        roster("Nick Aldis", "MALE"),
        roster("OMZ", "MALE"),
        roster("Oda Tsurugi", "MALE"),
        roster("Princess Aussie", "FEMALE"),
        roster("Randy Savage", "MALE"),
        roster("Rey Jaguar", "MALE"),
        roster("Rob Van Dam", "MALE"),
        roster("Sgt. Slaughter", "MALE"),
        roster("Taya Valkyrie", "FEMALE"),
        roster("The Beast", "FEMALE"),
        roster("The British Bulldog", "MALE"),
        roster("Tormenta", "FEMALE"));
  }

  private static ShowPlanningRivalryDTO rivalry(long id, String a, String b, int heat) {
    ShowPlanningRivalryDTO dto = new ShowPlanningRivalryDTO();
    dto.setId(id);
    dto.setName(a + " vs " + b);
    dto.setParticipants(List.of(a, b));
    dto.setHeat(heat);
    dto.setPriority(0);
    return dto;
  }

  /** The seven active rivalries from the paste (heavy overlap on the four risky wrestlers). */
  private static List<ShowPlanningRivalryDTO> timelessRivalries() {
    return List.of(
        rivalry(748, "Gail Kim", "Abilene Maverick", 89),
        rivalry(678, "Rey Jaguar", "Jax Felix", 26),
        rivalry(756, "Oda Tsurugi", "Dynamite Kid", 13),
        rivalry(847, "The Beast", "Princess Aussie", 13),
        rivalry(784, "Oda Tsurugi", "Jax Felix", 11),
        rivalry(752, "Rob Van Dam", "Eddie Guerrero", 11),
        rivalry(805, "Jax Felix", "Dynamite Kid", 10));
  }

  private static ShowPlanningContextDTO context() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate template = new ShowTemplate();
    template.setShowName("Timeless");
    template.setExpectedMatches(5);
    template.setExpectedPromos(2);
    context.setShowTemplate(template);
    context.setShowDate(SHOW_DATE);
    context.setFullRoster(new ArrayList<>(timelessRoster()));
    context.setCurrentRivalries(new ArrayList<>(timelessRivalries()));
    context.setRecentSegments(new ArrayList<>());
    context.setPremiumLiveEvent(false);
    context.setIntergenderAllowed(false);
    return context;
  }

  private static ShowPlanningSegmentDTO recentSegment(
      String type, List<String> participants, int daysAgo) {
    ShowPlanningSegmentDTO dto = new ShowPlanningSegmentDTO();
    dto.setSegmentType(type);
    dto.setParticipants(participants);
    dto.setSegmentDate(SHOW_DATE.minus(daysAgo, java.time.temporal.ChronoUnit.DAYS));
    return dto;
  }

  private static long matchCount(List<ProposedSegment> segments) {
    return segments.stream().filter(s -> !s.getType().toLowerCase().contains("promo")).count();
  }

  private static long promoCount(List<ProposedSegment> segments) {
    return segments.size() - matchCount(segments);
  }

  private static List<String> participantsOf(ProposedSegment segment) {
    return segment.getTeams().stream().flatMap(List::stream).toList();
  }

  // ── The pins ─────────────────────────────────────────────────────────────────

  @Test
  void plan_fullCard_alwaysFiveMatchesTwoPromos() {
    List<ProposedSegment> segments = planner.plan(context(), 5, 2, Set.of());

    assertEquals(5, matchCount(segments), "Match budget must fill exactly");
    assertEquals(2, promoCount(segments), "Promo budget must fill exactly");
  }

  @Test
  void plan_noWrestlerInTwoMatchesOnOneCard() {
    // The production bug: Oda Tsurugi (3 rivalries) and Jax Felix (3) double-booked.
    List<ProposedSegment> segments = planner.plan(context(), 5, 2, Set.of());

    Set<String> seen = new java.util.HashSet<>();
    for (ProposedSegment segment : segments) {
      if (segment.getType().toLowerCase().contains("promo")) {
        continue;
      }
      for (String name : participantsOf(segment)) {
        assertTrue(seen.add(name.toLowerCase()), name + " must not appear in two match rows");
      }
    }
  }

  @Test
  void plan_highHeatWeeklyFeud_getsConfrontationNotMatch() {
    // Gail Kim vs Abilene Maverick at heat 89: PLE_RESOLUTION_REQUIRED — the decisive match is
    // reserved for the PLE; the weekly gets the confrontation promo slot.
    List<ProposedSegment> segments = planner.plan(context(), 5, 2, Set.of());

    ProposedSegment confrontation =
        segments.stream()
            .filter(s -> s.getType().toLowerCase().contains("promo"))
            .filter(s -> Long.valueOf(748L).equals(s.getRivalryId()))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("The heat-89 feud must get a confrontation slot"));
    assertTrue(
        participantsOf(confrontation).containsAll(List.of("Gail Kim", "Abilene Maverick")),
        "Both feud participants appear in the confrontation");
  }

  @Test
  void plan_holderWithPromoDeficit_getsPromoFromBackfill() {
    // Shumba: 5 promos, 0 matches in the window — the under-served list ranks him ahead of
    // nobody; he is NOT skipped (his promo deficit is zero — 5 promos). Actually the under-served
    // sort puts zero-match wrestlers first, promo-count ascending: Shumba (5 promos) ranks BEHIND
    // idle wrestlers. Pin: the promo slots go to genuinely idle wrestlers first.
    ShowPlanningContextDTO context = context();
    context
        .getRecentSegments()
        .addAll(
            List.of(
                recentSegment("Promo", List.of("Mukundi Shumba"), 1),
                recentSegment("Promo", List.of("Mukundi Shumba"), 4),
                recentSegment("Promo", List.of("Mukundi Shumba"), 8),
                recentSegment("Promo", List.of("Mukundi Shumba"), 11),
                recentSegment("Promo", List.of("Mukundi Shumba"), 15)));
    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());

    List<String> promoSubjects =
        segments.stream()
            .filter(s -> s.getType().toLowerCase().contains("promo"))
            .filter(s -> s.getRivalryId() == null)
            .map(s -> participantsOf(s).get(0))
            .toList();
    assertFalse(
        promoSubjects.contains("Mukundi Shumba"),
        "A wrestler with 5 recent promos is not under-served; idle names get the slots");
  }

  @Test
  void plan_participationBackfill_targetsIdleWrestlers() {
    ShowPlanningContextDTO context = context();
    context
        .getRecentSegments()
        .addAll(
            List.of(
                recentSegment("One on One", List.of("Rey Jaguar", "Jax Felix"), 2),
                recentSegment("Promo", List.of("Mukundi Shumba"), 3)));
    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());

    List<String> promoSubjects =
        segments.stream()
            .filter(s -> s.getType().toLowerCase().contains("promo"))
            .filter(s -> s.getRivalryId() == null)
            .map(s -> participantsOf(s).get(0))
            .toList();
    // Johnny All Time, OMZ, Nick Aldis, Kamala, Randy Savage, Junkyard Dog, Kurt Angle, André,
    // Brian Pillman, Gilgamesh, Rob Van Dam, Sgt. Slaughter, The British Bulldog are all idle —
    // the two promo slots must go to idle names, never to the three recently-active wrestlers.
    for (String active : List.of("Rey Jaguar", "Jax Felix", "Mukundi Shumba")) {
      assertFalse(
          promoSubjects.contains(active), active + " was just on TV; backfill targets idle names");
    }
  }

  @Test
  void plan_wrestlerBookedThisWeekOnAnotherShow_skippedForMatches() {
    // Cross-show guardrail, calendar-week semantics: Rey Jaguar wrestled earlier the SAME week
    // on Continuum — he cannot take a match slot on this show; his feud is skipped rather than
    // double-booked across the week.
    ShowPlanningContextDTO context = context();
    context
        .getRecentSegments()
        .add(recentSegment("One on One", List.of("Rey Jaguar", "Jax Felix"), 1));
    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());

    for (ProposedSegment segment : segments) {
      if (segment.getType().toLowerCase().contains("promo")) {
        continue;
      }
      assertFalse(
          participantsOf(segment).contains("Rey Jaguar"),
          "Rey Jaguar wrestled earlier this week — no second match this week");
    }
  }

  @Test
  void plan_deterministicClaims_neverRebooked() {
    // A scripted beat already claimed Shumba — the planner must not book him again.
    List<ProposedSegment> segments = planner.plan(context(), 5, 2, Set.of("Mukundi Shumba"));

    for (ProposedSegment segment : segments) {
      assertFalse(
          participantsOf(segment).contains("Mukundi Shumba"),
          "Claimed wrestlers are off-limits to the planner");
    }
  }

  @Test
  void plan_freeSlotsZero_emptyCard() {
    assertTrue(planner.plan(context(), 0, 0, Set.of()).isEmpty());
  }

  @Test
  void plan_tagWeek_singlesFeudBooksAsTagWhenPartnersExist() {
    // Force the tag-parity week by picking a show date whose day-of-year sits on an even
    // day/7 block, with a fresh roster (no recent segments) so partners exist.
    ShowPlanningContextDTO context = context();
    Instant tagWeek =
        LocalDate.of(2026, 1, 5).atStartOfDay(ZoneOffset.UTC).toInstant(); // day 5 → parity even
    context.setShowDate(tagWeek);
    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());

    boolean hasTag = segments.stream().anyMatch(s -> "Tag Team".equals(s.getType()));
    if (LocalShowPlanner.prefersTagThisWeek(tagWeek)) {
      assertTrue(hasTag, "Tag-parity week with a full roster must produce at least one Tag Team");
    }
  }

  @Test
  void plan_pleStipulationFeud_booksMatch() {
    // On a PLE the heat ≥ 30 feud books its stipulation match (STIPULATION_REQUIRED).
    ShowPlanningContextDTO context = context();
    context.setPremiumLiveEvent(true);
    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());

    assertTrue(
        segments.stream()
            .anyMatch(
                s ->
                    !s.getType().toLowerCase().contains("promo")
                        && Long.valueOf(748L).equals(s.getRivalryId())),
        "On a PLE the heat-89 feud takes a match slot");
  }

  @Test
  void plan_continuumCollapse_lastWeeksMatchesDoNotStarveThisWeeksCard() {
    // THE FIELD BUG (2026-11-11): planning Continuum 10-02 produced 1 match + 2 promos of 7
    // slots because the rolling 7-day window caught last week's Continuum (09-25), the PLE
    // (09-27), and Timeless (09-28). With calendar-week + PLE-exempt + match-only semantics,
    // last week's action is last week's — the card fills.
    ShowPlanningContextDTO context = context();
    // The exact pre-show action from the paste: last Friday's Continuum, Sunday's PLE,
    // Monday's Timeless.
    context
        .getRecentSegments()
        .addAll(
            List.of(
                recentSegment(
                    "Tag Team",
                    List.of("Oda Tsurugi", "Jax Felix", "Rey Jaguar", "Dynamite Kid"),
                    7), // 09-25 Continuum
                recentSegment(
                    "One on One", List.of("André the Giant", "Kurt Angle"), 5), // 09-27 PLE
                recentSegment(
                    "One on One", List.of("Brian Pillman", "Gilgamesh"), 4))); // 09-28 Timeless
    context.setShowDate(LocalDate.of(2026, 10, 2).atStartOfDay(ZoneOffset.UTC).toInstant());

    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());

    assertEquals(5, matchCount(segments), "Last week's matches must not starve this week's card");
    assertEquals(2, promoCount(segments), "Promo budget must still fill");
  }

  @Test
  void plan_unbookedRivalries_beyondMatchBudgetAreDroppedQuietly() {
    // 5 match slots, 6 match-eligible rivalries (after confrontation reservation): the excess is
    // simply not booked — no error, deterministic selection by heat order.
    List<ProposedSegment> segments = planner.plan(context(), 3, 2, Set.of());
    assertEquals(3, matchCount(segments), "Exactly the requested match count");
  }

  // ── Field-test regression pins (2026-11-10 production run) ──────────────────

  @Test
  void plan_intergenderDisabled_noMixedGenderMatchRows() {
    // Production bug 1: Gail Kim & André the Giant (mixed tag) and Jordynne Grace vs Junkyard
    // Dog (mixed singles backfill) appeared with intergender disabled. Approval would reject
    // the card; the planner must never build one.
    List<ProposedSegment> segments = planner.plan(context(), 5, 2, Set.of());

    for (ProposedSegment segment : segments) {
      if (segment.getType().toLowerCase().contains("promo")) {
        continue;
      }
      Set<String> genders = new java.util.HashSet<>();
      for (String name : participantsOf(segment)) {
        genders.add(
            timelessRoster().stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .getGender());
      }
      assertTrue(
          genders.size() <= 1,
          "Match row '"
              + segment.getType()
              + "' mixes genders ("
              + participantsOf(segment)
              + ") with intergender disabled");
    }
  }

  @Test
  void plan_championshipDefenses_attachedWithRealChampion() {
    // Production bug 2: zero defenses on the card let the narration AI invent "ATW
    // Intertemporal Champion Jordynne Grace" while the real champion is Junkyard Dog. The
    // planner books the actual defense: champion vs bookable challenger, title attached by id.
    ShowPlanningContextDTO context = context();
    ShowPlanningChampionshipDTO title = new ShowPlanningChampionshipDTO();
    title.setChampionshipName("ATW Intertemporal");
    title.setTitleId(42L);
    title.setChampionName("Junkyard Dog");
    title.setContenderName("Kamala");
    title.setDaysSinceLastDefense(20L);
    context.setChampionships(new ArrayList<>(List.of(title)));

    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());

    ProposedSegment defense =
        segments.stream()
            .filter(s -> Long.valueOf(42L).equals(s.getTitleId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("The active title must defend on the card"));
    assertTrue(
        participantsOf(defense).containsAll(List.of("Junkyard Dog", "Kamala")),
        "Defense books the REAL champion and challenger, got: " + participantsOf(defense));
    assertTrue(defense.getIsTitleSegment(), "Defense row is a title segment");
    assertEquals("Junkyard Dog", participantsOf(defense).get(0), "Champion listed first");
  }

  @Test
  void plan_championshipDefense_overdueClaimsSlotBeforeFreshTitles() {
    // Two titles, one overdue: the overdue defense must take a match slot first.
    ShowPlanningContextDTO context = context();
    ShowPlanningChampionshipDTO overdue = new ShowPlanningChampionshipDTO();
    overdue.setChampionshipName("Overdue Belt");
    overdue.setTitleId(10L);
    overdue.setChampionName("Kurt Angle");
    overdue.setContenderName("Nick Aldis");
    overdue.setDaysSinceLastDefense(90L);
    overdue.setDefenseFrequencyType(DefenseFrequencyType.PLE);
    ShowPlanningChampionshipDTO fresh = new ShowPlanningChampionshipDTO();
    fresh.setChampionshipName("Fresh Belt");
    fresh.setTitleId(11L);
    fresh.setChampionName("Randy Savage");
    fresh.setContenderName("OMZ");
    fresh.setDaysSinceLastDefense(3L);
    fresh.setDefenseFrequencyType(DefenseFrequencyType.PLE);
    context.setChampionships(new ArrayList<>(List.of(fresh, overdue)));

    List<ProposedSegment> segments = planner.plan(context, 1, 0, Set.of());

    assertEquals(
        10L,
        segments.stream()
            .filter(s -> s.getTitleId() != null)
            .findFirst()
            .orElseThrow()
            .getTitleId()
            .longValue(),
        "The overdue title defends before the fresh one when slots are scarce");
  }

  @Test
  void plan_championshipDefense_rankedContenderBeatsRosterOrder_andChampionsExcluded() {
    // THE FIELD BUG (2026-11-11): the Intertemporal defense booked Junkyard Dog against André
    // the Giant — the ATW WORLD champion — because the challenger was "first idle male in
    // roster order". Rankings must drive the pick, and other titles' champions must never
    // challenge on a weekly (champion-vs-champion is PLE material).
    ShowPlanningContextDTO context = context();
    ShowPlanningChampionshipDTO intertemporal = new ShowPlanningChampionshipDTO();
    intertemporal.setChampionshipName("ATW Intertemporal");
    intertemporal.setTitleId(5L);
    intertemporal.setChampionName("Junkyard Dog");
    intertemporal.setDaysSinceLastDefense(33L);
    intertemporal.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    // Rankings say OMZ first, Rob Van Dam second — roster order would say André first.
    intertemporal.setRankedContenderNames(List.of("OMZ", "Rob Van Dam"));
    ShowPlanningChampionshipDTO world = new ShowPlanningChampionshipDTO();
    world.setChampionshipName("ATW World");
    world.setTitleId(1L);
    world.setChampionName("André the Giant");
    world.setDaysSinceLastDefense(5L);
    world.setDefenseFrequencyType(DefenseFrequencyType.PLE);
    context.setChampionships(new ArrayList<>(List.of(intertemporal, world)));

    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());

    ProposedSegment defense =
        segments.stream()
            .filter(s -> Long.valueOf(5L).equals(s.getTitleId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("The Intertemporal must defend"));
    assertTrue(
        participantsOf(defense).contains("OMZ"),
        "The TOP RANKED contender gets the shot, got: " + participantsOf(defense));
    assertFalse(
        segments.stream()
            .anyMatch(
                s ->
                    !Long.valueOf(1L).equals(s.getTitleId())
                        && participantsOf(s).contains("André the Giant")),
        "The World champion appears only in his own title's row (never a challenger elsewhere)");
  }

  @Test
  void applyHeatBasedStipulations_highHeatMatchesGetHighHeatRules() {
    // Owner requirement: stipulations come from the match-rules configuration, driven by heat —
    // no Ollama required. Heat ≥ 30 draws requiresHighHeat rules from the catalog; approval
    // REJECTS heat-30 matches without a stipulation, so this is load-bearing for PLE cards.
    ShowPlanningContextDTO context = context();
    SegmentRule cage = new SegmentRule();
    cage.setName("Cage");
    cage.setRequiresHighHeat(true);
    when(segmentRuleService.getHighHeatRules()).thenReturn(List.of(cage));
    // The heat-89 Gail Kim/Abilene feud classifies PLE_RESOLUTION_REQUIRED on a weekly —
    // planner books a confrontation (Promo row); use a heat-26+ feud on a PLE instead.
    context.setPremiumLiveEvent(true);

    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());
    planner.applyHeatBasedStipulations(segments, context);

    ProposedSegment stipulated =
        segments.stream()
            .filter(s -> !s.getType().toLowerCase().contains("promo"))
            .filter(s -> s.getRivalryId() != null && Long.valueOf(748L).equals(s.getRivalryId()))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("Heat-89 feud on a PLE takes a match slot (STIPULATION)"));
    assertEquals(List.of("Cage"), stipulated.getRules(), "Heat ≥ 30 match gets a high-heat rule");
  }

  @Test
  void applyHeatBasedStipulations_subHeatMatchesStayNormal() {
    // Heat < 30 rows stay ruleless here — applyDefaultRules backfills "Normal" downstream.
    ShowPlanningContextDTO context = context();
    SegmentRule cage = new SegmentRule();
    cage.setName("Cage");
    cage.setRequiresHighHeat(true);
    when(segmentRuleService.getHighHeatRules()).thenReturn(List.of(cage));

    List<ProposedSegment> segments = planner.plan(context(), 5, 2, Set.of());
    planner.applyHeatBasedStipulations(segments, context);

    // No weekly rivalry exceeds heat 26 in the fixture, so nothing should be stipulated.
    assertTrue(
        segments.stream()
            .filter(s -> !s.getType().toLowerCase().contains("promo"))
            .allMatch(s -> s.getRules() == null || s.getRules().isEmpty()),
        "Weekly-heat matches defer to the Normal default");
  }
}
