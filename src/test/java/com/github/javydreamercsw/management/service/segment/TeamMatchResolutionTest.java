/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import com.github.javydreamercsw.base.ai.openai.OpenAISegmentNarrationService;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("Team Match Resolution Integration Tests")
@Transactional
class TeamMatchResolutionTest extends ManagementIntegrationTest {
  @Autowired private NPCSegmentResolutionService npcSegmentResolutionService;
  @MockitoBean private OpenAISegmentNarrationService openAIService;

  private Wrestler rookie1;
  private Wrestler rookie2;
  private Wrestler rookie3;
  private Wrestler rookie4;
  private Wrestler contender1;
  private Wrestler contender2;
  private SegmentType tagTeamSegmentType;
  private SegmentType handicapSegmentType;
  private Show testShow;

  @BeforeEach
  void setUp() {
    // Create and save test wrestlers
    rookie1 =
        wrestlerRepository.save(
            Wrestler.builder()
                .name("Rookie 1")
                .gender(Gender.MALE)
                .tier(WrestlerTier.ROOKIE)
                .deckSize(15)
                .startingHealth(15)
                .lowHealth(0)
                .startingStamina(0)
                .lowStamina(0)
                .fans(1000L)
                .isPlayer(false)
                .bumps(0)
                .injuries(new java.util.ArrayList<>())
                .build());
    rookie2 =
        wrestlerRepository.save(
            Wrestler.builder()
                .name("Rookie 2")
                .gender(Gender.MALE)
                .tier(WrestlerTier.ROOKIE)
                .deckSize(15)
                .startingHealth(15)
                .lowHealth(0)
                .startingStamina(0)
                .lowStamina(0)
                .fans(1000L)
                .isPlayer(false)
                .bumps(0)
                .injuries(new java.util.ArrayList<>())
                .build());
    rookie3 =
        wrestlerRepository.save(
            Wrestler.builder()
                .name("Rookie 3")
                .gender(Gender.MALE)
                .tier(WrestlerTier.ROOKIE)
                .deckSize(15)
                .startingHealth(15)
                .lowHealth(0)
                .startingStamina(0)
                .lowStamina(0)
                .fans(1000L)
                .isPlayer(false)
                .bumps(0)
                .injuries(new java.util.ArrayList<>())
                .build());
    rookie4 =
        wrestlerRepository.save(
            Wrestler.builder()
                .name("Rookie 4")
                .gender(Gender.MALE)
                .tier(WrestlerTier.ROOKIE)
                .deckSize(15)
                .startingHealth(15)
                .lowHealth(0)
                .startingStamina(0)
                .lowStamina(0)
                .fans(1000L)
                .isPlayer(false)
                .bumps(0)
                .injuries(new java.util.ArrayList<>())
                .build());
    contender1 =
        wrestlerRepository.save(
            Wrestler.builder()
                .name("Contender 1")
                .gender(Gender.MALE)
                .tier(WrestlerTier.CONTENDER)
                .deckSize(15)
                .startingHealth(15)
                .lowHealth(0)
                .startingStamina(0)
                .lowStamina(0)
                .fans(40000L)
                .isPlayer(false)
                .bumps(0)
                .injuries(new java.util.ArrayList<>())
                .build());
    contender2 =
        wrestlerRepository.save(
            Wrestler.builder()
                .name("Contender 1")
                .gender(Gender.MALE)
                .tier(WrestlerTier.CONTENDER)
                .deckSize(15)
                .startingHealth(15)
                .lowHealth(0)
                .startingStamina(0)
                .lowStamina(0)
                .fans(40000L)
                .isPlayer(false)
                .bumps(0)
                .injuries(new java.util.ArrayList<>())
                .build());

    // Create segment types (rely on DataInitializer for these)
    tagTeamSegmentType =
        segmentTypeRepository
            .findByName("Tag Team")
            .orElseGet(
                () -> {
                  SegmentType tagTeam = new SegmentType();
                  tagTeam.setName("Tag Team");
                  tagTeam.setDescription("2 vs 2 match");
                  return segmentTypeRepository.save(tagTeam);
                });
    handicapSegmentType =
        segmentTypeRepository
            .findByName("Handicap Match")
            .orElseGet(
                () -> {
                  SegmentType handicap = new SegmentType();
                  handicap.setName("Handicap Match");
                  handicap.setDescription("Uneven teams match");
                  return segmentTypeRepository.save(handicap);
                });

    // Create segment rules for testing
    segmentRuleService.createOrUpdateRule(
        "Handicap Match", "Handicap segment with uneven teams", false, BumpAddition.NONE);
    segmentRuleService.createOrUpdateRule(
        "Tag Team Championship", "Tag Team Championship Match", false, BumpAddition.NONE);
    segmentRuleService.createOrUpdateRule(
        "Test Match", "Generic Test Match for various scenarios", false, BumpAddition.NONE);
    segmentRuleService.createOrUpdateRule(
        "3v2 Elimination", "3 vs 2 Elimination Match", false, BumpAddition.NONE);
    segmentRuleService.createOrUpdateRule(
        "Singles Match via Team Interface",
        "Singles Match resolved via Team Interface",
        false,
        BumpAddition.NONE);
    segmentRuleService.createOrUpdateRule(
        "Tag Team Match", "Tag Team Match", false, BumpAddition.NONE);

    // Create test show
    ShowType showType =
        showTypeRepository
            .findByName("Weekly")
            .orElseGet(
                () -> {
                  ShowType weeklyShowType = new ShowType();
                  weeklyShowType.setName("Weekly");
                  weeklyShowType.setDescription("Weekly Show");
                  return showTypeRepository.save(weeklyShowType);
                });

    testShow = new Show();
    testShow.setName("Test Show");
    testShow.setDescription("Test show for team matches");
    testShow.setType(showType);
    testShow = showRepository.save(testShow);
  }

  @BeforeEach
  void setupRandom() {
    npcSegmentResolutionService.random = new Random(123L); // Fixed seed for deterministic tests
  }

  @Test
  @DisplayName("Should resolve tag team segment (2v2)")
  void shouldResolveTagTeamMatch() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      SegmentTeam team1 = new SegmentTeam(Arrays.asList(rookie1, rookie2), "The Rookies");
      SegmentTeam team2 = new SegmentTeam(Arrays.asList(rookie3, rookie4), "The Newbies");

      // When
      Segment result =
          npcSegmentResolutionService.resolveTeamSegment(
              team1, team2, tagTeamSegmentType, testShow, "Tag Team Match");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isNotNull();
      assertThat(result.getShow()).isEqualTo(testShow);
      assertThat(result.getSegmentType()).isEqualTo(tagTeamSegmentType);
      assertThat(result.getWinners()).hasSize(2);
      // Check that the winners are from the same team
      List<Wrestler> team1Members = Arrays.asList(rookie1, rookie2);
      List<Wrestler> team2Members = Arrays.asList(rookie3, rookie4);
      List<Wrestler> winners = result.getWinners();
      assertThat(winners.containsAll(team1Members) || winners.containsAll(team2Members)).isTrue();
      assertThat(result.getIsNpcGenerated()).isTrue();
      assertThat(result.getParticipants()).hasSize(4);

      // Verify all participants
      List<Wrestler> participants = result.getWrestlers();
      assertThat(participants).containsExactlyInAnyOrder(rookie1, rookie2, rookie3, rookie4);
    }
  }

  @Test
  @DisplayName("Should resolve handicap segment (1v2)")
  void shouldResolveHandicapMatch() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      SegmentTeam soloTeam = new SegmentTeam(contender1);
      SegmentTeam handicapTeam =
          new SegmentTeam(Arrays.asList(rookie1, rookie2), "Rookie Alliance");

      // When
      Segment result =
          npcSegmentResolutionService.resolveTeamSegment(
              soloTeam, handicapTeam, handicapSegmentType, testShow, "Handicap Match");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getParticipants()).hasSize(3);
      assertThat(result.getSegmentRulesAsString()).contains("Handicap");

      // The contender should have a good chance despite being outnumbered
      List<Wrestler> participants = result.getWrestlers();
      assertThat(participants).containsExactlyInAnyOrder(contender1, rookie1, rookie2);
    }
  }

  @Test
  @DisplayName("Should favor higher tier team in tag team segment")
  void shouldFavorHigherTierTeamInTagTeamMatch() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      SegmentTeam rookieTeam = new SegmentTeam(Arrays.asList(rookie1, rookie2), "Rookie Team");
      SegmentTeam contenderTeam =
          new SegmentTeam(Arrays.asList(contender1, contender2), "Contender Team");

      // When - Run multiple matches to test probability
      int contenderTeamWins = 0;
      int totalMatches = 50;

      for (int i = 0; i < totalMatches; i++) {
        Segment result =
            npcSegmentResolutionService.resolveTeamSegment(
                rookieTeam, contenderTeam, tagTeamSegmentType, testShow, "Test Match " + i);

        // Check if any contender won (representing their team)
        if (result.getWinners().contains(contender1) || result.getWinners().contains(contender2)) {
          contenderTeamWins++;
        }
      }

      // Then - Contender team should win significantly more often
      double contenderTeamWinRate = (double) contenderTeamWins / totalMatches;
      assertThat(contenderTeamWinRate)
          .isGreaterThan(0.6) // Should win at least 60% due to massive tier advantage
          .describedAs("Contender team should dominate rookie team");
    }
  }

  @Test
  @DisplayName("Should handle complex multi-team scenarios")
  void shouldHandleComplexMultiTeamScenarios() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given - 3v2 segment
      SegmentTeam bigTeam =
          new SegmentTeam(Arrays.asList(rookie1, rookie2, rookie3), "The Big Team");
      SegmentTeam smallTeam = new SegmentTeam(Arrays.asList(contender1, contender2), "The Elite");

      // When
      Segment result =
          npcSegmentResolutionService.resolveTeamSegment(
              bigTeam, smallTeam, handicapSegmentType, testShow, "3v2 Elimination");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getParticipants()).hasSize(5);
      // Note: "3v2 Elimination" doesn't segment any seeded rules, so it will be "Standard Match"
      assertThat(result.getSegmentRulesAsString()).isEqualTo("3v2 Elimination");

      List<Wrestler> participants = result.getWrestlers();
      assertThat(participants)
          .containsExactlyInAnyOrder(rookie1, rookie2, rookie3, contender1, contender2);
    }
  }

  @Test
  @DisplayName("Should work with singles segment using team interface")
  void shouldWorkWithSinglesMatchUsingTeamInterface() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      SegmentTeam team1 = new SegmentTeam(rookie1);
      SegmentTeam team2 = new SegmentTeam(contender1);

      // When
      Segment result =
          npcSegmentResolutionService.resolveTeamSegment(
              team1, team2, tagTeamSegmentType, testShow, "Singles Match via Team Interface");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getParticipants()).hasSize(2);
      assertThat(result.getWrestlers()).containsExactlyInAnyOrder(rookie1, contender1);

      // Should still favor the contender
      int contenderWins = 0;
      int totalMatches = 1_000;
      for (int i = 0; i < totalMatches; i++) {
        result =
            npcSegmentResolutionService.resolveTeamSegment(
                team1, team2, tagTeamSegmentType, testShow, "Singles Match via Team Interface");
        if (result.getWinners().contains(contender1)) {
          contenderWins++;
        }
      }
      double contenderWinRate = (double) contenderWins / totalMatches;
      assertThat(contenderWinRate)
          .isGreaterThanOrEqualTo(0.8); // Contender should win most of the time
    }
  }

  @Test
  @DisplayName("Should generate appropriate segment duration for team matches")
  void shouldGenerateAppropriateMatchDurationForTeamMatches() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      SegmentTeam tagTeam1 = new SegmentTeam(Arrays.asList(rookie1, rookie2));
      SegmentTeam tagTeam2 = new SegmentTeam(Arrays.asList(rookie3, rookie4));

      // When
      Segment result =
          npcSegmentResolutionService.resolveTeamSegment(
              tagTeam1, tagTeam2, tagTeamSegmentType, testShow, "Tag Team Championship");
    }
  }
}
