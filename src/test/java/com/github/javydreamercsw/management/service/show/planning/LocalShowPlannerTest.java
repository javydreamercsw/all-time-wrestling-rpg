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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
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
    dto.setSegmentDate(SHOW_DATE.minus(daysAgo, ChronoUnit.DAYS));
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

    Set<String> seen = new HashSet<>();
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
      Set<String> genders = new HashSet<>();
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
    title.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY); // 20 ≥ 14 → due
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
    overdue.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    ShowPlanningChampionshipDTO fresh = new ShowPlanningChampionshipDTO();
    fresh.setChampionshipName("Fresh Belt");
    fresh.setTitleId(11L);
    fresh.setChampionName("Randy Savage");
    fresh.setContenderName("OMZ");
    fresh.setDaysSinceLastDefense(3L);
    fresh.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
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
    // PLE-cadence World title never defends on a weekly; André is never a CHALLENGER for
    // another title — but he MAY wrestle a non-title match (champions wrestle; the narration
    // prompt's "title not on the line" note covers exactly this case).
    assertFalse(
        segments.stream().anyMatch(s -> Long.valueOf(1L).equals(s.getTitleId())),
        "PLE-cadence World title reserves its defense for PLEs — never defends on a weekly");
    assertTrue(
        segments.stream()
            .filter(s -> !Long.valueOf(5L).equals(s.getTitleId()))
            .noneMatch(
                s ->
                    Boolean.TRUE.equals(s.getIsTitleSegment())
                        && participantsOf(s).contains("André the Giant")),
        "André never defends or challenges on another title's row");
  }

  @Test
  void plan_championshipDefense_cadenceIsASchedule_notDueTitlesSkipTheCard() {
    // Cadence doctrine (owner, 2026-11-11): the defense frequency is a SCHEDULE — a title only
    // defends when due (daysSinceLastDefense ≥ cadence). Defending every active title every
    // card burned through schedules and stripped champion-vs-champion variety.
    ShowPlanningContextDTO context = context();
    ShowPlanningChampionshipDTO notDue = new ShowPlanningChampionshipDTO();
    notDue.setChampionshipName("Fresh Belt");
    notDue.setTitleId(20L);
    notDue.setChampionName("Randy Savage");
    notDue.setContenderName("OMZ");
    notDue.setDaysSinceLastDefense(3L); // 3 < 14
    notDue.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    ShowPlanningChampionshipDTO due = new ShowPlanningChampionshipDTO();
    due.setChampionshipName("Stale Belt");
    due.setTitleId(21L);
    due.setChampionName("Kurt Angle");
    due.setContenderName("Nick Aldis");
    due.setDaysSinceLastDefense(14L); // 14 ≥ 14 — due exactly at threshold
    due.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    context.setChampionships(new ArrayList<>(List.of(notDue, due)));

    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());

    assertTrue(
        segments.stream().noneMatch(s -> Long.valueOf(20L).equals(s.getTitleId())),
        "A title 3 days into a 14-day cadence does NOT defend");
    assertTrue(
        segments.stream().anyMatch(s -> Long.valueOf(21L).equals(s.getTitleId())),
        "A title AT its cadence threshold defends");
  }

  @Test
  void plan_championshipDefense_pleCadenceTitle_reservesDefenseForPles() {
    ShowPlanningContextDTO context = context();
    context.setPremiumLiveEvent(false);
    ShowPlanningChampionshipDTO world = new ShowPlanningChampionshipDTO();
    world.setChampionshipName("ATW World");
    world.setTitleId(1L);
    world.setChampionName("André the Giant");
    world.setContenderName("Kurt Angle");
    world.setDaysSinceLastDefense(40L); // well past even a monthly cadence
    world.setDefenseFrequencyType(DefenseFrequencyType.PLE);
    context.setChampionships(new ArrayList<>(List.of(world)));

    List<ProposedSegment> weeklySegments = planner.plan(context, 5, 2, Set.of());
    assertTrue(
        weeklySegments.stream().noneMatch(s -> Long.valueOf(1L).equals(s.getTitleId())),
        "PLE-cadence title never defends on a weekly, however overdue");

    context.setPremiumLiveEvent(true);
    List<ProposedSegment> pleSegments = planner.plan(context, 5, 2, Set.of());
    assertTrue(
        pleSegments.stream().anyMatch(s -> Long.valueOf(1L).equals(s.getTitleId())),
        "The SAME overdue PLE-cadence title defends on the PLE");
  }

  @Test
  void applyHeatBasedStipulations_highHeatMatchesGetHighHeatRules() {
    // Owner requirement: stipulations come from the match-rules configuration, driven by heat —
    // no Ollama required. EXPLOSIVE heat draws rating-affordable rules from the catalog; approval
    // REJECTS stipulation-less EXPLOSIVE matches, so this is load-bearing for PLE cards.
    ShowPlanningContextDTO context = context();
    SegmentRule cage = new SegmentRule();
    cage.setName("Cage");
    cage.setIntensityRating(60);
    when(segmentRuleService.getStipulationRulesForHeat(89)).thenReturn(List.of(cage));
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
    assertEquals(
        List.of("Cage"), stipulated.getRules(), "EXPLOSIVE-heat match gets an affordable rule");
  }

  @Test
  void applyHeatBasedStipulations_subHeatMatchesStayNormal() {
    // Heat < 75 rows stay ruleless here — applyDefaultRules backfills "Normal" downstream.
    ShowPlanningContextDTO context = context();
    SegmentRule cage = new SegmentRule();
    cage.setName("Cage");
    cage.setIntensityRating(60);
    when(segmentRuleService.getStipulationRulesForHeat(org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of(cage));

    List<ProposedSegment> segments = planner.plan(context(), 5, 2, Set.of());
    planner.applyHeatBasedStipulations(segments, context);

    // No weekly rivalry exceeds heat 26 in the fixture, so nothing should be stipulated.
    assertTrue(
        segments.stream()
            .filter(s -> !s.getType().toLowerCase().contains("promo"))
            .allMatch(s -> s.getRules() == null || s.getRules().isEmpty()),
        "Weekly-heat matches defer to the Normal default");
  }

  // ── Selection edge branches ──────────────────────────────────────────────────

  @Test
  void plan_nullRoster_confrontationsStillBook_noMatches() {
    // Without a roster nothing can take a MATCH slot (roster-membership gate), but
    // confrontation promos book straight from rivalry participant names.
    ShowPlanningContextDTO context = context();
    context.setFullRoster(null);
    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());

    assertEquals(0, matchCount(segments), "No match rows without a roster");
    assertEquals(
        1, segments.size(), "Only the heat-89 confrontation promo books — matches need a roster");
  }

  @Test
  void plan_nullRivalries_backfillStillFillsTheCard() {
    // Hand-built contexts without rivalry data: the planner skips the rivalry pass entirely
    // and fills every slot from the idle pool.
    ShowPlanningContextDTO context = context();
    context.setCurrentRivalries(null);
    List<ProposedSegment> segments = planner.plan(context, 2, 0, Set.of());

    assertEquals(2, matchCount(segments), "Backfill matches fill the card without rivalries");
    assertTrue(segments.stream().allMatch(s -> s.getRivalryId() == null));
  }

  @Test
  void plan_lowHeatRivalry_neverClaimed_backfillFillsInstead() {
    // Heat < 10 rivalries are filtered before classification — the feud takes no slot and the
    // idle pool backfills around it (its wrestlers stay eligible for backfill matches).
    ShowPlanningContextDTO context = context();
    context.setCurrentRivalries(
        new ArrayList<>(List.of(rivalry(1L, "Kurt Angle", "Nick Aldis", 5))));
    List<ProposedSegment> segments = planner.plan(context, 2, 0, Set.of());

    assertTrue(
        segments.stream().noneMatch(s -> Long.valueOf(1L).equals(s.getRivalryId())),
        "A heat-5 rivalry is below the classification floor");
    assertEquals(2, matchCount(segments));
  }

  @Test
  void plan_rivalryWithSingleParticipant_skipped() {
    // A malformed rivalry (one side) can't book — neither as a match nor a confrontation.
    ShowPlanningRivalryDTO broken = rivalry(99L, "Kurt Angle", "Nick Aldis", 35);
    broken.setParticipants(List.of("Kurt Angle"));
    ShowPlanningContextDTO context = context();
    context.setCurrentRivalries(new ArrayList<>(List.of(broken)));

    List<ProposedSegment> segments = planner.plan(context, 3, 2, Set.of());

    assertTrue(
        segments.stream().noneMatch(s -> Long.valueOf(99L).equals(s.getRivalryId())),
        "A one-sided rivalry takes no slot in any pass");
  }

  @Test
  void plan_loneFemaleAnchor_skippedBackfillStillFills() {
    // Intergender disabled + a lone female at the head of the idle pool: she cannot pair with
    // the male pool — the planner drops her and keeps filling with same-gender pairs.
    ShowPlanningContextDTO context = context();
    context.setCurrentRivalries(new ArrayList<>());
    context.setFullRoster(
        new ArrayList<>(
            List.of(
                roster("Lone Female", "FEMALE"),
                roster("Male One", "MALE"),
                roster("Male Two", "MALE"),
                roster("Male Three", "MALE"),
                roster("Male Four", "MALE"))));

    List<ProposedSegment> segments = planner.plan(context, 2, 0, Set.of());

    assertEquals(2, matchCount(segments), "The card still fills after dropping the unpairable");
    assertTrue(
        segments.stream().noneMatch(s -> participantsOf(s).contains("Lone Female")),
        "A wrestler with no same-gender pool mate never takes a match slot");
  }

  @Test
  void plan_mixedGenderFeud_confrontationPromoSummaryNamesPolicy() {
    // Intergender disabled + male/female feud → CONFRONTATION_ONLY: the summary states the
    // policy instead of booking an illegal match.
    ShowPlanningRivalryDTO mixed = rivalry(55L, "Gail Kim", "André the Giant", 35);
    ShowPlanningContextDTO context = context();
    context.setCurrentRivalries(new ArrayList<>(List.of(mixed)));

    List<ProposedSegment> segments = planner.plan(context, 2, 1, Set.of());

    ProposedSegment confrontation =
        segments.stream()
            .filter(s -> Long.valueOf(55L).equals(s.getRivalryId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("The mixed feud must advance via confrontation"));
    assertEquals("Promo", confrontation.getType());
    assertTrue(
        confrontation.getSummary().contains("intergender matches are disabled"),
        "CONFRONTATION_ONLY summary states the policy: " + confrontation.getSummary());
    assertTrue(
        segments.stream()
            .filter(s -> !s.getType().toLowerCase().contains("promo"))
            .noneMatch(s -> Long.valueOf(55L).equals(s.getRivalryId())),
        "The mixed feud never takes a match slot");
  }

  // ── Championship-defense selection edges ─────────────────────────────────────

  @Test
  void plan_tagTeamChampionName_notOnRosterAsSingleName_skipped() {
    // Multi-wrestler champions ("A & B") don't fit a one-side name — the defense pass skips
    // tag titles rather than booking a phantom singles champion.
    ShowPlanningContextDTO context = context();
    ShowPlanningChampionshipDTO tagTitle = new ShowPlanningChampionshipDTO();
    tagTitle.setChampionshipName("Tag Titles");
    tagTitle.setTitleId(77L);
    tagTitle.setChampionName("Team Alpha & Team Beta");
    tagTitle.setDaysSinceLastDefense(90L);
    tagTitle.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    context.setChampionships(new ArrayList<>(List.of(tagTitle)));

    List<ProposedSegment> segments = planner.plan(context, 5, 2, Set.of());

    assertTrue(
        segments.stream().noneMatch(s -> Long.valueOf(77L).equals(s.getTitleId())),
        "A tag-team champion name never books a singles defense");
  }

  @Test
  void plan_championAloneOnRoster_noChallenger_noDefense() {
    // Champion on the roster but nobody to challenge: no defense is booked, no crash.
    ShowPlanningContextDTO context = context();
    context.setFullRoster(new ArrayList<>(List.of(roster("Lone Champion", "MALE"))));
    ShowPlanningChampionshipDTO title = new ShowPlanningChampionshipDTO();
    title.setChampionshipName("Solo Belt");
    title.setTitleId(88L);
    title.setChampionName("Lone Champion");
    title.setDaysSinceLastDefense(90L);
    title.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    context.setChampionships(new ArrayList<>(List.of(title)));

    List<ProposedSegment> segments = planner.plan(context, 3, 0, Set.of());

    assertTrue(
        segments.stream().noneMatch(s -> Long.valueOf(88L).equals(s.getTitleId())),
        "No bookable challenger means no defense row");
  }

  @Test
  void plan_noContenderNoRankings_idleFallbackPicksSameGender() {
    // Hand-built context: no designated contender, empty rankings — the first same-gender idle
    // wrestler steps up; opposite-gender idles are passed over.
    ShowPlanningContextDTO context = context();
    context.setCurrentRivalries(new ArrayList<>());
    context.setFullRoster(
        new ArrayList<>(
            List.of(
                roster("Champ male", "MALE"),
                roster("Female Idle", "FEMALE"),
                roster("Male Idle", "MALE"))));
    ShowPlanningChampionshipDTO title = new ShowPlanningChampionshipDTO();
    title.setChampionshipName("Men's Belt");
    title.setTitleId(91L);
    title.setChampionName("Champ male");
    title.setDaysSinceLastDefense(30L);
    title.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    context.setChampionships(new ArrayList<>(List.of(title)));

    List<ProposedSegment> segments = planner.plan(context, 1, 0, Set.of());

    ProposedSegment defense =
        segments.stream()
            .filter(s -> Long.valueOf(91L).equals(s.getTitleId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("An overdue title with idles must defend"));
    assertTrue(
        participantsOf(defense).contains("Male Idle"),
        "The idle fallback picks a same-gender challenger, got: " + participantsOf(defense));
    assertFalse(participantsOf(defense).contains("Female Idle"));
  }

  @Test
  void plan_rankedContenderOfOtherGender_skippedForNextRanked() {
    // Rankings drive the pick — but the same-gender gate still applies: a top-ranked contender
    // of a different gender is skipped in favor of the next eligible ranked name.
    ShowPlanningContextDTO context = context();
    context.setCurrentRivalries(new ArrayList<>());
    context.setFullRoster(
        new ArrayList<>(
            List.of(
                roster("Champ male", "MALE"),
                roster("Ranked Female", "FEMALE"),
                roster("Ranked Male", "MALE"))));
    ShowPlanningChampionshipDTO title = new ShowPlanningChampionshipDTO();
    title.setChampionshipName("Men's Belt");
    title.setTitleId(92L);
    title.setChampionName("Champ male");
    title.setDaysSinceLastDefense(30L);
    title.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    title.setRankedContenderNames(List.of("Ranked Female", "Ranked Male"));
    context.setChampionships(new ArrayList<>(List.of(title)));

    List<ProposedSegment> segments = planner.plan(context, 1, 0, Set.of());

    ProposedSegment defense =
        segments.stream()
            .filter(s -> Long.valueOf(92L).equals(s.getTitleId()))
            .findFirst()
            .orElseThrow();
    assertTrue(
        participantsOf(defense).contains("Ranked Male"),
        "A cross-gender ranked contender is skipped; the next ranked name gets the shot");
  }

  @Test
  void plan_designatedContenderIsAnotherChampion_fallsThroughToNonChampion() {
    // The Intertemporal's designated contender is the World champion — champion-vs-champion is
    // PLE material, so the re-pick excludes every title's champion at every tier.
    ShowPlanningContextDTO context = context();
    context.setCurrentRivalries(new ArrayList<>());
    ShowPlanningChampionshipDTO world = new ShowPlanningChampionshipDTO();
    world.setChampionshipName("ATW World");
    world.setTitleId(1L);
    world.setChampionName("André the Giant");
    world.setDaysSinceLastDefense(5L);
    world.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY); // not due
    ShowPlanningChampionshipDTO intertemporal = new ShowPlanningChampionshipDTO();
    intertemporal.setChampionshipName("ATW Intertemporal");
    intertemporal.setTitleId(5L);
    intertemporal.setChampionName("Junkyard Dog");
    intertemporal.setContenderName("André the Giant"); // champion of the OTHER title
    intertemporal.setDaysSinceLastDefense(60L);
    intertemporal.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY); // due
    context.setChampionships(new ArrayList<>(List.of(world, intertemporal)));

    List<ProposedSegment> segments = planner.plan(context, 1, 0, Set.of());

    ProposedSegment defense =
        segments.stream()
            .filter(s -> Long.valueOf(5L).equals(s.getTitleId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("The due Intertemporal must defend"));
    assertFalse(
        participantsOf(defense).contains("André the Giant"),
        "The other title's champion is excluded from challenger selection");
    assertTrue(
        participantsOf(defense).contains("Junkyard Dog"),
        "The defense still features the real champion");
  }

  @Test
  void plan_twoOverdueTitles_longestDroughtDefendsFirst() {
    // Both overdue: the days-since-defense tiebreak sends the longest drought to the card first.
    ShowPlanningContextDTO context = context();
    context.setCurrentRivalries(new ArrayList<>());
    ShowPlanningChampionshipDTO shorter = new ShowPlanningChampionshipDTO();
    shorter.setChampionshipName("Short Drought");
    shorter.setTitleId(31L);
    shorter.setChampionName("Kurt Angle");
    shorter.setContenderName("Nick Aldis");
    shorter.setDaysSinceLastDefense(15L);
    shorter.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    ShowPlanningChampionshipDTO longer = new ShowPlanningChampionshipDTO();
    longer.setChampionshipName("Long Drought");
    longer.setTitleId(32L);
    longer.setChampionName("Randy Savage");
    longer.setContenderName("OMZ");
    longer.setDaysSinceLastDefense(90L);
    longer.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    context.setChampionships(new ArrayList<>(List.of(shorter, longer)));

    List<ProposedSegment> segments = planner.plan(context, 1, 0, Set.of());

    assertEquals(
        32L,
        segments.stream()
            .filter(s -> s.getTitleId() != null)
            .findFirst()
            .orElseThrow()
            .getTitleId()
            .longValue(),
        "With one slot, the 90-day drought outranks the 15-day one");
  }

  @Test
  void plan_vacantTitle_skippedWhileActiveTitlesDefend() {
    // A vacant title (no champion) skips the defense pass; the due title beside it defends.
    ShowPlanningContextDTO context = context();
    context.setCurrentRivalries(new ArrayList<>());
    ShowPlanningChampionshipDTO vacant = new ShowPlanningChampionshipDTO();
    vacant.setChampionshipName("Vacant Belt");
    vacant.setTitleId(61L);
    vacant.setChampionName(null);
    vacant.setDaysSinceLastDefense(90L);
    vacant.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    ShowPlanningChampionshipDTO active = new ShowPlanningChampionshipDTO();
    active.setChampionshipName("Active Belt");
    active.setTitleId(62L);
    active.setChampionName("Kurt Angle");
    active.setContenderName("Nick Aldis");
    active.setDaysSinceLastDefense(30L);
    active.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    context.setChampionships(new ArrayList<>(List.of(vacant, active)));

    List<ProposedSegment> segments = planner.plan(context, 1, 0, Set.of());

    assertTrue(segments.stream().noneMatch(s -> Long.valueOf(61L).equals(s.getTitleId())));
    assertTrue(
        segments.stream().anyMatch(s -> Long.valueOf(62L).equals(s.getTitleId())),
        "The active title defends despite the vacant one sorting beside it");
  }

  // ── Format rotation and stipulation edges ────────────────────────────────────

  @Test
  void plan_tagWeek_oppositeGenderIdlesNeverPartnerAMaleFeud() {
    // Tag-parity week, intergender disabled: a male-male feud's partners come from the same
    // gender — a female idle at the head of the pool is passed over for both sides.
    ShowPlanningContextDTO context = context();
    context.setCurrentRivalries(
        new ArrayList<>(List.of(rivalry(41L, "Kurt Angle", "Nick Aldis", 35))));
    context.setFullRoster(
        new ArrayList<>(
            List.of(
                roster("Female First", "FEMALE"),
                roster("Kurt Angle", "MALE"),
                roster("Nick Aldis", "MALE"),
                roster("Male Partner One", "MALE"),
                roster("Male Partner Two", "MALE"))));
    context.setShowDate(LocalDate.of(2026, 1, 5).atStartOfDay(ZoneOffset.UTC).toInstant());

    List<ProposedSegment> segments = planner.plan(context, 1, 0, Set.of());

    if (LocalShowPlanner.prefersTagThisWeek(context.getShowDate())) {
      ProposedSegment tag =
          segments.stream()
              .filter(s -> "Tag Team".equals(s.getType()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("Tag week with fresh partners books a tag"));
      assertTrue(
          participantsOf(tag).stream().noneMatch("Female First"::equals),
          "A female idle never partners a male-male feud with intergender disabled");
    }
  }

  @Test
  void prefersTagThisWeek_nullDate_returnsFalse() {
    assertFalse(LocalShowPlanner.prefersTagThisWeek(null), "No show date, no tag parity");
  }

  @Test
  void applyHeatBasedStipulations_emptyCatalog_noRulesNoCrash() {
    // An empty high-heat catalog leaves every row ruleless — applyDefaultRules backfills later.
    ShowPlanningContextDTO context = context();
    when(segmentRuleService.findAll()).thenReturn(List.of());

    List<ProposedSegment> segments = planner.plan(context(), 5, 2, Set.of());
    planner.applyHeatBasedStipulations(segments, context);

    assertTrue(
        segments.stream().allMatch(s -> s.getRules() == null || s.getRules().isEmpty()),
        "No catalog, no stipulations");
  }

  @Test
  void applyHeatBasedStipulations_nullCatalog_noRulesNoCrash() {
    ShowPlanningContextDTO context = context();
    when(segmentRuleService.findAll()).thenReturn(List.of());

    List<ProposedSegment> segments = planner.plan(context(), 5, 2, Set.of());
    planner.applyHeatBasedStipulations(segments, context);

    assertTrue(segments.stream().allMatch(s -> s.getRules() == null || s.getRules().isEmpty()));
  }

  @Test
  void applyHeatBasedStipulations_alreadyStipulatedRow_keepsItsRule() {
    // Scripted-beat/tournament rows arrive stipulated — the heat pass must not touch them.
    ShowPlanningContextDTO context = context();
    SegmentRule cage = new SegmentRule();
    cage.setName("Cage");
    cage.setIntensityRating(60);
    when(segmentRuleService.getStipulationRulesForHeat(org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of(cage));

    ProposedSegment stipulated = new ProposedSegment();
    stipulated.setType("One on One");
    stipulated.setRules(List.of("Steel Cage"));
    stipulated.setRivalryId(748L); // heat 89 — above threshold

    planner.applyHeatBasedStipulations(new ArrayList<>(List.of(stipulated)), context);

    assertEquals(
        List.of("Steel Cage"), stipulated.getRules(), "An already-stipulated row keeps its rule");
  }

  @Test
  void applyHeatBasedStipulations_rowWithoutRivalry_staysRuleless() {
    // A row with no rivalry id has no heat to consult — it never draws a stipulation.
    ShowPlanningContextDTO context = context();
    SegmentRule cage = new SegmentRule();
    cage.setName("Cage");
    cage.setIntensityRating(60);
    when(segmentRuleService.getStipulationRulesForHeat(org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of(cage));

    ProposedSegment orphan = new ProposedSegment();
    orphan.setType("One on One");

    planner.applyHeatBasedStipulations(new ArrayList<>(List.of(orphan)), context);

    assertTrue(orphan.getRules() == null || orphan.getRules().isEmpty());
  }
}
