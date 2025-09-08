package com.github.javydreamercsw.management.service.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.TestcontainersConfiguration;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.match.Match;
import com.github.javydreamercsw.management.domain.show.match.MatchRepository;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.match.type.MatchTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Arrays;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("Team Match Resolution Integration Tests")
class TeamMatchResolutionIT {

  @Autowired NPCMatchResolutionService npcMatchResolutionService;
  @Autowired WrestlerService wrestlerService;
  @Autowired WrestlerRepository wrestlerRepository;
  @Autowired MatchRepository matchRepository;
  @Autowired MatchTypeRepository matchTypeRepository;
  @Autowired ShowRepository showRepository;
  @Autowired ShowTypeRepository showTypeRepository;
  @Autowired MatchRuleService matchRuleService;
  @Autowired DeckRepository deckRepository; // Autowire DeckRepository

  private Wrestler rookie1;
  private Wrestler rookie2;
  private Wrestler rookie3;
  private Wrestler rookie4;
  private Wrestler contender1;
  private Wrestler contender2;
  private MatchType tagTeamMatchType;
  private MatchType handicapMatchType;
  private Show testShow;

  private Wrestler createWrestler(@NonNull String name) {
    Wrestler w = new Wrestler();
    w.setName("Rookie One");
    w.setIsPlayer(true);
    w.setDeckSize(0);
    w.setStartingStamina(100);
    w.setLowStamina(25);
    w.setStartingHealth(100);
    w.setLowHealth(25);
    w = wrestlerRepository.saveAndFlush(w);
    return w;
  }

  @BeforeEach
  void setUp() {
    // Create and save test wrestlers
    rookie1 = createWrestler("Rookie One");

    rookie2 = createWrestler("Rookie Two");

    rookie3 = createWrestler("Rookie Three");

    rookie4 = createWrestler("Rookie Four");

    contender1 = createWrestler("Contender One");

    contender2 = createWrestler("Contender Two");

    // Award fans to create tier differences
    // Now that wrestlers are saved, their IDs are available and they are managed
    Assertions.assertNotNull(contender1.getId());
    wrestlerService.awardFans(contender1.getId(), 45_000L); // CONTENDER tier
    Assertions.assertNotNull(contender2.getId());
    wrestlerService.awardFans(contender2.getId(), 45_000L); // CONTENDER tier

    // Refresh wrestler entities from database to get updated fan counts
    // These should now be found as they were explicitly saved and flushed
    contender1 = wrestlerRepository.findById(contender1.getId()).orElseThrow();
    contender2 = wrestlerRepository.findById(contender2.getId()).orElseThrow();

    // Create match types (rely on DataInitializer for these)
    tagTeamMatchType = matchTypeRepository.findByName("Tag Team").orElseThrow();
    handicapMatchType = matchTypeRepository.findByName("Handicap Match").orElseThrow();

    // Create match rules for testing
    matchRuleService.createOrUpdateRule(
        "Handicap Match", "Handicap match with uneven teams", false);

    // Create test show
    ShowType showType = new ShowType();
    showType.setName("Weekly Show");
    showType.setDescription("Weekly wrestling show for testing");
    showType = showTypeRepository.save(showType);

    testShow = new Show();
    testShow.setName("Test Show");
    testShow.setDescription("Test show for team matches");
    testShow.setType(showType);
    testShow = showRepository.save(testShow);
  }

  @AfterEach
  void cleanUp() {
    matchRepository.deleteAll();
    deckRepository.deleteAll(); // Delete decks before wrestlers
    wrestlerRepository.deleteAll();
    matchTypeRepository.deleteAll();
    showRepository.deleteAll();
    showTypeRepository.deleteAll();
  }

  @Test
  @DisplayName("Should resolve tag team match (2v2)")
  void shouldResolveTagTeamMatch() {
    // Given
    MatchTeam team1 = new MatchTeam(Arrays.asList(rookie1, rookie2), "The Rookies");
    MatchTeam team2 = new MatchTeam(Arrays.asList(rookie3, rookie4), "The Newbies");

    // When
    Match result =
        npcMatchResolutionService.resolveTeamMatch(
            team1, team2, tagTeamMatchType, testShow, "Tag Team Match");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isNotNull();
    assertThat(result.getShow()).isEqualTo(testShow);
    assertThat(result.getMatchType()).isEqualTo(tagTeamMatchType);
    assertThat(result.getWinner()).isIn(rookie1, rookie2, rookie3, rookie4);
    assertThat(result.getIsNpcGenerated()).isTrue();
    assertThat(result.getParticipants()).hasSize(4);
    assertThat(result.getDurationMinutes()).isBetween(10, 35);
    assertThat(result.getMatchRating()).isBetween(1, 5);

    // Verify all participants
    List<Wrestler> participants = result.getWrestlers();
    assertThat(participants).containsExactlyInAnyOrder(rookie1, rookie2, rookie3, rookie4);
  }

  @Test
  @DisplayName("Should resolve handicap match (1v2)")
  void shouldResolveHandicapMatch() {
    // Given
    MatchTeam soloTeam = new MatchTeam(contender1);
    MatchTeam handicapTeam = new MatchTeam(Arrays.asList(rookie1, rookie2), "Rookie Alliance");

    // When
    Match result =
        npcMatchResolutionService.resolveTeamMatch(
            soloTeam, handicapTeam, handicapMatchType, testShow, "Handicap Match");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getParticipants()).hasSize(3);
    assertThat(result.getMatchRulesAsString()).contains("Handicap");

    // The contender should have a good chance despite being outnumbered
    List<Wrestler> participants = result.getWrestlers();
    assertThat(participants).containsExactlyInAnyOrder(contender1, rookie1, rookie2);
  }

  @Test
  @DisplayName("Should favor higher tier team in tag team match")
  void shouldFavorHigherTierTeamInTagTeamMatch() {
    // Given
    MatchTeam rookieTeam = new MatchTeam(Arrays.asList(rookie1, rookie2), "Rookie Team");
    MatchTeam contenderTeam =
        new MatchTeam(Arrays.asList(contender1, contender2), "Contender Team");

    // When - Run multiple matches to test probability
    int contenderTeamWins = 0;
    int totalMatches = 50;

    for (int i = 0; i < totalMatches; i++) {
      Match result =
          npcMatchResolutionService.resolveTeamMatch(
              rookieTeam, contenderTeam, tagTeamMatchType, testShow, "Test Match " + i);

      // Check if any contender won (representing their team)
      if (result.getWinner().equals(contender1) || result.getWinner().equals(contender2)) {
        contenderTeamWins++;
      }
    }

    // Then - Contender team should win significantly more often
    double contenderTeamWinRate = (double) contenderTeamWins / totalMatches;
    assertThat(contenderTeamWinRate)
        .isGreaterThan(0.8) // Should win at least 80% due to massive tier advantage
        .describedAs("Contender team should dominate rookie team");
  }

  @Test
  @DisplayName("Should handle complex multi-team scenarios")
  void shouldHandleComplexMultiTeamScenarios() {
    // Given - 3v2 match
    MatchTeam bigTeam = new MatchTeam(Arrays.asList(rookie1, rookie2, rookie3), "The Big Team");
    MatchTeam smallTeam = new MatchTeam(Arrays.asList(contender1, contender2), "The Elite");

    // When
    Match result =
        npcMatchResolutionService.resolveTeamMatch(
            bigTeam, smallTeam, handicapMatchType, testShow, "3v2 Elimination");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getParticipants()).hasSize(5);
    // Note: "3v2 Elimination" doesn't match any seeded rules, so it will be "Standard Match"
    assertThat(result.getMatchRulesAsString()).isEqualTo("Standard Match");
    assertThat(result.getDurationMinutes()).isBetween(15, 35); // Longer due to complexity

    List<Wrestler> participants = result.getWrestlers();
    assertThat(participants)
        .containsExactlyInAnyOrder(rookie1, rookie2, rookie3, contender1, contender2);
  }

  @Test
  @DisplayName("Should work with singles match using team interface")
  void shouldWorkWithSinglesMatchUsingTeamInterface() {
    // Given
    MatchTeam team1 = new MatchTeam(rookie1);
    MatchTeam team2 = new MatchTeam(contender1);

    // When
    Match result =
        npcMatchResolutionService.resolveTeamMatch(
            team1, team2, tagTeamMatchType, testShow, "Singles Match via Team Interface");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getParticipants()).hasSize(2);
    assertThat(result.getWrestlers()).containsExactlyInAnyOrder(rookie1, contender1);

    // Should still favor the contender
    assertThat(result.getWinner()).isEqualTo(contender1);
  }

  @Test
  @DisplayName("Should generate appropriate match duration for team matches")
  void shouldGenerateAppropriateMatchDurationForTeamMatches() {
    // Given
    MatchTeam tagTeam1 = new MatchTeam(Arrays.asList(rookie1, rookie2));
    MatchTeam tagTeam2 = new MatchTeam(Arrays.asList(rookie3, rookie4));

    // When
    Match result =
        npcMatchResolutionService.resolveTeamMatch(
            tagTeam1, tagTeam2, tagTeamMatchType, testShow, "Tag Team Championship");

    // Then - Tag team matches should be longer than singles
    assertThat(result.getDurationMinutes())
        .isBetween(12, 35) // Longer than typical singles matches
        .describedAs("Tag team matches should have longer duration");
  }
}
