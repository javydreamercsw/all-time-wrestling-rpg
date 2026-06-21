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
  private static final int WEEKS = 26; // 6-month season
  private static final int SEGMENTS_PER_SHOW = 6; // typical weekly show
  private static final int WRESTLERS_PER_SEGMENT = 4; // average tag match / multi-man
  private static final int PLE_EVERY_N_WEEKS = 4;

  // Current production heat values (from SegmentAdjudicationService)
  private static final int MATCH_HEAT = 1;
  private static final int PROMO_HEAT = 4;
  private static final int PLE_AI_TARGETED_HEAT = 3;
  private static final int REGULAR_AI_TARGETED_HEAT = 2;

  // Resolution thresholds (from GameSettingService defaults / DB)
  private static final int RESOLUTION_THRESHOLD_PLE = 30;
  private static final int RESOLUTION_THRESHOLD_REGULAR = 35;
  private static final int HEAT_NEEDED_TO_TRY = 20;

  private List<String> roster;
  private Random rng;

  @BeforeEach
  void setUp() {
    rng = new Random(42); // fixed seed for reproducibility
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
   */
  private SimResult simulate(
      boolean heatDecayEnabled,
      int decayAmount,
      int decayIntervalDays,
      int maxRivalryDurationDays,
      boolean requireExistingRivalryForHeat,
      boolean promoCreatesRivalry) {

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
        List<String> participants = pickRandom(roster, WRESTLERS_PER_SEGMENT);

        for (int i = 0; i < participants.size(); i++) {
          for (int j = i + 1; j < participants.size(); j++) {
            String key = rivalryKey(participants.get(i), participants.get(j));
            boolean exists = rivalries.containsKey(key);

            if (!exists) {
              // Current rules: always create. Fixed rule: only create for promos or targeted.
              boolean shouldCreate =
                  !requireExistingRivalryForHeat || (isPromo && promoCreatesRivalry);
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
      int threshold = isPle ? RESOLUTION_THRESHOLD_PLE : RESOLUTION_THRESHOLD_REGULAR;
      boolean tryResolution = isPle || !requireExistingRivalryForHeat; // regular shows resolve too

      if (tryResolution) {
        List<String> toResolve =
            rivalries.entrySet().stream()
                .filter(e -> e.getValue() >= HEAT_NEEDED_TO_TRY)
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
            true);

    printResult("BASELINE (current rules)", result);

    // Assert the known problem: rivalries balloon to near the max possible pairs
    // With 20 wrestlers, max unique pairs = 190. After 26 shows we expect > 100 active.
    assertThat(result.activeCount())
        .as("Current rules should produce excessive active rivalries")
        .isGreaterThan(80);
    assertThat(result.totalResolved())
        .as("Few or zero rivalries should resolve without decay")
        .isLessThan(result.totalCreated() / 4);
  }

  @Test
  @DisplayName("Fix 1: Enable heat decay (7-day interval, 2 heat/interval)")
  void fix1_enableDecay() {
    SimResult result =
        simulate(
            true, // decay enabled
            2, // 2 heat per interval
            7, // 7-day interval
            0, false, true);

    printResult("FIX 1: Enable decay (2/7d)", result);

    // Decay alone should reduce avg heat and final count vs baseline
    SimResult baseline = simulate(false, 1, 7, 0, false, true);
    assertThat(result.activeCount()).isLessThan(baseline.activeCount());
    assertThat(result.avgHeat()).isLessThan(baseline.avgHeat());
  }

  @Test
  @DisplayName("Fix 2: Only create rivalries when wrestlers share a promo, not every match")
  void fix2_promoOnlyCreation() {
    SimResult result =
        simulate(
            false, 1, 7, 0, true, // don't auto-create on match — require existing or promo
            true);

    printResult("FIX 2: Promo-only creation", result);

    SimResult baseline = simulate(false, 1, 7, 0, false, true);
    // Fewer rivalries active than baseline (promos still create pairs but matches no longer do)
    assertThat(result.activeCount()).isLessThan(baseline.activeCount());
    // Some rivalries should accumulate enough heat to be resolution-eligible
    assertThat(result.hotRivalries()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Fix 3: Max rivalry duration of 90 days auto-closes stale feuds")
  void fix3_maxDuration() {
    SimResult result =
        simulate(
            false, 1, 7, 90, // auto-close after 90 days
            false, true);

    printResult("FIX 3: 90-day max duration", result);

    SimResult baseline = simulate(false, 1, 7, 0, false, true);
    assertThat(result.activeCount()).isLessThan(baseline.activeCount());
    assertThat(result.totalResolved()).isGreaterThan(baseline.totalResolved());
  }

  @Test
  @DisplayName("Fix 4: Combined — decay + promo-only creation + 90-day expiry (recommended)")
  void fix4_combined_recommended() {
    SimResult result =
        simulate(
            true, // decay
            2, // 2 heat per interval
            7, 90, // 90-day max duration
            true, // promo-only creation
            true);

    printResult("FIX 4: COMBINED (recommended)", result);

    // The combined approach should keep active rivalries under control
    assertThat(result.activeCount())
        .as("Combined fixes should keep active rivalries manageable")
        .isLessThan(25);
    assertThat(result.totalResolved())
        .as("More rivalries should resolve or expire with combined fixes")
        .isGreaterThan(0);
    // With promo-only creation + decay, individual rivalries accumulate concentrated heat
    // Total resolved+decayed should exceed creations minus survivors
    assertThat(result.totalResolved() + result.totalDecayed())
        .as("Most created rivalries should be cleaned up")
        .isGreaterThan(result.totalCreated() / 2);
  }

  @Test
  @DisplayName("Summary: print comparison table of all scenarios")
  void summary_comparisonTable() {
    record Scenario(
        String label,
        boolean decay,
        int decayAmt,
        int decayDays,
        int maxDays,
        boolean requireExisting,
        boolean promoCreates) {}

    List<Scenario> scenarios =
        List.of(
            new Scenario("Baseline (current)", false, 1, 7, 0, false, true),
            new Scenario("Decay only (2/7d)", true, 2, 7, 0, false, true),
            new Scenario("Decay only (5/7d)", true, 5, 7, 0, false, true),
            new Scenario("Promo-only creation", false, 1, 7, 0, true, true),
            new Scenario("Max duration 90d", false, 1, 7, 90, false, true),
            new Scenario("Max duration 60d", false, 1, 7, 60, false, true),
            new Scenario("Decay + 90d expiry", true, 2, 7, 90, false, true),
            new Scenario("Promo + 90d expiry", false, 1, 7, 90, true, true),
            new Scenario("COMBINED (recommended)", true, 2, 7, 90, true, true));

    System.out.println("\n" + "=".repeat(110));
    System.out.printf(
        "%-35s %8s %8s %8s %8s %8s %8s%n",
        "Scenario", "Active", "Created", "Resolved", "Decayed", "AvgHeat", "Hot(20+)");
    System.out.println("=".repeat(110));

    SimResult baseline = null;
    for (Scenario s : scenarios) {
      SimResult r =
          simulate(
              s.decay(),
              s.decayAmt(),
              s.decayDays(),
              s.maxDays(),
              s.requireExisting(),
              s.promoCreates());
      if (baseline == null) baseline = r;
      System.out.printf(
          "%-35s %8d %8d %8d %8d %8.1f %8d%n",
          s.label(),
          r.activeCount(),
          r.totalCreated(),
          r.totalResolved(),
          r.totalDecayed(),
          r.avgHeat(),
          r.hotRivalries());
    }
    System.out.println("=".repeat(110));
    System.out.printf(
        "Roster: %d wrestlers | Shows: %d weekly + %d PLEs | Segments/show: %d | Wrestlers/segment:"
            + " %d%n",
        ROSTER_SIZE,
        WEEKS - WEEKS / PLE_EVERY_N_WEEKS,
        WEEKS / PLE_EVERY_N_WEEKS,
        SEGMENTS_PER_SHOW,
        WRESTLERS_PER_SEGMENT);

    // The combined fix must beat baseline on all key metrics
    SimResult combined = simulate(true, 2, 7, 90, true, true);
    assertThat(combined.activeCount()).isLessThan(baseline.activeCount());
    assertThat(combined.totalResolved()).isGreaterThanOrEqualTo(baseline.totalResolved());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private List<String> pickRandom(final List<String> source, final int count) {
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
