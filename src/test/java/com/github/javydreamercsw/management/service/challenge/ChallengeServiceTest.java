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

  @Test
  void customChallengesHaveNullWeekNumber() {
    service
        .getCustomChallenges()
        .forEach(
            c ->
                assertFalse(
                    c.getWeekNumber() != null,
                    "Custom challenge should have null weekNumber: " + c.getId()));
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
}
