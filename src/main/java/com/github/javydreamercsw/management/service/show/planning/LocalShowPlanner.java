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

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningChampionshipDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRivalryDTO;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The deterministic card builder (ATW-wbch): fills the free match/promo slots left after scripted
 * beats, tournament slots, and briefcase rows claim theirs. Replaces the AI's booking agency with
 * local selection — the AI later narrates the fixed card, it no longer chooses participants.
 *
 * <p>Selection doctrine (mirrors the prompt's rules via the shared {@link RivalryClassifier}):
 *
 * <ul>
 *   <li><b>Matches:</b> rivalries sorted by heat descending (the {@code getUnbookedRivalriesByHeat}
 *       greedy precedent), classified with the same rules the prompt teaches. MUST_BOOK /
 *       PROMO_COOLDOWN / PLE_RESOLUTION_ELIGIBLE feuds fill match slots with NON-OVERLAPPING
 *       pairings — a wrestler already booked in a match on this card, or already booked this
 *       calendar week on any show, is skipped. PLE_RESOLUTION_REQUIRED (heat ≥ 30 weekly) and
 *       CONFRONTATION_ONLY feuds never take a match slot: they get confrontation promos instead,
 *       keeping the decisive stipulation match PLE-only.
 *   <li><b>Format rotation:</b> when the week's parity favors it and the roster has fresh partners,
 *       a singles feud books as a Tag Team match (each feud side gains a partner); otherwise One on
 *       One. The parity flips week to week so the same feud doesn't repeat the same shape show
 *       after show.
 *   <li><b>Participation backfill:</b> remaining promo slots go to the under-served roster first
 *       (zero match appearances, then fewest promos, name tiebreak) — replacing the AI's habit of
 *       concentrating on the handful of wrestlers the heat data names.
 *   <li><b>Guardrails are constructive:</b> the selector never books a wrestler twice in matches on
 *       one card, and skips wrestlers already booked this calendar week across shows.
 * </ul>
 */
@Component
@Slf4j
public class LocalShowPlanner {

  private final WeeklyParticipationTracker tracker;
  private final SegmentRuleService segmentRuleService;

  public LocalShowPlanner(
      WeeklyParticipationTracker tracker, SegmentRuleService segmentRuleService) {
    this.tracker = tracker;
    this.segmentRuleService = segmentRuleService;
  }

  /**
   * Builds the local card for the free slots.
   *
   * @param context the planning context (roster, rivalries, recent segments, held briefcases)
   * @param freeMatches match slots left after deterministic claims
   * @param freePromos promo slots left after deterministic claims
   * @param claimedWrestlerNames wrestlers already on the card (beats/tournaments/briefcases),
   *     context-cased; the planner never re-books them
   */
  public List<ProposedSegment> plan(
      ShowPlanningContextDTO context,
      int freeMatches,
      int freePromos,
      Set<String> claimedWrestlerNames) {
    List<ProposedSegment> segments = new ArrayList<>();
    if (freeMatches <= 0 && freePromos <= 0) {
      return segments;
    }

    Instant showDate = context.getShowDate();
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(context.getRecentSegments(), showDate);
    List<String> availableNames =
        context.getFullRoster() == null
            ? List.of()
            : context.getFullRoster().stream().map(w -> w.getName()).toList();
    Set<String> rosterSet =
        availableNames.stream()
            .map(WeeklyParticipationTracker::normalize)
            .collect(Collectors.toSet());
    Set<String> claimed =
        claimedWrestlerNames.stream()
            .map(WeeklyParticipationTracker::normalize)
            .filter(rosterSet::contains)
            .collect(Collectors.toSet());
    Set<String> matchBooked = new HashSet<>(claimed);
    Set<String> bookedThisWeek = tracker.bookedThisWeek(tally, availableNames);
    bookedThisWeek.addAll(claimed);

    // Gender by normalized name — the intergender gate consults it for every pairing the
    // planner assembles (rivalry sides, tag partners, idle backfill), not just the feud itself.
    boolean intergenderAllowed = context.isIntergenderAllowed();
    Map<String, String> genderByName = new HashMap<>();
    if (context.getFullRoster() != null) {
      context
          .getFullRoster()
          .forEach(
              w -> {
                if (w.getName() != null && w.getGender() != null) {
                  genderByName.put(
                      WeeklyParticipationTracker.normalize(w.getName()), w.getGender());
                }
              });
    }

    List<ShowPlanningRivalryDTO> rivalries = rivalriesByHeat(context);
    Map<Long, String> classifications = classificationsOf(context, rivalries);

    // Wrestlers in NO active rivalry — the fresh-partner pool and the idle-vs-idle backfill pool.
    // Partners drawn from feuding wrestlers would starve those wrestlers' own pending feuds.
    Set<String> feudingNames =
        rivalries.stream()
            .filter(r -> r.getParticipants() != null)
            .flatMap(r -> r.getParticipants().stream())
            .filter(java.util.Objects::nonNull)
            .map(WeeklyParticipationTracker::normalize)
            .collect(Collectors.toSet());
    List<String> idlePool =
        availableNames.stream()
            .filter(name -> !feudingNames.contains(WeeklyParticipationTracker.normalize(name)))
            .toList();

    // ── Championship defenses ───────────────────────────────────────────────────
    // BEFORE rivalry matches: a title defense outranks mid-card feuds — a card with zero
    // defenses also lets the narration AI invent its own champion (the ATW Intertemporal bug).
    // Book the real thing: the actual champion defends against the actual contender when both
    // are bookable; overdue titles defend first (defense cadence is the whole point of the
    // frequency setting). When no contender is set, a top available rival from the idle pool
    // steps up.
    fillChampionshipDefenses(
        context,
        segments,
        matchBooked,
        bookedThisWeek,
        rosterSet,
        genderByName,
        idlePool,
        freeMatches);

    // ── Match slots: heat-sorted, non-overlapping, format-rotated ───────────────
    boolean preferTag = prefersTagThisWeek(showDate);
    for (ShowPlanningRivalryDTO rivalry : rivalries) {
      if (segments.stream().filter(LocalShowPlanner::isMatchRow).count() >= freeMatches) {
        break;
      }
      if (!isMatchEligible(classifications.get(rivalry.getId()))) {
        continue;
      }
      List<String> sides = rivalry.getParticipants();
      if (sides == null || sides.size() < 2 || sides.get(0) == null || sides.get(1) == null) {
        continue;
      }
      String a = sides.get(0);
      String b = sides.get(1);
      if (!isMatchAvailable(a, matchBooked, bookedThisWeek, rosterSet)
          || !isMatchAvailable(b, matchBooked, bookedThisWeek, rosterSet)) {
        log.debug(
            "Rivalry '{}' skipped for a match slot: a participant is already booked (card or"
                + " this week)",
            rivalry.getName());
        continue;
      }

      List<String> side1 = new ArrayList<>(List.of(a));
      List<String> side2 = new ArrayList<>(List.of(b));
      String type = WellKnownSegmentType.ONE_ON_ONE.getDisplayName();
      String aGender = genderByName.get(WeeklyParticipationTracker.normalize(a));
      String bGender = genderByName.get(WeeklyParticipationTracker.normalize(b));
      // A tag mixes BOTH sides on one row: with intergender disabled the two feud anchors must
      // share a gender, or the row fails approval's any-mixed-gender check.
      boolean tagPossible =
          preferTag
              && (intergenderAllowed
                  || aGender == null
                  || bGender == null
                  || aGender.equals(bGender));
      if (tagPossible) {
        // Tag Team: each feud side gains a fresh partner from the IDLE pool — partners drawn
        // from feuding wrestlers would starve those wrestlers' own pending feuds. Partners must
        // match their feud side's gender when intergender is disabled (approval rejects mixed
        // tags; the planner never builds one in the first place).
        List<String> partners =
            freshPartnersFor(
                idlePool, matchBooked, rosterSet, genderByName, a, b, intergenderAllowed, 2);
        if (partners.size() == 2) {
          side1.add(partners.get(0));
          side2.add(partners.get(1));
          type = WellKnownSegmentType.TAG_TEAM.getDisplayName();
        }
      }

      ProposedSegment row = new ProposedSegment();
      row.setType(type);
      row.setTeams(new ArrayList<>(List.of(side1, side2)));
      row.setRivalryId(rivalry.getId());
      row.setSource(SOURCE_LOCAL_PLANNER);
      row.setSummary(summarizeMatch(rivalry, type));
      segments.add(row);
      // Tag partners are now booked in a match on this card too.
      side1.forEach(name -> matchBooked.add(WeeklyParticipationTracker.normalize(name)));
      side2.forEach(name -> matchBooked.add(WeeklyParticipationTracker.normalize(name)));
    }

    // ── Idle-vs-idle backfill matches ───────────────────────────────────────────
    // Rivalries couldn't fill every match slot (skipped for bookings, or simply fewer feuds than
    // slots). A real booker fills the card: pair idle wrestlers so the show hits its match count
    // and under-used names get in-ring time. Singles pairings from the idle pool, one match per
    // wrestler still enforced, same-gender pairs when intergender is disabled.
    List<String> backfillPool =
        idlePool.stream()
            .filter(name -> isMatchAvailable(name, matchBooked, bookedThisWeek, rosterSet))
            .collect(Collectors.toCollection(ArrayList::new));
    while (segments.stream().filter(LocalShowPlanner::isMatchRow).count() < freeMatches
        && backfillPool.size() >= 2) {
      String anchor = backfillPool.get(0);
      String anchorGender = genderByName.get(WeeklyParticipationTracker.normalize(anchor));
      List<String> pair =
          backfillPool.stream()
              .skip(1)
              .filter(
                  name -> {
                    if (intergenderAllowed || anchorGender == null) {
                      return true;
                    }
                    String gender = genderByName.get(WeeklyParticipationTracker.normalize(name));
                    return anchorGender.equals(gender);
                  })
              .limit(1)
              .toList();
      if (pair.isEmpty()) {
        // No same-gender partner for the anchor — drop the anchor and try the next wrestler
        // (e.g. a lone female among males must not stall the fill).
        backfillPool.remove(0);
        continue;
      }
      String partner = pair.get(0);
      backfillPool.remove(anchor);
      backfillPool.remove(partner);
      ProposedSegment row = new ProposedSegment();
      row.setType(WellKnownSegmentType.ONE_ON_ONE.getDisplayName());
      row.setTeams(
          new ArrayList<>(
              List.of(new ArrayList<>(List.of(anchor)), new ArrayList<>(List.of(partner)))));
      row.setSource(SOURCE_LOCAL_PLANNER);
      row.setSummary(anchor + " vs " + partner + " — under-card bout booked by the local planner.");
      segments.add(row);
      matchBooked.add(WeeklyParticipationTracker.normalize(anchor));
      matchBooked.add(WeeklyParticipationTracker.normalize(partner));
    }

    // ── Promo / confrontation slots ─────────────────────────────────────────────
    int promoAssigned =
        fillConfrontationSlots(context, rivalries, classifications, segments, freePromos);
    promoAssigned +=
        fillBackfillPromos(
            segments, availableNames, tally, matchBooked, freePromos - promoAssigned);

    log.info(
        "Local planner built {} segment(s): {} match(es), {} promo/confrontation slot(s) of the"
            + " {} free",
        segments.size(),
        segments.stream().filter(LocalShowPlanner::isMatchRow).count(),
        promoAssigned,
        freeMatches + freePromos);
    return segments;
  }

  /** Row source marker on every locally planned segment. */
  public static final String SOURCE_LOCAL_PLANNER = "Local planner";

  /**
   * High-heat weekly feuds (PLE_RESOLUTION_REQUIRED) and mixed-gender feuds (CONFRONTATION_ONLY)
   * advance via confrontation promo slots — both feud participants in one Promo-type row. Talking
   * segments may share wrestlers who already matched: the one-match rule is match-specific.
   */
  private int fillConfrontationSlots(
      ShowPlanningContextDTO context,
      List<ShowPlanningRivalryDTO> rivalries,
      Map<Long, String> classifications,
      List<ProposedSegment> segments,
      int freePromos) {
    int assigned = 0;
    for (ShowPlanningRivalryDTO rivalry : rivalries) {
      if (assigned >= freePromos) {
        break;
      }
      String classification = classifications.get(rivalry.getId());
      if (!RivalryClassifier.PLE_RESOLUTION_REQUIRED.equals(classification)
          && !RivalryClassifier.CONFRONTATION_ONLY.equals(classification)) {
        continue;
      }
      List<String> participants = rivalry.getParticipants();
      if (participants == null || participants.size() < 2) {
        continue;
      }
      ProposedSegment row = new ProposedSegment();
      row.setType("Promo");
      row.setTeams(new ArrayList<>(List.of(new ArrayList<>(participants))));
      row.setRivalryId(rivalry.getId());
      row.setSource(SOURCE_LOCAL_PLANNER);
      row.setSummary(summarizeConfrontation(rivalry, classification));
      segments.add(row);
      assigned++;
    }
    return assigned;
  }

  /**
   * Participation backfill: under-served wrestlers (no recent match, then fewest promos) take the
   * remaining promo slots as solo promos. Wrestlers already in a match on this card are skipped —
   * one match plus a promo is legal, but the backfill exists precisely to surface names the card
   * hasn't seen.
   */
  private int fillBackfillPromos(
      List<ProposedSegment> segments,
      List<String> availableNames,
      Map<String, WeeklyParticipationTracker.Participation> tally,
      Set<String> matchBooked,
      int promoSlotsLeft) {
    int assigned = 0;
    Set<String> promoUsed = new HashSet<>();
    for (String name : tracker.underServedFirst(tally, availableNames)) {
      if (assigned >= promoSlotsLeft) {
        break;
      }
      String key = WeeklyParticipationTracker.normalize(name);
      if (promoUsed.contains(key) || matchBooked.contains(key)) {
        continue;
      }
      ProposedSegment row = new ProposedSegment();
      row.setType("Promo");
      row.setTeams(new ArrayList<>(List.of(new ArrayList<>(List.of(name)))));
      row.setSource(SOURCE_LOCAL_PLANNER);
      segments.add(row);
      promoUsed.add(key);
      assigned++;
    }
    return assigned;
  }

  /**
   * Championship defense pass (ATW-wbch): every active title on the card's universe defends when
   * its champion and a challenger can both take a match slot. Overdue titles claim slots first —
   * the defense cadence exists to force these bookings. The title is attached by id ({@code
   * titleId}) and resolved to the entity at approval, so the proposed card shows a REAL title
   * defense instead of leaving the narration AI to invent champions.
   *
   * <p>Challenger selection, in order: the title's designated contender (bookable), else the
   * highest-tier available idle wrestler of the champion's gender. The defense consumes a match
   * slot only when one is free; when the card is out of match slots, nothing is booked (the
   * narration prompt already carries championship context for flavor).
   */
  private void fillChampionshipDefenses(
      ShowPlanningContextDTO context,
      List<ProposedSegment> segments,
      Set<String> matchBooked,
      Set<String> bookedThisWeek,
      Set<String> rosterSet,
      Map<String, String> genderByName,
      List<String> idlePool,
      int freeMatches) {
    if (context.getChampionships() == null || context.getChampionships().isEmpty()) {
      return;
    }
    // Overdue titles first, then by days-since-defense descending (longest drought defends first).
    List<ShowPlanningChampionshipDTO> titles = new ArrayList<>(context.getChampionships());
    titles.sort(
        (a, b) -> {
          if (a.isOverdue() != b.isOverdue()) {
            return a.isOverdue() ? -1 : 1;
          }
          return Long.compare(
              b.getDaysSinceLastDefense() == null ? 0 : b.getDaysSinceLastDefense(),
              a.getDaysSinceLastDefense() == null ? 0 : a.getDaysSinceLastDefense());
        });
    // Champions of OTHER titles never challenge here: champion-vs-champion is a PLE main-event
    // caliber match (field bug: the Intertemporal defense consumed the World champion André,
    // blocking his own World defense on the same card).
    Set<String> allChampions =
        titles.stream()
            .map(ShowPlanningChampionshipDTO::getChampionName)
            .filter(name -> name != null && !name.isBlank())
            .map(WeeklyParticipationTracker::normalize)
            .collect(Collectors.toSet());
    for (ShowPlanningChampionshipDTO title : titles) {
      if (segments.stream().filter(LocalShowPlanner::isMatchRow).count() >= freeMatches) {
        break;
      }
      if (title.getChampionName() == null
          || title.getChampionName().isBlank()
          || title.getTitleId() == null) {
        continue; // vacant or unresolvable titles skip this pass
      }
      String champion = title.getChampionName();
      // Multi-wrestler champions ("A & B") don't fit a one-side name — skip tag titles here.
      if (!rosterSet.contains(WeeklyParticipationTracker.normalize(champion))) {
        continue;
      }
      String challenger = pickChallenger(title, champion, idlePool, rosterSet, genderByName);
      if (challenger != null
          && allChampions.contains(WeeklyParticipationTracker.normalize(challenger))) {
        log.debug(
            "Challenger '{}' skipped for '{}': champion of another title — champion-vs-champion"
                + " is PLE material",
            challenger,
            title.getChampionshipName());
        challenger =
            pickChallengerExcludingChampions(
                title, champion, idlePool, rosterSet, genderByName, allChampions);
      }
      if (challenger == null
          || !isMatchAvailable(champion, matchBooked, bookedThisWeek, rosterSet)
          || !isMatchAvailable(challenger, matchBooked, bookedThisWeek, rosterSet)) {
        log.debug(
            "Title '{}' skipped: champion or challenger already booked (card or this week)",
            title.getChampionshipName());
        continue;
      }

      ProposedSegment row = new ProposedSegment();
      row.setType(WellKnownSegmentType.ONE_ON_ONE.getDisplayName());
      row.setTeams(
          new ArrayList<>(
              List.of(new ArrayList<>(List.of(champion)), new ArrayList<>(List.of(challenger)))));
      row.setTitleId(title.getTitleId());
      row.setIsTitleSegment(true);
      row.setSource(SOURCE_LOCAL_PLANNER);
      row.setSummary(
          String.format(
              "%s defense: %s (champion) vs %s%s.",
              title.getChampionshipName(),
              champion,
              challenger,
              title.getContenderName() != null && !title.getContenderName().isBlank()
                  ? " — designated contender"
                  : " — top available challenger"));
      segments.add(row);
      matchBooked.add(WeeklyParticipationTracker.normalize(champion));
      matchBooked.add(WeeklyParticipationTracker.normalize(challenger));
      log.info("Booked {} defense: {} vs {}", title.getChampionshipName(), champion, challenger);
    }
  }

  /**
   * Challenger for a defense, in priority order: the title's designated contender when bookable,
   * then the top RANKED bookable contender ({@code rankedContenderNames} — RankingService's
   * tier/cooldown/fans doctrine), then the top idle wrestler of the champion's gender as a fallback
   * when the rankings are empty. Same-gender when intergender is disabled; unknown genders pass.
   */
  private String pickChallenger(
      ShowPlanningChampionshipDTO title,
      String champion,
      List<String> idlePool,
      Set<String> rosterSet,
      Map<String, String> genderByName) {
    String contender = title.getContenderName();
    if (contender != null
        && !contender.isBlank()
        && rosterSet.contains(WeeklyParticipationTracker.normalize(contender))) {
      return contender.trim();
    }
    // Ranked contenders — real rankings (fans, tier, cooldowns), not roster order.
    for (String ranked : title.getRankedContenderNames()) {
      String key = WeeklyParticipationTracker.normalize(ranked);
      if (!rosterSet.contains(key) || WeeklyParticipationTracker.normalize(champion).equals(key)) {
        continue;
      }
      String gender = genderByName.get(key);
      if (championGenderBlocks(champion, gender, genderByName)) {
        continue;
      }
      return ranked;
    }
    // Fallback: idle pool in roster order (hand-built contexts without rankings).
    String championGender = genderByName.get(WeeklyParticipationTracker.normalize(champion));
    for (String candidate : idlePool) {
      String key = WeeklyParticipationTracker.normalize(candidate);
      if (!rosterSet.contains(key)) {
        continue;
      }
      if (WeeklyParticipationTracker.normalize(champion).equals(key)) {
        continue; // champion can't challenge themselves
      }
      String gender = genderByName.get(key);
      if (championGender != null && !championGender.equals(gender)) {
        continue; // same-gender gate
      }
      return candidate;
    }
    return null;
  }

  /** True when the candidate's gender differs from the champion's under the current policy. */
  private boolean championGenderBlocks(
      String champion, String candidateGender, Map<String, String> genderByName) {
    // The intergender flag itself is enforced at pairing time by the caller; here only the
    // same-gender preference for challenger selection applies.
    String championGender = genderByName.get(WeeklyParticipationTracker.normalize(champion));
    return championGender != null && !championGender.equals(candidateGender);
  }

  /**
   * Re-pick with the other-titles' champions excluded from every tier of the search (designated
   * contender through ranked list through idle fallback).
   */
  private String pickChallengerExcludingChampions(
      ShowPlanningChampionshipDTO title,
      String champion,
      List<String> idlePool,
      Set<String> rosterSet,
      Map<String, String> genderByName,
      Set<String> excludedChampions) {
    String contender = title.getContenderName();
    if (contender != null
        && !contender.isBlank()
        && !excludedChampions.contains(WeeklyParticipationTracker.normalize(contender))
        && rosterSet.contains(WeeklyParticipationTracker.normalize(contender))) {
      return contender.trim();
    }
    for (String ranked : title.getRankedContenderNames()) {
      String key = WeeklyParticipationTracker.normalize(ranked);
      if (!rosterSet.contains(key)
          || excludedChampions.contains(key)
          || WeeklyParticipationTracker.normalize(champion).equals(key)) {
        continue;
      }
      if (championGenderBlocks(champion, genderByName.get(key), genderByName)) {
        continue;
      }
      return ranked;
    }
    String championGender = genderByName.get(WeeklyParticipationTracker.normalize(champion));
    for (String candidate : idlePool) {
      String key = WeeklyParticipationTracker.normalize(candidate);
      if (!rosterSet.contains(key)
          || excludedChampions.contains(key)
          || WeeklyParticipationTracker.normalize(champion).equals(key)) {
        continue;
      }
      if (championGender != null && !championGender.equals(genderByName.get(key))) {
        continue;
      }
      return candidate;
    }
    return null;
  }

  /**
   * Heat-based stipulations (ATW-wbch): match rows the planner built without a rule get one from
   * the seeded catalog — heat ≥ {@link RivalryClassifier#PLE_RESOLUTION_HEAT} draws a {@code
   * requiresHighHeat} rule (Cage, No DQ, Last Man Standing…), everything else gets "Normal".
   * Rotation over the eligible list keeps consecutive high-heat matches from all landing on the
   * same stipulation. Approval VALIDATES this: heat ≥ 30 matches without a stipulation are rejected
   * by ShowPlanningService.validateCard.
   */
  void applyHeatBasedStipulations(List<ProposedSegment> segments, ShowPlanningContextDTO context) {
    List<SegmentRule> highHeatRules =
        segmentRuleService.getHighHeatRules() == null
            ? List.of()
            : segmentRuleService.getHighHeatRules();
    if (highHeatRules.isEmpty()) {
      return; // catalog empty — applyDefaultRules' Normal/Promo fallback still applies
    }
    int stipulationIndex = 0;
    for (ProposedSegment row : segments) {
      if (row.getRules() != null && !row.getRules().isEmpty()) {
        continue; // already stipulated (scripted beat, tournament row)
      }
      if (isPromoRow(row)) {
        continue;
      }
      int heat = rivalryHeatOf(context, row.getRivalryId());
      if (heat >= RivalryClassifier.PLE_RESOLUTION_HEAT) {
        SegmentRule rule = highHeatRules.get(stipulationIndex % highHeatRules.size());
        stipulationIndex++;
        row.setRules(List.of(rule.getName()));
        row.setSummary(
            (row.getSummary() == null ? "" : row.getSummary() + " ") + "[" + rule.getName() + "]");
      }
      // Sub-threshold matches stay ruleless here — applyDefaultRules backfills "Normal".
    }
  }

  /** Heat of the rivalry a row advances; 0 when the row isn't rivalry-backed. */
  private int rivalryHeatOf(ShowPlanningContextDTO context, Long rivalryId) {
    if (rivalryId == null || context.getCurrentRivalries() == null) {
      return 0;
    }
    return context.getCurrentRivalries().stream()
        .filter(r -> rivalryId.equals(r.getId()))
        .findFirst()
        .map(ShowPlanningRivalryDTO::getHeat)
        .orElse(0);
  }

  private static boolean isPromoRow(ProposedSegment row) {
    return row.getType() != null && row.getType().toLowerCase(Locale.ROOT).contains("promo");
  }

  /**
   * Whether the wrestler may take a match slot: on the roster, unbooked on-card, unbooked this
   * week.
   */
  private boolean isMatchAvailable(
      String name, Set<String> matchBooked, Set<String> bookedThisWeek, Set<String> rosterSet) {
    String key = WeeklyParticipationTracker.normalize(name);
    return rosterSet.contains(key) && !matchBooked.contains(key) && !bookedThisWeek.contains(key);
  }

  /**
   * Finds up to {@code count} fresh partners from the idle pool: on the roster, unbooked, and —
   * when intergender is disabled — the same gender as the feud side they'll team with (partner 0
   * teams with side1's anchor, partner 1 with side2's). Unknown genders (hand-built contexts) pass
   * the gate. Deterministic order (pool order) keeps output stable for tests and repeat runs.
   */
  private List<String> freshPartnersFor(
      List<String> idlePool,
      Set<String> matchBooked,
      Set<String> rosterSet,
      Map<String, String> genderByName,
      String side1Anchor,
      String side2Anchor,
      boolean intergenderAllowed,
      int count) {
    String side1Gender = genderByName.get(WeeklyParticipationTracker.normalize(side1Anchor));
    String side2Gender = genderByName.get(WeeklyParticipationTracker.normalize(side2Anchor));
    List<String> partners = new ArrayList<>();
    for (String candidate : idlePool) {
      if (partners.size() >= count) {
        break;
      }
      String key = WeeklyParticipationTracker.normalize(candidate);
      if (matchBooked.contains(key) || !rosterSet.contains(key)) {
        continue;
      }
      if (!intergenderAllowed) {
        String candidateGender = genderByName.get(key);
        String requiredGender = partners.isEmpty() ? side1Gender : side2Gender;
        if (requiredGender != null && !requiredGender.equals(candidateGender)) {
          continue;
        }
      }
      partners.add(candidate);
    }
    return partners;
  }

  private Map<Long, String> classificationsOf(
      ShowPlanningContextDTO context, List<ShowPlanningRivalryDTO> rivalries) {
    Map<String, String> genderByName = new HashMap<>();
    if (context.getFullRoster() != null) {
      context
          .getFullRoster()
          .forEach(
              w -> {
                if (w.getName() != null) {
                  genderByName.put(w.getName().trim(), w.getGender());
                }
              });
    }
    Map<Long, String> classifications = new HashMap<>();
    for (ShowPlanningRivalryDTO rivalry : rivalries) {
      classifications.put(
          rivalry.getId(),
          RivalryClassifier.classify(
              rivalry,
              context.isIntergenderAllowed(),
              RivalryClassifier.isMixedGender(rivalry, genderByName),
              context.isPremiumLiveEvent()));
    }
    return classifications;
  }

  /**
   * Rivalries with heat ≥ 10 (the context is already scrubbed/filtered upstream), hottest first.
   */
  private List<ShowPlanningRivalryDTO> rivalriesByHeat(ShowPlanningContextDTO context) {
    if (context.getCurrentRivalries() == null) {
      return List.of();
    }
    return context.getCurrentRivalries().stream()
        .filter(r -> r.getHeat() >= 10)
        .sorted((a, b) -> Integer.compare(b.getHeat(), a.getHeat()))
        .toList();
  }

  private boolean isMatchEligible(String classification) {
    return RivalryClassifier.MUST_BOOK.equals(classification)
        || RivalryClassifier.PROMO_COOLDOWN.equals(classification)
        || RivalryClassifier.PLE_RESOLUTION_ELIGIBLE.equals(classification)
        // On a PLE, heat ≥ 30 IS the stipulation match.
        || RivalryClassifier.STIPULATION_REQUIRED.equals(classification);
  }

  /**
   * Week parity for format rotation: ISO week-of-year from the show date (day-of-year / 7 keeps the
   * parity stable and needs no locale-sensitive week fields). Consecutive weeks flip.
   */
  static boolean prefersTagThisWeek(Instant showDate) {
    if (showDate == null) {
      return false;
    }
    int dayOfYear = showDate.atZone(ZoneId.of("UTC")).toLocalDate().get(ChronoField.DAY_OF_YEAR);
    return (dayOfYear / 7) % 2 == 0;
  }

  private static boolean isMatchRow(ProposedSegment row) {
    return row.getType() != null && !row.getType().toLowerCase(Locale.ROOT).contains("promo");
  }

  private static String summarizeMatch(ShowPlanningRivalryDTO rivalry, String type) {
    return String.format(
        "%s (heat %d) — %s booked by the local planner; AI narrates the story.",
        rivalry.getName(), rivalry.getHeat(), type);
  }

  private static String summarizeConfrontation(
      ShowPlanningRivalryDTO rivalry, String classification) {
    if (RivalryClassifier.PLE_RESOLUTION_REQUIRED.equals(classification)) {
      return String.format(
          "%s (heat %d) confrontation — the decisive match is reserved for the next PLE.",
          rivalry.getName(), rivalry.getHeat());
    }
    return String.format(
        "%s confrontation — intergender matches are disabled, the feud advances out of the ring.",
        rivalry.getName());
  }
}
