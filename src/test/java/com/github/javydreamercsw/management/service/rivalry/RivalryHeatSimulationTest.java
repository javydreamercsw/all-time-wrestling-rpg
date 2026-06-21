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
package com.github.javydreamercsw.management.service.rivalry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Simulates heat accumulation and resolution over a season of shows using the current production
 * rules, then re-runs with proposed fixes to compare outcomes.
 *
 * <p>Key questions answered: - How many rivalries exist after N shows under current rules? - Does
 * heat ever resolve, or does it only accumulate? - Which proposed changes most effectively keep the
 * count manageable?
 *
 * <p>This is a pure-Java simulation — no Spring context needed.
 */
@DisplayName("Rivalry Heat Growth vs. Resolution Simulation")
class RivalryHeatSimulationTest {

  // Roster and show config
  private static final int ROSTER_SIZE = 20;
  private static final int WEEKS = 52; // full year
  private static final int SEGMENTS_PER_SHOW = 6; // typical weekly show
  private static final int WRESTLERS_PER_SEGMENT = 4; // average tag match / multi-man
  private static final int PLE_EVERY_N_WEEKS = 4;

  // Current production heat values (from SegmentAdjudicationService)
  private static final int MATCH_HEAT = 1;
  private static final int PROMO_HEAT = 4;

  // Resolution thresholds (from GameSettingService defaults / DB)
  private static final int RESOLUTION_THRESHOLD_PLE = 30;
  private static final int RESOLUTION_THRESHOLD_REGULAR = 35;
  private static final int HEAT_NEEDED_TO_TRY = 20;

  private List<String> roster;

  @BeforeEach
  void setUp() {
    roster = new ArrayList<>();
    for (int i = 1; i <= ROSTER_SIZE; i++) {
      roster.add("W" + i);
    }
  }

  // -------------------------------------------------------------------------
  // Simulation engine
  // -------------------------------------------------------------------------

  /**
   * @param heatDecayEnabled whether decay runs nightly
   * @param decayAmount heat removed per interval when stale
   * @param decayIntervalDays days of inactivity before decay fires
   * @param maxRivalryDurationDays auto-close after this many in-show-days (0 = disabled)
   * @param requireExistingRivalryForHeat only add heat if a rivalry already exists (don't
   *     auto-create on every pair)
   * @param promoCreatesRivalry whether promos auto-create new rivalries
   * @param regularShowResolution whether regular (non-PLE) shows also attempt resolution
   * @param heatNeededToTry minimum heat before resolution is attempted
   * @param pleTreshold d20+d20 sum needed to resolve at PLE
   * @param regularThreshold d20+d20 sum needed to resolve on regular shows
   */
  private SimResult simulate(
      boolean heatDecayEnabled,
      int decayAmount,
      int decayIntervalDays,
      int maxRivalryDurationDays,
      boolean requireExistingRivalryForHeat,
      boolean promoCreatesRivalry,
      boolean regularShowResolution) {
    return simulate(
        heatDecayEnabled,
        decayAmount,
        decayIntervalDays,
        maxRivalryDurationDays,
        requireExistingRivalryForHeat,
        promoCreatesRivalry,
        regularShowResolution,
        HEAT_NEEDED_TO_TRY,
        RESOLUTION_THRESHOLD_PLE,
        RESOLUTION_THRESHOLD_REGULAR);
  }

  private SimResult simulate(
      boolean heatDecayEnabled,
      int decayAmount,
      int decayIntervalDays,
      int maxRivalryDurationDays,
      boolean requireExistingRivalryForHeat,
      boolean promoCreatesRivalry,
      boolean regularShowResolution,
      int heatNeededToTry,
      int pleThreshold,
      int regularThreshold) {

    // Each simulate() call uses a fresh fixed-seed RNG for reproducible, independent results.
    Random rng = new Random(42);

    // Map "W1|W2" → heat (active only; removed when rivalry ends)
    Map<String, Integer> rivalries = new HashMap<>();
    // Track when a rivalry last had a heat event (in show-day units)
    Map<String, Integer> lastActivity = new HashMap<>();
    // Track when a rivalry started (in show-day units)
    Map<String, Integer> startDay = new HashMap<>();

    int totalResolved = 0;
    int totalCreated = 0;
    int totalDecayed = 0;

    for (int week = 1; week <= WEEKS; week++) {
      boolean isPle = (week % PLE_EVERY_N_WEEKS == 0);
      int showDay = week * 7;

      // --- 1. Run show segments ---
      for (int seg = 0; seg < SEGMENTS_PER_SHOW; seg++) {
        boolean isPromo = rng.nextInt(5) == 0; // ~20% promos
        int heat = isPromo ? PROMO_HEAT : MATCH_HEAT;

        // Pick random participants
        List<String> participants = pickRandom(roster, WRESTLERS_PER_SEGMENT, rng);

        for (int i = 0; i < participants.size(); i++) {
          for (int j = i + 1; j < participants.size(); j++) {
            String key = rivalryKey(participants.get(i), participants.get(j));
            boolean exists = rivalries.containsKey(key);

            if (!exists) {
              // Current rules: always create. Fixed rule: only create for promos or targeted.
              boolean shouldCreate =
                  !requireExistingRivalryForHeat || isPromo && promoCreatesRivalry;
              if (shouldCreate) {
                rivalries.put(key, 0);
                startDay.put(key, showDay);
                totalCreated++;
                exists = true;
              }
            }

            if (exists) {
              int newHeat = rivalries.get(key) + heat;
              rivalries.put(key, newHeat);
              lastActivity.put(key, showDay);
            }
          }
        }
      }

      // --- 2. Attempt resolutions (PLEs always try; regular shows only if configured) ---
      int threshold = isPle ? pleThreshold : regularThreshold;
      boolean tryResolution = isPle || regularShowResolution;

      if (tryResolution) {
        List<String> toResolve =
            rivalries.entrySet().stream()
                .filter(e -> e.getValue() >= heatNeededToTry)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (String key : toResolve) {
          int roll1 = rng.nextInt(20) + 1;
          int roll2 = rng.nextInt(20) + 1;
          if (roll1 + roll2 >= threshold) {
            rivalries.remove(key);
            lastActivity.remove(key);
            startDay.remove(key);
            totalResolved++;
          }
        }
      }

      // --- 3. Apply heat decay (nightly — approximated as once per week) ---
      if (heatDecayEnabled) {
        int staleThreshold = showDay - decayIntervalDays;
        for (String key : new ArrayList<>(rivalries.keySet())) {
          int last = lastActivity.getOrDefault(key, 0);
          if (last < staleThreshold && rivalries.get(key) > 0) {
            int decayed = Math.max(0, rivalries.get(key) - decayAmount);
            rivalries.put(key, decayed);
            totalDecayed++;
            if (decayed == 0) {
              rivalries.remove(key);
              lastActivity.remove(key);
              startDay.remove(key);
            }
          }
        }
      }

      // --- 4. Auto-close expired rivalries ---
      if (maxRivalryDurationDays > 0) {
        int expiryDay = showDay - maxRivalryDurationDays;
        for (String key : new ArrayList<>(rivalries.keySet())) {
          int started = startDay.getOrDefault(key, showDay);
          if (started < expiryDay) {
            rivalries.remove(key);
            lastActivity.remove(key);
            startDay.remove(key);
            totalResolved++; // counts as expired/ended
          }
        }
      }
    }

    // Collect final state
    int activeCount = rivalries.size();
    double avgHeat =
        rivalries.isEmpty()
            ? 0
            : rivalries.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    int maxHeat = rivalries.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    long hotRivalries = rivalries.values().stream().filter(h -> h >= HEAT_NEEDED_TO_TRY).count();

    return new SimResult(
        activeCount, totalCreated, totalResolved, totalDecayed, avgHeat, maxHeat, hotRivalries);
  }

  // -------------------------------------------------------------------------
  // Scenarios
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Baseline: current production rules (no decay, no expiry, all-pairs heat)")
  void baseline_currentRules() {
    SimResult result =
        simulate(
            false, // decay disabled
            1, 7, 0, // no max duration
            false, // always create rivalries
            true, false); // no regular-show resolution

    printResult("BASELINE (current rules, 52 weeks)", result);

    // With 20 wrestlers max unique pairs = 190; all-pairs creation saturates within a season.
    assertThat(result.activeCount())
        .as("Current rules should produce excessive active rivalries over a year")
        .isGreaterThan(100);
    assertThat(result.totalResolved())
        .as("Few rivalries should resolve without decay over PLE-only resolution")
        .isLessThan(result.totalCreated() / 4);
  }

  @Test
  @DisplayName("Fix 1: Enable heat decay (7-day interval, 2 heat/interval)")
  void fix1_enableDecay() {
    SimResult baseline = simulate(false, 1, 7, 0, false, true, false);
    SimResult result = simulate(true, 2, 7, 0, false, true, false);

    printResult("FIX 1: Enable decay (2/7d)", result);

    assertThat(result.activeCount()).isLessThan(baseline.activeCount());
    assertThat(result.avgHeat()).isLessThan(baseline.avgHeat());
  }

  @Test
  @DisplayName("Fix 2: Only create rivalries when wrestlers share a promo, not every match")
  void fix2_promoOnlyCreation() {
    SimResult baseline = simulate(false, 1, 7, 0, false, true, false);
    SimResult result = simulate(false, 1, 7, 0, true, true, false);

    printResult("FIX 2: Promo-only creation", result);

    assertThat(result.activeCount()).isLessThan(baseline.activeCount());
    assertThat(result.hotRivalries()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Fix 3: Max rivalry duration of 90 days auto-closes stale feuds")
  void fix3_maxDuration() {
    SimResult baseline = simulate(false, 1, 7, 0, false, true, false);
    SimResult result = simulate(false, 1, 7, 90, false, true, false);

    printResult("FIX 3: 90-day max duration", result);

    assertThat(result.activeCount()).isLessThan(baseline.activeCount());
    assertThat(result.totalResolved()).isGreaterThan(baseline.totalResolved());
  }

  @Test
  @DisplayName("Fix 4: Combined — decay + promo-only creation + 90-day expiry (no regular res.)")
  void fix4_combined_no_regular_resolution() {
    SimResult result = simulate(true, 2, 7, 90, true, true, false);

    printResult("FIX 4: COMBINED, PLE-only resolution", result);

    assertThat(result.activeCount())
        .as("Combined fixes should keep active rivalries manageable over a year")
        .isLessThan(50);
    assertThat(result.totalResolved() + result.totalDecayed())
        .as("Most created rivalries should be cleaned up")
        .isGreaterThan(result.totalCreated() / 2);
  }

  @Test
  @DisplayName("Fix 5: Combined + regular-show resolution enabled")
  void fix5_combined_with_regular_resolution() {
    SimResult withoutRegular = simulate(true, 2, 7, 90, true, true, false);
    SimResult withRegular = simulate(true, 2, 7, 90, true, true, true);

    printResult("FIX 5: COMBINED + regular resolution", withRegular);

    // Regular-show resolution threshold (35) is *higher* than PLE (30), so it resolves
    // similar or fewer hot rivalries formally — but attempts happen every week, so total
    // cleanup (resolved + decayed) should be at least as good.
    assertThat(withRegular.totalResolved() + withRegular.totalDecayed())
        .as("Regular resolution should not make overall cleanup worse")
        .isGreaterThanOrEqualTo(withoutRegular.totalResolved() + withoutRegular.totalDecayed() - 5);
    // Active pool should be in the same ballpark
    assertThat(withRegular.activeCount())
        .as("Active pool with regular resolution should stay manageable")
        .isLessThan(60);
  }

  @Test
  @DisplayName("Fix 6: Lower heat-needed-to-try (20→10) — more rivalries qualify for resolution")
  void fix6_lowerHeatThreshold() {
    // With current defaults only rivalries reaching 20 heat get a resolution roll.
    // Dropping to 10 lets younger rivalries resolve earlier.
    SimResult current = simulate(true, 2, 7, 90, true, true, false);
    SimResult lowered =
        simulate(
            true,
            2,
            7,
            90,
            true,
            true,
            false,
            10,
            RESOLUTION_THRESHOLD_PLE,
            RESOLUTION_THRESHOLD_REGULAR);

    printResult("FIX 6: heat-needed-to-try = 10 (was 20)", lowered);

    assertThat(lowered.totalResolved())
        .as("Lowering heat threshold should produce more formal resolutions")
        .isGreaterThan(current.totalResolved());
    assertThat(lowered.activeCount()).as("Active pool should be manageable").isLessThan(50);
  }

  @Test
  @DisplayName("Fix 7: Regular-show resolution with a reachable threshold (25, not 35)")
  void fix7_regularResolutionLowerThreshold() {
    // Regular threshold 35 on 2d20 (max 40) resolves only ~6% of the time.
    // Dropping to 25 gives ~45% per attempt — much more useful on weekly shows.
    SimResult pleOnly =
        simulate(
            true,
            2,
            7,
            90,
            true,
            true,
            false,
            10,
            RESOLUTION_THRESHOLD_PLE,
            RESOLUTION_THRESHOLD_REGULAR);
    SimResult regularLow =
        simulate(true, 2, 7, 90, true, true, true, 10, RESOLUTION_THRESHOLD_PLE, 25);

    printResult("FIX 7: regular-show res. threshold = 25 + heat-floor = 10", regularLow);

    // With a reachable regular threshold, weekly resolution attempts whittle the active pool
    // faster — the active count should beat PLE-only even if some resolutions shift to decay.
    assertThat(regularLow.activeCount())
        .as("Lower regular-show threshold should further reduce the active rivalry pool")
        .isLessThan(pleOnly.activeCount());
    assertThat(regularLow.activeCount())
        .as("Active rivalry pool should stay very low")
        .isLessThan(20);
  }

  @Test
  @DisplayName("Summary: print full comparison table (52-week season)")
  void summary_comparisonTable() {
    record Scenario(
        String label,
        boolean decay,
        int decayAmt,
        int decayDays,
        int maxDays,
        boolean requireExisting,
        boolean promoCreates,
        boolean regularRes,
        int heatFloor,
        int pleT,
        int regT) {}

    List<Scenario> scenarios =
        List.of(
            new Scenario("Baseline (current)", false, 1, 7, 0, false, true, false, 20, 30, 35),
            new Scenario("Decay only (2/7d)", true, 2, 7, 0, false, true, false, 20, 30, 35),
            new Scenario("Decay only (5/7d)", true, 5, 7, 0, false, true, false, 20, 30, 35),
            new Scenario("Promo-only creation", false, 1, 7, 0, true, true, false, 20, 30, 35),
            new Scenario("Max duration 90d", false, 1, 7, 90, false, true, false, 20, 30, 35),
            new Scenario("Max duration 60d", false, 1, 7, 60, false, true, false, 20, 30, 35),
            new Scenario("Decay + 90d expiry", true, 2, 7, 90, false, true, false, 20, 30, 35),
            new Scenario("Promo + 90d expiry", false, 1, 7, 90, true, true, false, 20, 30, 35),
            new Scenario("COMBINED (PLE-only)", true, 2, 7, 90, true, true, false, 20, 30, 35),
            new Scenario("COMBINED + reg.res.", true, 2, 7, 90, true, true, true, 20, 30, 35),
            new Scenario("Fix6: heat-floor=10", true, 2, 7, 90, true, true, false, 10, 30, 35),
            new Scenario("Fix7: floor=10+reg.th=25", true, 2, 7, 90, true, true, true, 10, 30, 25));

    System.out.println("\n" + "=".repeat(115));
    System.out.printf(
        "%-38s %8s %8s %8s %8s %8s %8s%n",
        "Scenario", "Active", "Created", "Resolved", "Decayed", "AvgHeat", "Hot(20+)");
    System.out.println("=".repeat(115));

    SimResult baseline = null;
    for (Scenario s : scenarios) {
      SimResult r =
          simulate(
              s.decay(),
              s.decayAmt(),
              s.decayDays(),
              s.maxDays(),
              s.requireExisting(),
              s.promoCreates(),
              s.regularRes(),
              s.heatFloor(),
              s.pleT(),
              s.regT());
      if (baseline == null) {
        baseline = r;
      }
      System.out.printf(
          "%-38s %8d %8d %8d %8d %8.1f %8d%n",
          s.label(),
          r.activeCount(),
          r.totalCreated(),
          r.totalResolved(),
          r.totalDecayed(),
          r.avgHeat(),
          r.hotRivalries());
    }
    System.out.println("=".repeat(115));
    System.out.printf(
        "Roster: %d wrestlers | Shows: %d weekly + %d PLEs | Segments/show: %d |"
            + " Wrestlers/segment: %d%n",
        ROSTER_SIZE,
        WEEKS - WEEKS / PLE_EVERY_N_WEEKS,
        WEEKS / PLE_EVERY_N_WEEKS,
        SEGMENTS_PER_SHOW,
        WRESTLERS_PER_SEGMENT);

    // Combined fix must beat baseline on active count; total cleanup (resolved+decayed) must be
    // higher even if formal resolution count is lower (decay handles most of the closure).
    SimResult combined = simulate(true, 2, 7, 90, true, true, false);
    assertThat(combined.activeCount()).isLessThan(baseline.activeCount());
    assertThat(combined.totalResolved() + combined.totalDecayed())
        .isGreaterThan(baseline.totalResolved() + baseline.totalDecayed());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private List<String> pickRandom(final List<String> source, final int count, final Random rng) {
    List<String> shuffled = new ArrayList<>(source);
    java.util.Collections.shuffle(shuffled, rng);
    return shuffled.subList(0, Math.min(count, shuffled.size()));
  }

  private String rivalryKey(final String a, final String b) {
    return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
  }

  private void printResult(final String label, final SimResult r) {
    System.out.printf(
        "%n[%s]%n  Active: %d | Created: %d | Resolved: %d | Decayed: %d | AvgHeat: %.1f"
            + " | MaxHeat: %d | Hot(20+): %d%n",
        label,
        r.activeCount(),
        r.totalCreated(),
        r.totalResolved(),
        r.totalDecayed(),
        r.avgHeat(),
        r.maxHeat(),
        r.hotRivalries());
  }

  record SimResult(
      int activeCount,
      int totalCreated,
      int totalResolved,
      int totalDecayed,
      double avgHeat,
      int maxHeat,
      long hotRivalries) {}
}
