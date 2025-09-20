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
  void testAdjudicateMatch() {
    // Given
    long initialFans = 10000L;
    Wrestler winner = new Wrestler();
    winner.setName("Winner");
    winner.setFans(initialFans);
    winner.setBumps(0);
    winner.setStartingHealth(100);
    winner.setStartingStamina(100);
    winner.setLowHealth(20);
    winner.setLowStamina(20);
    winner.setDeckSize(50);
    winner.setTier(WrestlerTier.MIDCARDER);
    winner.setIsPlayer(false);
    wrestlerRepository.save(winner);

    Wrestler loser = new Wrestler();
    loser.setName("Loser");
    loser.setFans(initialFans);
    loser.setBumps(0);
    loser.setStartingHealth(100);
    loser.setStartingStamina(100);
    loser.setLowHealth(20);
    loser.setLowStamina(20);
    loser.setDeckSize(50);
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
    Wrestler updatedWinner = wrestlerRepository.findById(winner.getId()).get();
    Wrestler updatedLoser = wrestlerRepository.findById(loser.getId()).get();

    // Winner fan gain: (2d6 + 3) * 1000 + matchQualityBonus
    // Min: (2+3)*1000 + 0 = 5000. Max: (12+3)*1000 + 10000 = 25000
    org.assertj.core.api.Assertions.assertThat(updatedWinner.getFans())
        .isBetween(initialFans + 5000, initialFans + 25000);

    // Loser fan gain: (1d6 + 3) * 1000 + matchQualityBonus
    // Min: (1+3)*1000 + 0 = 4000. Max: (6+3)*1000 + 10000 = 19000
    org.assertj.core.api.Assertions.assertThat(updatedLoser.getFans())
        .isBetween(initialFans + 4000, initialFans + 19000);

    assertEquals(1, updatedWinner.getBumps());
    assertEquals(1, updatedLoser.getBumps());
  }
}
