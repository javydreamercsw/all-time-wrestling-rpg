package com.github.javydreamercsw.management.service.match;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MatchAdjudicationServiceTest {
  @Autowired private MatchAdjudicationService matchAdjudicationService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private WrestlerService wrestlerService;

  @Test
  @Disabled("Disabled due to pipeline failures; needs investigation into Testcontainers/MySQL setup. See: https://github.com/javydreamercsw/all-time-wrestling-rpg/issues/10")
  void testAdjudicateMatch() {
    // Given
    long initialFans = 10_000L;
    Wrestler winner = new Wrestler();
    winner.setName("Winner");
    winner.setFans(initialFans);
    winner.setBumps(0);
    winner.setStartingHealth(13);
    winner.setStartingStamina(14);
    winner.setLowHealth(2);
    winner.setLowStamina(2);
    winner.setDeckSize(16);
    winner.setTier(WrestlerTier.MIDCARDER);
    winner.setIsPlayer(false);
    wrestlerRepository.save(winner);

    Wrestler loser = new Wrestler();
    loser.setName("Loser");
    loser.setFans(initialFans);
    loser.setBumps(0);
    loser.setStartingHealth(14);
    loser.setStartingStamina(16);
    loser.setLowHealth(4);
    loser.setLowStamina(2);
    loser.setDeckSize(15);
    loser.setTier(WrestlerTier.MIDCARDER);
    loser.setIsPlayer(false);
    wrestlerRepository.save(loser);

    ShowType showType = new ShowType();
    showType.setName("Test Show Type");
    showType.setDescription("Test Description");
    showTypeRepository.save(showType);

    Show show = new Show();
    show.setName("Test Show");
    show.setDescription("Test Description");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    showRepository.save(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Match");
    segmentTypeRepository.save(segmentType);

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment.addParticipant(winner);
    segment.addParticipant(loser);
    segment.setWinners(List.of(winner));
    segmentRepository.save(segment);

    // When
    matchAdjudicationService.adjudicateMatch(segment);

    // Then
    Assertions.assertNotNull(winner.getId());
    Optional<Wrestler> updatedWinner =
        wrestlerRepository.findById(winner.getId()).stream().findFirst();
    Assertions.assertTrue(updatedWinner.isPresent());
    Assertions.assertNotNull(loser.getId());
    Optional<Wrestler> updatedLoser =
        wrestlerRepository.findById(loser.getId()).stream().findFirst();
    Assertions.assertTrue(updatedLoser.isPresent());

    // Winner fan gain: (2d6 + 3) * 1000 + matchQualityBonus
    // Min: (2+3)*1000 + 0 = 5000. Max: (12+3)*1000 + 10000 = 25000
    org.assertj.core.api.Assertions.assertThat(updatedWinner.get().getFans())
        .isBetween(initialFans + 5_000, initialFans + 25_000);

    // Loser fan gain: (1d6 + 3) * 1000 + matchQualityBonus
    // Min: (1+3)*1000 + 0 = 4000. Max: (6+3)*1000 + 10000 = 19000
    org.assertj.core.api.Assertions.assertThat(updatedLoser.get().getFans())
        .isBetween(initialFans + 4_000, initialFans + 19_000);

    assertEquals(1, updatedWinner.get().getBumps());
    assertEquals(1, updatedLoser.get().getBumps());
  }
}
