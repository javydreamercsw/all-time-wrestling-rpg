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
package com.github.javydreamercsw.management.service.challenge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.Difficulty;
import com.github.javydreamercsw.management.dto.challenge.ChallengeDTO;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class ChallengeServiceTest {

  private ChallengeService service;

  @BeforeEach
  void setUp() {
    service = new ChallengeService(new ObjectMapper(), new PathMatchingResourcePatternResolver());
    service.init();
  }

  @Test
  void loadsAllShippedChallenges() {
    assertEquals(countClasspathChallenges(c -> true), service.getAllChallenges().size());
  }

  @Test
  void allShippedChallengesAreActive() {
    assertTrue(service.getActiveChallenges().stream().allMatch(ChallengeDTO::isActive));
  }

  @Test
  void officialChallengesAreNonCustom() {
    List<ChallengeDTO> official = service.getOfficialChallenges();
    assertEquals(
        countClasspathChallenges(c -> c.isActive() && !"CUSTOM".equals(c.getExpansionCode())),
        official.size());
    official.forEach(c -> assertFalse("CUSTOM".equals(c.getExpansionCode())));
  }

  @Test
  void customChallengesHaveCustomExpansionCode() {
    List<ChallengeDTO> custom = service.getCustomChallenges();
    assertEquals(
        countClasspathChallenges(c -> c.isActive() && "CUSTOM".equals(c.getExpansionCode())),
        custom.size());
    custom.forEach(c -> assertEquals("CUSTOM", c.getExpansionCode()));
  }

  private int countClasspathChallenges(final Predicate<ChallengeDTO> filter) {
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources("classpath:challenges/**/*.json");
      ObjectMapper mapper = new ObjectMapper();
      int count = 0;
      for (Resource r : resources) {
        String name = r.getFilename();
        if (name != null && name.contains("achievements")) {
          continue;
        }
        try (InputStream is = r.getInputStream()) {
          List<ChallengeDTO> batch = mapper.readValue(is, new TypeReference<>() {});
          count += (int) batch.stream().filter(filter).count();
        }
      }
      return count;
    } catch (IOException e) {
      throw new RuntimeException("Failed to count classpath challenges", e);
    }
  }

  @Test
  void weeklyNumbersArePresent() {
    List<Integer> weeks =
        service.getAllChallenges().stream().map(ChallengeDTO::getWeekNumber).toList();
    assertTrue(weeks.contains(1));
    assertTrue(weeks.contains(2));
    assertTrue(weeks.contains(3));
  }

  /**
   * Community challenges carry no scheduled week. The Deadly Combat arc is the one exception — it
   * ships as custom-classified Season 1 weekly content, so exactly those entries carry week numbers
   * 7 through 11.
   */
  @Test
  void customWeekNumbersAppearOnlyOnTheDeadlyCombatArc() {
    service
        .getCustomChallenges()
        .forEach(
            c ->
                assertEquals(
                    c.getId().contains("deadly_combat"),
                    c.getWeekNumber() != null,
                    "Custom challenges must be weekless unless they belong to the Deadly Combat"
                        + " arc: "
                        + c.getId()));
  }

  @Test
  void customChallengeIdsAreReachable() {
    assertTrue(service.getChallenge("custom_s1_01_tap_out").isPresent());
    assertTrue(service.getChallenge("custom_s1_08_war_games").isPresent());
  }

  @Test
  void challengeTwoRequiresTwoExpansions() {
    ChallengeDTO c2 = service.getChallenge("week_03_all_time_king").orElseThrow();
    assertTrue(c2.getRequiredExpansions().contains("EXTREME"));
    assertTrue(c2.getRequiredExpansions().contains("MATT_CARDONA"));
  }

  @Test
  void challengeOneHasConditions() {
    ChallengeDTO c1 = service.getChallenge("week_01_locker_room_attack").orElseThrow();
    assertFalse(c1.getConditions().isEmpty());
  }

  @Test
  void challengeThreeHasModifiersAndNotes() {
    ChallengeDTO c3 = service.getChallenge("week_02_broken_neck").orElseThrow();
    assertFalse(c3.getModifiers().isEmpty());
    assertTrue(c3.getNotes() != null && !c3.getNotes().isBlank());
  }

  // ── Deadly Combat: OMZ's covert-tournament arc (weeks 7-11) ────────────────

  /**
   * OMZ's biography names the covert Siberian prison tournament "Deadly Combat" that he won five
   * consecutive years. The arc ships one challenge per tournament year as custom-classified Season
   * 1 weekly content, so all five must be present with ascending week numbers 7 through 11.
   */
  @Test
  void deadlyCombatArcShipsAllFiveTournamentYears() {
    for (int week = 7; week < 11 + 1; week++) {
      final int weekNum = week;
      final int year = week - 6;
      ChallengeDTO challenge =
          service
              .getChallenge("custom_s1_%02d_deadly_combat_year_%d".formatted(week + 2, year))
              .orElseThrow(
                  () ->
                      new AssertionError(
                          "Deadly Combat year " + year + " (week " + weekNum + ") is missing"));
      assertEquals("Season 1", challenge.getSeason());
      assertEquals(week, challenge.getWeekNumber());
      assertEquals("OMZ", challenge.getRequiredWrestlerNames().getFirst());
    }
  }

  /**
   * The arc escalates through the tournament years: MEDIUM Year One, then HARD from Year Two on —
   * the finale card is HARD with the LEGENDARY tier expressed in its rules text (the Warden's
   * cruelty modifiers).
   */
  @Test
  void deadlyCombatDifficultyEscalatesAcrossTheArc() {
    List<Difficulty> expected =
        List.of(
            Difficulty.MEDIUM, Difficulty.HARD, Difficulty.HARD, Difficulty.HARD, Difficulty.HARD);
    List<Difficulty> actual =
        IntStream.rangeClosed(7, 11)
            .mapToObj(
                week ->
                    service
                        .getChallenge(
                            "custom_s1_%02d_deadly_combat_year_%d".formatted(week + 2, week - 6))
                        .orElseThrow()
                        .getDifficulty())
            .toList();
    assertEquals(expected, actual);
  }

  /**
   * Every match type must resolve to a shipped SegmentRule name — the detail dialog links to the
   * in-app match-info page by exact name match. The arc leans on Extreme-style rules per the issue:
   * Barbwire Exploding Deathmatch, Last Man Standing, Cage, I Quit Match, and a Gauntlet finale.
   */
  @Test
  void deadlyCombatMatchTypesResolveToShippedSegmentRules() throws Exception {
    Set<String> shippedRuleNames = new HashSet<>();
    try (InputStream is = getClass().getResourceAsStream("/segment_rules.json")) {
      new ObjectMapper()
          .readValue(is, new TypeReference<List<Map<String, Object>>>() {}).stream()
              .map(r -> (String) r.get("name"))
              .forEach(shippedRuleNames::add);
    }
    IntStream.rangeClosed(7, 11)
        .mapToObj(
            week ->
                service
                    .getChallenge(
                        "custom_s1_%02d_deadly_combat_year_%d".formatted(week + 2, week - 6))
                    .orElseThrow())
        .forEach(
            challenge -> {
              assertTrue(
                  shippedRuleNames.contains(challenge.getMatchType()),
                  challenge.getId()
                      + " matchType '"
                      + challenge.getMatchType()
                      + "' does not match any shipped SegmentRule name");
            });
  }

  /** The gauntlet finale draws all four opponents from the Extreme roster. */
  @Test
  void deadlyCombatFinaleRequiresExtremeExpansion() {
    ChallengeDTO finale = service.getChallenge("custom_s1_13_deadly_combat_year_5").orElseThrow();
    assertEquals("Gauntlet", finale.getMatchType());
    // Extreme-only arc: every opponent comes from the Extreme roster, so RUMBLE is NOT required.
    assertTrue(finale.getRequiredExpansions().contains("EXTREME"));
    assertTrue(
        !finale.getRequiredExpansions().contains("RUMBLE"),
        "RUMBLE is not part of the Extreme-only arc");
    assertTrue(finale.getRequiredWrestlerNames().size() >= 4, "OMZ plus gauntlet opponents");
    assertFalse(finale.getModifiers().isEmpty(), "gauntlet needs between-match modifiers");
  }

  @Test
  void reloadRetainsLoadedChallenges() {
    int before = service.getAllChallenges().size();
    service.reload();
    assertEquals(before, service.getAllChallenges().size());
  }

  @Test
  void resolveContentDirEndsWithChallenges() {
    assertEquals("challenges", ChallengeService.resolveContentDir().getFileName().toString());
  }

  @Test
  void resolveContentDirIsAbsolute() {
    assertTrue(ChallengeService.resolveContentDir().isAbsolute());
  }

  @Test
  void allChallengeAchievementKeysExistInShippedAchievementFiles() throws Exception {
    // Achievement keys may ship in achievements.json (the global catalog) OR in
    // any challenges/**/weekly_achievements.json — AchievementSync upserts both
    // at startup (single-source-of-truth change, ATW-i2ar). Assert each
    // challenge's key is defined in at least one of the shipped files.
    Set<String> knownKeys = new HashSet<>();
    try (InputStream is = getClass().getResourceAsStream("/achievements.json")) {
      new ObjectMapper()
          .readValue(is, new TypeReference<List<Map<String, Object>>>() {}).stream()
              .map(a -> (String) a.get("key"))
              .forEach(knownKeys::add);
    }
    // Walk the challenges directory the same way ChallengeService does.
    try (var files = Files.walk(Path.of("src", "main", "resources", "challenges"))) {
      for (Path p : files.filter(f -> f.toString().endsWith(".json")).toList()) {
        if (p.getFileName().toString().toLowerCase(Locale.ROOT).contains("achievements")) {
          try (InputStream is = new FileInputStream(p.toFile())) {
            new ObjectMapper()
                .readValue(is, new TypeReference<List<Map<String, Object>>>() {}).stream()
                    .map(a -> (String) a.get("key"))
                    .forEach(knownKeys::add);
          }
        }
      }
    }

    service.getAllChallenges().stream()
        .filter(c -> c.getAchievementKey() != null && !c.getAchievementKey().isBlank())
        .forEach(
            c ->
                assertTrue(
                    knownKeys.contains(c.getAchievementKey()),
                    "Challenge '"
                        + c.getId()
                        + "' has achievementKey '"
                        + c.getAchievementKey()
                        + "' not found in any shipped achievements file"));
  }

  /**
   * Every Season 2 challenge (custom_s2_*) references a key in the global catalog, and the season
   * carries its completion achievement — ChallengeCompletionService derives
   * CHALLENGE_SEASON_2_COMPLETE from the "Season 2" season label.
   */
  @Test
  void seasonTwoAchievementKeysShipInGlobalCatalog() throws Exception {
    Set<String> keys = new HashSet<>();
    try (InputStream is = getClass().getResourceAsStream("/achievements.json")) {
      new ObjectMapper()
          .readValue(is, new TypeReference<List<Map<String, Object>>>() {}).stream()
              .map(a -> (String) a.get("key"))
              .forEach(keys::add);
    }
    IntStream.rangeClosed(1, 22)
        .forEach(
            i ->
                assertTrue(
                    keys.contains("CHALLENGE_CUSTOM_S2_%02d".formatted(i)),
                    "missing CHALLENGE_CUSTOM_S2_%02d".formatted(i)));
    assertTrue(keys.contains("CHALLENGE_SEASON_2_COMPLETE"));
  }

  // ── Season 2: custom challenge season (slots 1-22) ─────────────────────────

  /**
   * The on-ramp opens the season with two EASY slots: the inverted survive-the-Beast objective
   * (ATW-yuem) and Cardona's King of the Indies opener (ATW-byhj part 1).
   */
  @Test
  void seasonTwoOnRampShipsSurvivalAndIndiesChallenges() {
    ChallengeDTO beast = service.getChallenge("custom_s2_01_beast_survive").orElseThrow();
    assertEquals("Season 2", beast.getSeason());
    assertEquals("CUSTOM", beast.getExpansionCode());
    assertEquals(Difficulty.EASY, beast.getDifficulty());
    assertEquals("Last Man Standing", beast.getMatchType());
    assertEquals("The Beast", beast.getRequiredWrestlerNames().getFirst());
    assertEquals("CHALLENGE_CUSTOM_S2_01", beast.getAchievementKey());

    ChallengeDTO cardona = service.getChallenge("custom_s2_02_belt_collector_indies").orElseThrow();
    assertEquals(Difficulty.EASY, cardona.getDifficulty());
    assertEquals("CHALLENGE_CUSTOM_S2_02", cardona.getAchievementKey());
  }

  /**
   * Rising action raises every slot to MEDIUM: Raven's Flock interference wall (ATW-wfux),
   * Pillman's random-modifier variance (ATW-q46r), and Slaughter's crowd-heat debut (ATW-xl1k act
   * 1).
   */
  @Test
  void seasonTwoRisingActionShipsFlockCannonAndHero() {
    ChallengeDTO raven = service.getChallenge("custom_s2_03_raven_flock").orElseThrow();
    assertEquals(Difficulty.MEDIUM, raven.getDifficulty());
    assertEquals("No DQ", raven.getMatchType());
    assertEquals("Raven", raven.getRequiredWrestlerNames().getFirst());
    assertEquals("CHALLENGE_CUSTOM_S2_03", raven.getAchievementKey());

    ChallengeDTO pillman = service.getChallenge("custom_s2_04_loose_cannon").orElseThrow();
    assertEquals(Difficulty.MEDIUM, pillman.getDifficulty());
    assertEquals("No DQ", pillman.getMatchType());
    assertEquals("Brian Pillman", pillman.getRequiredWrestlerNames().getFirst());
    assertEquals("CHALLENGE_CUSTOM_S2_04", pillman.getAchievementKey());

    ChallengeDTO slaughter = service.getChallenge("custom_s2_05_slaughter_hero").orElseThrow();
    assertEquals(Difficulty.MEDIUM, slaughter.getDifficulty());
    assertEquals("Submission", slaughter.getMatchType());
    assertEquals("Sgt. Slaughter", slaughter.getRequiredWrestlerNames().getFirst());
    assertEquals("CHALLENGE_CUSTOM_S2_05", slaughter.getAchievementKey());
  }

  /**
   * The grind opens at HARD: Cardona's deathmatch crown defense (ATW-byhj part 2), the tag-format
   * Bulldogs reunion (ATW-usk9 part 1), and Slaughter's traitor turn with the crowd against him
   * (ATW-xl1k act 2).
   */
  @Test
  void seasonTwoGrindOpensWithDeathmatchBulldogsAndTraitor() {
    ChallengeDTO cardona =
        service.getChallenge("custom_s2_06_belt_collector_deathmatch").orElseThrow();
    assertEquals(Difficulty.MEDIUM, cardona.getDifficulty());
    assertEquals("Barbwire Exploding Deathmatch", cardona.getMatchType());
    assertEquals("Matt Cardona", cardona.getRequiredWrestlerNames().getFirst());
    assertEquals("Sabu", cardona.getRequiredWrestlerNames().getLast());
    assertTrue(cardona.getRequiredExpansions().contains("EXTREME"));
    assertEquals("CHALLENGE_CUSTOM_S2_06", cardona.getAchievementKey());

    ChallengeDTO bulldogs = service.getChallenge("custom_s2_07_bulldogs_reunion").orElseThrow();
    assertEquals(Difficulty.MEDIUM, bulldogs.getDifficulty());
    assertEquals("Elimination", bulldogs.getMatchType());
    assertEquals("The British Bulldog", bulldogs.getRequiredWrestlerNames().getFirst());
    assertEquals("Dynamite Kid", bulldogs.getRequiredWrestlerNames().getLast());
    assertEquals("CHALLENGE_CUSTOM_S2_07", bulldogs.getAchievementKey());

    ChallengeDTO traitor = service.getChallenge("custom_s2_08_slaughter_traitor").orElseThrow();
    assertEquals(Difficulty.HARD, traitor.getDifficulty());
    assertEquals("Cage", traitor.getMatchType());
    assertEquals("Sgt. Slaughter", traitor.getRequiredWrestlerNames().getFirst());
    assertEquals("CHALLENGE_CUSTOM_S2_08", traitor.getAchievementKey());
  }
}
