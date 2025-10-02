package com.github.javydreamercsw.management.service.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NPC Segment Resolution Service Tests")
class NPCSegmentResolutionServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private Clock clock;
  @Mock private Random random;

  @InjectMocks private NPCSegmentResolutionService npcSegmentResolutionService;

  private Show show;
  private SegmentType segmentType;

  @BeforeEach
  void setUp() {
    show = new Show();
    show.setId(1L);
    show.setName("Test Show");

    segmentType = new SegmentType();
    segmentType.setName("Test Segment");

    when(clock.instant()).thenReturn(Instant.now());
    when(segmentRepository.save(any(Segment.class))).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  @DisplayName("Test Team 1 wins with weighted random selection")
  void testResolveTeamSegment_Team1Wins() {
    // Given
    Wrestler w1 = createWrestler(1L, "W1", WrestlerTier.MIDCARDER, 50000);
    Wrestler w2 = createWrestler(2L, "W2", WrestlerTier.ROOKIE, 10000);
    SegmentTeam team1 = new SegmentTeam(List.of(w1), "Team 1");
    SegmentTeam team2 = new SegmentTeam(List.of(w2), "Team 2");

    // Mocking wrestler repo to handle participant adding
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(w1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(w2));

    // Team 1 has a much higher weight, so they should have a higher win probability.
    // Let's say Team 1 has 80% win probability.
    // We make the random roll be less than that to ensure Team 1 wins.
    when(random.nextDouble()).thenReturn(0.79); // 79 is less than 80

    // When
    Segment result =
        npcSegmentResolutionService.resolveTeamSegment(team1, team2, segmentType, show, "");

    // Then
    assertEquals(team1.getMembers(), result.getWinners());
  }

  @Test
  @DisplayName("Test Team 2 wins with weighted random selection")
  void testResolveTeamSegment_Team2Wins() {
    // Given
    Wrestler w1 = createWrestler(1L, "W1", WrestlerTier.MIDCARDER, 50000);
    Wrestler w2 = createWrestler(2L, "W2", WrestlerTier.ROOKIE, 10000);
    SegmentTeam team1 = new SegmentTeam(List.of(w1), "Team 1");
    SegmentTeam team2 = new SegmentTeam(List.of(w2), "Team 2");

    // Mocking wrestler repo to handle participant adding
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(w1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(w2));

    // Team 1 win probability is ~83.34%. We need the random value to be higher than that.
    // random.nextDouble() * 100 must be > 83.34. So nextDouble() must be > 0.8334.
    when(random.nextDouble()).thenReturn(0.8335);

    // When
    Segment result =
        npcSegmentResolutionService.resolveTeamSegment(team1, team2, segmentType, show, "Normal");

    // Then
    assertEquals(team2.getMembers(), result.getWinners());
  }

  @Test
  @DisplayName("Test multi-team segment winner selection")
  void testResolveMultiTeamSegment() {
    // Given
    Wrestler w1 = createWrestler(1L, "W1", WrestlerTier.MAIN_EVENTER, 100000);
    Wrestler w2 = createWrestler(2L, "W2", WrestlerTier.MIDCARDER, 50000);
    Wrestler w3 = createWrestler(3L, "W3", WrestlerTier.ROOKIE, 10000);
    SegmentTeam team1 = new SegmentTeam(List.of(w1), "Team 1"); // Highest weight
    SegmentTeam team2 = new SegmentTeam(List.of(w2), "Team 2");
    SegmentTeam team3 = new SegmentTeam(List.of(w3), "Team 3"); // Lowest weight

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(w1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(w2));
    when(wrestlerRepository.findById(3L)).thenReturn(Optional.of(w3));

    // Let's say weights are Team1=100, Team2=50, Team3=10. Total=160.
    // To make Team 2 win, randomValue should be > 100 and <= 150.
    // random.nextDouble() * 160 should be in that range.
    // e.g., if nextDouble() returns 0.7, randomValue = 112.
    when(random.nextDouble()).thenReturn(0.7);

    // When
    Segment result =
        npcSegmentResolutionService.resolveMultiTeamSegment(
            Arrays.asList(team1, team2, team3), segmentType, show, "");

    // Then
    assertEquals(team2.getMembers(), result.getWinners());
  }

  private Wrestler createWrestler(Long id, String name, WrestlerTier tier, long fans) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(id);
    wrestler.setName(name);
    wrestler.setTier(tier);
    wrestler.setFans(fans);
    return wrestler;
  }
}
