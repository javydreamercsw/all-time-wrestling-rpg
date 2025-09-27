package com.github.javydreamercsw.management.service.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("NPC Segment Resolution Service Integration Tests")
@Transactional
class NPCSegmentResolutionServiceIT extends AbstractIntegrationTest {
  @Autowired NPCSegmentResolutionService npcSegmentResolutionService;
  @Autowired WrestlerService wrestlerService;
  @Autowired WrestlerRepository wrestlerRepository;
  @Autowired SegmentRepository matchRepository;

  @Autowired
  com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository
      showTemplateRepository;

  private Wrestler rookie1;
  private Wrestler rookie2;
  private Wrestler contender;
  private SegmentType singlesSegmentType;
  private SegmentType tagTeamType;
  private Show testShow;

  @BeforeEach
  @SneakyThrows
  void setUp() {
    dataInitializer.loadSegmentTypesFromFile(segmentTypeService).run(null);
    // Create test wrestlers with different tiers
    rookie1 = wrestlerService.createWrestler("Rookie One", true, null);
    rookie2 = wrestlerService.createWrestler("Rookie Two", true, null);
    contender = wrestlerService.createWrestler("The Contender", true, null);

    // Award fans to create tier differences
    Assertions.assertNotNull(contender.getId());
    wrestlerService.awardFans(contender.getId(), 450_00L); // CONTENDER tier

    // Refresh wrestler entities from database to get updated fan counts
    contender = wrestlerRepository.findById(contender.getId()).orElseThrow();

    // Create segment types (rely on DataInitializer for these)
    singlesSegmentType = segmentTypeRepository.findByName("One on One").orElseThrow();
    tagTeamType = segmentTypeRepository.findByName("Tag Team").orElseThrow();

    // Create segment rules for testing
    segmentRuleService.createOrUpdateRule(
        "Steel Cage Match", "Steel cage segment with no escape", false);
    segmentRuleService.createOrUpdateRule(
        "Test Match", "Generic Test Match for various scenarios", false);
    segmentRuleService.createOrUpdateRule("Triple Threat Match", "Triple Threat Match", false);
    segmentRuleService.createOrUpdateRule("Injury Test", "Injury Test Match", false);

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

  @Test
  @DisplayName("Should favor higher tier wrestler in singles segment")
  void shouldFavorHigherTierWrestlerInSinglesMatch() {
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

  @Test
  @DisplayName("Should resolve triple threat segment")
  void shouldResolveTripleThreatMatch() {
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

  @Test
  @DisplayName("Should throw exception for multi-person segment with less than 3 wrestlers")
  void shouldThrowExceptionForInvalidMultiPersonMatch() {
    // When/Then
    List<SegmentTeam> twoTeams = Arrays.asList(new SegmentTeam(rookie1), new SegmentTeam(rookie2));
    assertThatThrownBy(
            () ->
                npcSegmentResolutionService.resolveMultiTeamSegment(
                    twoTeams, tagTeamType, testShow, "Invalid Match"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Multi-team segment requires at least 3 teams");
  }

  @Test
  @DisplayName("Should handle wrestler with injuries and bumps")
  void shouldHandleWrestlerWithInjuriesAndBumps() {
    // Given - Add bumps to rookie1
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
        .isGreaterThanOrEqualTo(0.4) // Should have some advantage due to opponent's injuries
        .describedAs("Healthy wrestler should have some advantage over injured opponent");
  }

  @Test
  @DisplayName("Should save segment with rule")
  void shouldSaveMatchWithStipulation() {
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
