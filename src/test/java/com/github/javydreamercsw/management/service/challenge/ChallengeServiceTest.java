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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.dto.challenge.ChallengeDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    assertEquals(11, service.getAllChallenges().size());
  }

  @Test
  void allShippedChallengesAreActive() {
    assertTrue(service.getActiveChallenges().stream().allMatch(ChallengeDTO::isActive));
  }

  @Test
  void officialChallengesAreNonCustom() {
    List<ChallengeDTO> official = service.getOfficialChallenges();
    assertEquals(3, official.size());
    official.forEach(c -> assertFalse("CUSTOM".equals(c.getExpansionCode())));
  }

  @Test
  void customChallengesHaveCustomExpansionCode() {
    List<ChallengeDTO> custom = service.getCustomChallenges();
    assertEquals(8, custom.size());
    custom.forEach(c -> assertEquals("CUSTOM", c.getExpansionCode()));
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
}
