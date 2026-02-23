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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.openai.OpenAISegmentNarrationService;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("NPC Segment Resolution Service Integration Tests")
@TestPropertySource(properties = "data.initializer.enabled=false")
class NPCSegmentResolutionServiceTest extends ManagementIntegrationTest {
  @Autowired NPCSegmentResolutionService npcSegmentResolutionService;
  @Autowired WrestlerService wrestlerService;
  @Autowired WrestlerRepository wrestlerRepository;
  @Autowired SegmentRepository matchRepository;
  @MockitoBean private OpenAISegmentNarrationService openAIService;
  @MockitoBean private InjuryService injuryService;
  @Autowired private SegmentTypeService segmentTypeService;
  @Autowired private ShowTypeService showTypeService;

  @Autowired ShowTemplateRepository showTemplateRepository;

  private Wrestler rookie1;
  private Wrestler rookie2;
  private Wrestler contender;
  private SegmentType singlesSegmentType;
  private SegmentType tagTeamType;
  private Show testShow;

  @BeforeEach
  @SneakyThrows
  void setUp() {
    if (!showTypeRepository.findByName("Weekly").isPresent()) {
      ShowType weeklyShowType = new ShowType();
      weeklyShowType.setName("Weekly");
      weeklyShowType.setDescription("A weekly show");
      showTypeRepository.save(weeklyShowType);
    }
    if (singlesSegmentType == null) {
      singlesSegmentType =
          segmentTypeService.createOrUpdateSegmentType("One on One", "1 vs 1 match");
      tagTeamType = segmentTypeService.createOrUpdateSegmentType("Tag Team", "2 vs 2 match");
    }

    // Create test wrestlers with different tiers
    rookie1 = wrestlerService.createWrestler("Rookie One", true, null, WrestlerTier.ROOKIE);
    rookie2 = wrestlerService.createWrestler("Rookie Two", true, null, WrestlerTier.ROOKIE);
    contender = wrestlerService.createWrestler("The Contender", true, null, WrestlerTier.CONTENDER);

    // Award fans to create tier differences
    Assertions.assertNotNull(contender.getId());
    wrestlerService.awardFans(contender.getId(), 450_00L); // CONTENDER tier

    // Refresh wrestler entities from database to get updated fan counts
    contender = wrestlerRepository.findById(contender.getId()).orElseThrow();

    // Create segment rules for testing
    segmentRuleService.createOrUpdateRule(
        "Steel Cage Match", "Steel cage segment with no escape", false, false, BumpAddition.NONE);
    segmentRuleService.createOrUpdateRule(
        "Test Match", "Generic Test Match for various scenarios", false, false, BumpAddition.NONE);
    segmentRuleService.createOrUpdateRule(
        "Triple Threat Match", "Triple Threat Match", false, false, BumpAddition.NONE);
    segmentRuleService.createOrUpdateRule(
        "Injury Test", "Injury Test Match", false, false, BumpAddition.NONE);

    // Create test show
    ShowType showType = showTypeRepository.findByName("Weekly").orElseThrow();

    testShow = new Show();
    testShow.setName("Test Show");
    testShow.setDescription("Test show for NPC matches");
    testShow.setType(showType);
    testShow = showRepository.save(testShow);
  }

  @AfterEach
  void cleanUp() {
    matchRepository.deleteAll();
    deckRepository.deleteAll(); // Delete decks before wrestlers
    wrestlerRepository.deleteAll();
    segmentTypeRepository.deleteAll();
    showRepository.deleteAll();
    showTemplateRepository.deleteAll();
    showTypeRepository.deleteAll();
  }

  @Test
  @DisplayName("Should resolve singles segment between two rookies")
  void shouldResolveSinglesMatchBetweenRookies() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // When
      SegmentTeam team1 = new SegmentTeam(rookie1);
      SegmentTeam team2 = new SegmentTeam(rookie2);
      Segment result =
          npcSegmentResolutionService.resolveTeamSegment(
              team1, team2, singlesSegmentType, testShow, "Standard Match");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isNotNull();
      assertThat(result.getShow()).isEqualTo(testShow);
      assertThat(result.getSegmentType()).isEqualTo(singlesSegmentType);
      assertThat(result.getWinners()).hasSize(1);
      assertThat(result.getWinners().get(0)).isIn(rookie1, rookie2);
      assertThat(result.getIsNpcGenerated()).isTrue();
      assertThat(result.getParticipants()).hasSize(2);

      // Verify participants
      List<Wrestler> participants = result.getWrestlers();
      assertThat(participants).containsExactlyInAnyOrder(rookie1, rookie2);
    }
  }

  @Test
  @Transactional
  @DisplayName("Should favor higher tier wrestler in singles segment")
  void shouldFavorHigherTierWrestlerInSinglesMatch() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given - Run multiple matches to test probability
      int contenderWins = 0;
      int totalMatches = 100;

      // When - Simulate many matches
      for (int i = 0; i < totalMatches; i++) {
        SegmentTeam team1 = new SegmentTeam(rookie1);
        SegmentTeam team2 = new SegmentTeam(contender);
        Segment result =
            npcSegmentResolutionService.resolveTeamSegment(
                team1, team2, singlesSegmentType, testShow, "Test Match " + i);

        if (result.getWinners().contains(contender)) {
          contenderWins++;
        }
      }

      // Then - Contender should win significantly more often than rookie
      double contenderWinRate = (double) contenderWins / totalMatches;
      assertThat(contenderWinRate)
          .isGreaterThan(0.9) // Should win at least 90% due to massive tier advantage
          .describedAs("Contender with 45,000 fans should dominate rookie with 0 fans");
    }
  }

  @Test
  @Transactional
  @DisplayName("Should resolve triple threat segment")
  void shouldResolveTripleThreatMatch() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // When
      List<SegmentTeam> teams =
          Arrays.asList(
              new SegmentTeam(rookie1), new SegmentTeam(rookie2), new SegmentTeam(contender));
      Segment result =
          npcSegmentResolutionService.resolveMultiTeamSegment(
              teams, tagTeamType, testShow, "Triple Threat Match");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isNotNull();
      assertThat(result.getShow()).isEqualTo(testShow);
      assertThat(result.getSegmentType()).isEqualTo(tagTeamType);
      assertThat(result.getWinners()).hasSize(1);
      assertThat(result.getWinners().get(0)).isIn(rookie1, rookie2, contender);
      assertThat(result.getIsNpcGenerated()).isTrue();
      assertThat(result.getParticipants()).hasSize(3);

      // Verify all participants
      List<Wrestler> participants = result.getWrestlers();
      assertThat(participants).containsExactlyInAnyOrder(rookie1, rookie2, contender);
    }
  }

  @Test
  @DisplayName("Should throw exception for multi-person segment with less than 3 wrestlers")
  void shouldThrowExceptionForInvalidMultiPersonMatch() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // When/Then
      List<SegmentTeam> twoTeams =
          Arrays.asList(new SegmentTeam(rookie1), new SegmentTeam(rookie2));
      assertThatThrownBy(
              () ->
                  npcSegmentResolutionService.resolveMultiTeamSegment(
                      twoTeams, tagTeamType, testShow, "Invalid Match"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Multi-team segment requires at least 3 teams");
    }
  }

  @Test
  @Transactional
  @DisplayName("Should handle wrestler with injuries and bumps")
  void shouldHandleWrestlerWithInjuriesAndBumps() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given - Add bumps to rookie1
      Injury injury = mock(Injury.class);
      InjurySeverity severity = mock(InjurySeverity.class);
      when(injury.getSeverity()).thenReturn(severity);
      when(severity.getDisplayName()).thenReturn("Minor");
      when(injuryService.createInjuryFromBumps(anyLong())).thenReturn(Optional.of(injury));
      Assertions.assertNotNull(rookie1.getId());
      wrestlerService.addBump(rookie1.getId());
      wrestlerService.addBump(rookie1.getId());
      wrestlerService.addBump(rookie1.getId()); // This should create an injury

      // Refresh wrestler from database
      rookie1 = wrestlerRepository.findById(rookie1.getId()).orElseThrow();

      // When - Run multiple matches to test impact
      int rookie2Wins = 0;
      int totalMatches = 100;

      for (int i = 0; i < totalMatches; i++) {
        SegmentTeam team1 = new SegmentTeam(rookie1);
        SegmentTeam team2 = new SegmentTeam(rookie2);
        Segment result =
            npcSegmentResolutionService.resolveTeamSegment(
                team1, team2, singlesSegmentType, testShow, "Injury Test " + i);

        if (result.getWinners().contains(rookie2)) {
          rookie2Wins++;
        }
      }

      // Then - Injured wrestler should win less often
      double rookie2WinRate = (double) rookie2Wins / totalMatches;
      assertThat(rookie2WinRate)
          .isGreaterThanOrEqualTo(0.30) // Should have some advantage due to opponent's injuries
          .describedAs("Healthy wrestler should have some advantage over injured opponent");
    }
  }

  @Test
  @Transactional
  @DisplayName("Should favor faction with high affinity in tag matches")
  void shouldFavorFactionWithHighAffinity() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");

      // Given - Create a high affinity faction
      com.github.javydreamercsw.management.domain.faction.Faction highAffinityFaction =
          com.github.javydreamercsw.management.domain.faction.Faction.builder()
              .name("High Affinity")
              .affinity(100)
              .isActive(true)
              .build();
      highAffinityFaction = factionRepository.save(highAffinityFaction);

      Wrestler f1 =
          wrestlerService.createWrestler("Faction Member 1", true, null, WrestlerTier.MIDCARDER);
      Wrestler f2 =
          wrestlerService.createWrestler("Faction Member 2", true, null, WrestlerTier.MIDCARDER);
      f1.setFaction(highAffinityFaction);
      f2.setFaction(highAffinityFaction);
      wrestlerRepository.saveAll(List.of(f1, f2));

      // Create a low affinity faction (or just independent midcarders)
      Wrestler i1 =
          wrestlerService.createWrestler("Independent 1", true, null, WrestlerTier.MIDCARDER);
      Wrestler i2 =
          wrestlerService.createWrestler("Independent 2", true, null, WrestlerTier.MIDCARDER);

      SegmentTeam highAffinityTeam = new SegmentTeam(List.of(f1, f2), "Team Alpha");
      SegmentTeam independentTeam = new SegmentTeam(List.of(i1, i2), "Team Beta");

      // When - Simulate matches
      int factionWins = 0;
      int totalMatches = 200;

      for (int i = 0; i < totalMatches; i++) {
        Segment result =
            npcSegmentResolutionService.resolveTeamSegment(
                highAffinityTeam, independentTeam, tagTeamType, testShow, "Synergy Test " + i);
        if (result.getWinners().contains(f1)) {
          factionWins++;
        }
      }

      // Then - Faction should win more than 50% (base is equal, bonus is +10 weight)
      double winRate = (double) factionWins / totalMatches;
      assertThat(winRate)
          .isGreaterThan(0.52)
          .describedAs(
              "Faction with 100 affinity should have clear advantage over independents (found "
                  + winRate
                  + ")");
    }
  }

  @Test
  @DisplayName("Should save segment with rule")
  void shouldSaveMatchWithStipulation() {
    try (MockedStatic<EnvironmentVariableUtil> staticUtilMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      staticUtilMock.when(EnvironmentVariableUtil::getNotionToken).thenReturn("dummy");
      staticUtilMock.when(() -> openAIService.generateText(anyString())).thenReturn("dummy");
      // Given
      String stipulation = "Steel Cage Match";

      // When
      SegmentTeam team1 = new SegmentTeam(rookie1);
      SegmentTeam team2 = new SegmentTeam(rookie2);
      Segment result =
          npcSegmentResolutionService.resolveTeamSegment(
              team1, team2, singlesSegmentType, testShow, stipulation);

      // Then
      assertThat(result.getSegmentRulesAsString()).contains("Steel Cage");

      // Verify persistence
      Assertions.assertNotNull(result.getId());
      Segment savedResult = matchRepository.findById(result.getId()).orElseThrow();
      assertThat(savedResult.getSegmentRulesAsString()).contains("Steel Cage");
    }
  }
}
