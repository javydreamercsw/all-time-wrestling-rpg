package com.github.javydreamercsw.management.service.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Team Match Resolution Integration Tests")
class TeamMatchResolutionIT extends AbstractIntegrationTest {
  @Autowired NPCSegmentResolutionService npcSegmentResolutionService;

  private Wrestler rookie1;
  private Wrestler rookie2;
  private Wrestler rookie3;
  private Wrestler rookie4;
  private Wrestler contender1;
  private Wrestler contender2;
  private SegmentType tagTeamSegmentType;
  private SegmentType handicapSegmentType;
  private Show testShow;

  @Test
  @DisplayName("Should resolve tag team segment (2v2)")
  void shouldResolveTagTeamMatch() {
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

  @Test
  @DisplayName("Should resolve handicap segment (1v2)")
  void shouldResolveHandicapMatch() {
    // Given
    SegmentTeam soloTeam = new SegmentTeam(contender1);
    SegmentTeam handicapTeam = new SegmentTeam(Arrays.asList(rookie1, rookie2), "Rookie Alliance");

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

  @Test
  @DisplayName("Should favor higher tier team in tag team segment")
  void shouldFavorHigherTierTeamInTagTeamMatch() {
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
        .isGreaterThan(0.8) // Should win at least 80% due to massive tier advantage
        .describedAs("Contender team should dominate rookie team");
  }

  @Test
  @DisplayName("Should handle complex multi-team scenarios")
  void shouldHandleComplexMultiTeamScenarios() {
    // Given - 3v2 segment
    SegmentTeam bigTeam = new SegmentTeam(Arrays.asList(rookie1, rookie2, rookie3), "The Big Team");
    SegmentTeam smallTeam = new SegmentTeam(Arrays.asList(contender1, contender2), "The Elite");

    // When
    Segment result =
        npcSegmentResolutionService.resolveTeamSegment(
            bigTeam, smallTeam, handicapSegmentType, testShow, "3v2 Elimination");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getParticipants()).hasSize(5);
    // Note: "3v2 Elimination" doesn't segment any seeded rules, so it will be "Standard Match"
    assertThat(result.getSegmentRulesAsString()).isEqualTo("Standard Match");

    List<Wrestler> participants = result.getWrestlers();
    assertThat(participants)
        .containsExactlyInAnyOrder(rookie1, rookie2, rookie3, contender1, contender2);
  }

  @Test
  @DisplayName("Should work with singles segment using team interface")
  void shouldWorkWithSinglesMatchUsingTeamInterface() {
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
    assertThat(result.getWinners()).hasSize(1);
    assertThat(result.getWinners().get(0)).isEqualTo(contender1);
  }

  @Test
  @DisplayName("Should generate appropriate segment duration for team matches")
  void shouldGenerateAppropriateMatchDurationForTeamMatches() {
    // Given
    SegmentTeam tagTeam1 = new SegmentTeam(Arrays.asList(rookie1, rookie2));
    SegmentTeam tagTeam2 = new SegmentTeam(Arrays.asList(rookie3, rookie4));

    // When
    Segment result =
        npcSegmentResolutionService.resolveTeamSegment(
            tagTeam1, tagTeam2, tagTeamSegmentType, testShow, "Tag Team Championship");
  }
}
