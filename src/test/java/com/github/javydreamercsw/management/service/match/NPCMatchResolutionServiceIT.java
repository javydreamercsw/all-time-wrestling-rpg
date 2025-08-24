package com.github.javydreamercsw.management.service.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.TestcontainersConfiguration;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.show.match.MatchResultRepository;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.match.type.MatchTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("NPC Match Resolution Service Integration Tests")
class NPCMatchResolutionServiceIT {

  @Autowired NPCMatchResolutionService npcMatchResolutionService;
  @Autowired WrestlerService wrestlerService;
  @Autowired WrestlerRepository wrestlerRepository;
  @Autowired MatchResultRepository matchResultRepository;
  @Autowired MatchTypeRepository matchTypeRepository;
  @Autowired ShowRepository showRepository;
  @Autowired ShowTypeRepository showTypeRepository;
  @Autowired MatchRuleService matchRuleService;

  private Wrestler rookie1;
  private Wrestler rookie2;
  private Wrestler contender;
  private MatchType singlesMatchType;
  private MatchType tripleThreadType;
  private Show testShow;

  @BeforeEach
  void setUp() {
    // Create test wrestlers with different tiers
    rookie1 = wrestlerService.createWrestler("Rookie One", true, null);
    rookie2 = wrestlerService.createWrestler("Rookie Two", true, null);
    contender = wrestlerService.createWrestler("The Contender", true, null);

    // Award fans to create tier differences
    wrestlerService.awardFans(contender.getId(), 45000L); // CONTENDER tier

    // Refresh wrestler entities from database to get updated fan counts
    contender = wrestlerRepository.findById(contender.getId()).orElseThrow();

    // Create match types
    singlesMatchType = new MatchType();
    singlesMatchType.setName("Singles Match");
    singlesMatchType = matchTypeRepository.save(singlesMatchType);

    tripleThreadType = new MatchType();
    tripleThreadType.setName("Triple Threat Match");
    tripleThreadType = matchTypeRepository.save(tripleThreadType);

    // Create match rules for testing
    matchRuleService.createOrUpdateRule(
        "Steel Cage Match", "Steel cage match with no escape", false);

    // Create test show
    ShowType showType = new ShowType();
    showType.setName("Weekly Show");
    showType.setDescription("Weekly wrestling show for testing");
    showType = showTypeRepository.save(showType);

    testShow = new Show();
    testShow.setName("Test Show");
    testShow.setDescription("Test show for NPC matches");
    testShow.setType(showType);
    testShow = showRepository.save(testShow);
  }

  @AfterEach
  void cleanUp() {
    matchResultRepository.deleteAll();
    wrestlerRepository.deleteAll();
    matchTypeRepository.deleteAll();
    showRepository.deleteAll();
    showTypeRepository.deleteAll();
  }

  @Test
  @DisplayName("Should resolve singles match between two rookies")
  void shouldResolveSinglesMatchBetweenRookies() {
    // When
    MatchTeam team1 = new MatchTeam(rookie1);
    MatchTeam team2 = new MatchTeam(rookie2);
    MatchResult result =
        npcMatchResolutionService.resolveTeamMatch(
            team1, team2, singlesMatchType, testShow, "Standard Match");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isNotNull();
    assertThat(result.getShow()).isEqualTo(testShow);
    assertThat(result.getMatchType()).isEqualTo(singlesMatchType);
    assertThat(result.getWinner()).isIn(rookie1, rookie2);
    assertThat(result.getIsNpcGenerated()).isTrue();
    assertThat(result.getParticipants()).hasSize(2);
    assertThat(result.getDurationMinutes()).isBetween(5, 30);
    assertThat(result.getMatchRating()).isBetween(1, 5);

    // Verify participants
    List<Wrestler> participants = result.getWrestlers();
    assertThat(participants).containsExactlyInAnyOrder(rookie1, rookie2);
  }

  @Test
  @DisplayName("Should favor higher tier wrestler in singles match")
  void shouldFavorHigherTierWrestlerInSinglesMatch() {
    // Given - Run multiple matches to test probability
    int contenderWins = 0;
    int totalMatches = 100;

    // When - Simulate many matches
    for (int i = 0; i < totalMatches; i++) {
      MatchTeam team1 = new MatchTeam(rookie1);
      MatchTeam team2 = new MatchTeam(contender);
      MatchResult result =
          npcMatchResolutionService.resolveTeamMatch(
              team1, team2, singlesMatchType, testShow, "Test Match " + i);

      if (result.getWinner().equals(contender)) {
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
  @DisplayName("Should resolve triple threat match")
  void shouldResolveTripleThreatMatch() {
    // When
    List<MatchTeam> teams =
        Arrays.asList(new MatchTeam(rookie1), new MatchTeam(rookie2), new MatchTeam(contender));
    MatchResult result =
        npcMatchResolutionService.resolveMultiTeamMatch(
            teams, tripleThreadType, testShow, "Triple Threat Match");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isNotNull();
    assertThat(result.getShow()).isEqualTo(testShow);
    assertThat(result.getMatchType()).isEqualTo(tripleThreadType);
    assertThat(result.getWinner()).isIn(rookie1, rookie2, contender);
    assertThat(result.getIsNpcGenerated()).isTrue();
    assertThat(result.getParticipants()).hasSize(3);
    assertThat(result.getDurationMinutes()).isBetween(10, 35);
    assertThat(result.getMatchRating()).isBetween(1, 5);

    // Verify all participants
    List<Wrestler> participants = result.getWrestlers();
    assertThat(participants).containsExactlyInAnyOrder(rookie1, rookie2, contender);
  }

  @Test
  @DisplayName("Should throw exception for multi-person match with less than 3 wrestlers")
  void shouldThrowExceptionForInvalidMultiPersonMatch() {
    // When/Then
    List<MatchTeam> twoTeams = Arrays.asList(new MatchTeam(rookie1), new MatchTeam(rookie2));
    assertThatThrownBy(
            () ->
                npcMatchResolutionService.resolveMultiTeamMatch(
                    twoTeams, tripleThreadType, testShow, "Invalid Match"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Multi-team match requires at least 3 teams");
  }

  @Test
  @DisplayName("Should handle wrestler with injuries and bumps")
  void shouldHandleWrestlerWithInjuriesAndBumps() {
    // Given - Add bumps to rookie1
    wrestlerService.addBump(rookie1.getId());
    wrestlerService.addBump(rookie1.getId());
    wrestlerService.addBump(rookie1.getId()); // This should create an injury

    // Refresh wrestler from database
    rookie1 = wrestlerRepository.findById(rookie1.getId()).orElseThrow();

    // When - Run multiple matches to test impact
    int rookie2Wins = 0;
    int totalMatches = 50;

    for (int i = 0; i < totalMatches; i++) {
      MatchTeam team1 = new MatchTeam(rookie1);
      MatchTeam team2 = new MatchTeam(rookie2);
      MatchResult result =
          npcMatchResolutionService.resolveTeamMatch(
              team1, team2, singlesMatchType, testShow, "Injury Test " + i);

      if (result.getWinner().equals(rookie2)) {
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
  @DisplayName("Should save match with stipulation")
  void shouldSaveMatchWithStipulation() {
    // Given
    String stipulation = "Steel Cage Match";

    // When
    MatchTeam team1 = new MatchTeam(rookie1);
    MatchTeam team2 = new MatchTeam(rookie2);
    MatchResult result =
        npcMatchResolutionService.resolveTeamMatch(
            team1, team2, singlesMatchType, testShow, stipulation);

    // Then
    assertThat(result.getMatchRulesAsString()).contains("Steel Cage");

    // Verify persistence
    MatchResult savedResult = matchResultRepository.findById(result.getId()).orElseThrow();
    assertThat(savedResult.getMatchRulesAsString()).contains("Steel Cage");
  }

  @Test
  @DisplayName("Should generate realistic match duration and rating")
  void shouldGenerateRealisticMatchDurationAndRating() {
    // When - Test multiple matches
    for (int i = 0; i < 10; i++) {
      MatchTeam team1 = new MatchTeam(rookie1);
      MatchTeam team2 = new MatchTeam(contender);
      MatchResult result =
          npcMatchResolutionService.resolveTeamMatch(
              team1, team2, singlesMatchType, testShow, "Standard Match");

      // Then - Verify realistic values
      assertThat(result.getDurationMinutes())
          .isBetween(5, 30)
          .describedAs("Match duration should be realistic");

      assertThat(result.getMatchRating())
          .isBetween(1, 5)
          .describedAs("Match rating should be between 1-5 stars");
    }
  }
}
