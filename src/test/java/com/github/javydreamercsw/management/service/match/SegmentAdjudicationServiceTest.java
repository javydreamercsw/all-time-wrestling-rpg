package com.github.javydreamercsw.management.service.match;

import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentAdjudicationServiceTest {

  @Mock private WrestlerService wrestlerService;

  @InjectMocks private SegmentAdjudicationService segmentAdjudicationService;

  private Segment segment;
  private Title title;
  private Wrestler champion;
  private Wrestler challenger;

  @BeforeEach
  void setUp() {
    segment = new Segment();
    title = new Title();
    champion = new Wrestler();
    challenger = new Wrestler();

    champion.setId(1L);
    champion.setName("Champion");
    challenger.setId(2L);
    challenger.setName("Challenger");

    title.setId(1L);
    title.setName("ATW World Championship");
    title.setTier(WrestlerTier.MAIN_EVENTER);
    title.setChampion(List.of(champion));

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Title Match");
    segment.setSegmentType(segmentType);
    segment.setIsTitleSegment(true);
    segment.setTitles(Set.of(title));
    segment.addParticipant(champion);
    segment.addParticipant(challenger);
  }

  @Test
  void testAdjudicateMatch_ChallengerPaysFee() {
    // Given
    when(wrestlerService.awardFans(challenger.getId(), -title.getContenderEntryFee()));

    // When
    segmentAdjudicationService.adjudicateMatch(segment);

    // Then
    verify(wrestlerService, times(1)).awardFans(challenger.getId(), title.getContenderEntryFee());
    verify(wrestlerService, never()).awardFans(champion.getId(), title.getContenderEntryFee());
  }
}
