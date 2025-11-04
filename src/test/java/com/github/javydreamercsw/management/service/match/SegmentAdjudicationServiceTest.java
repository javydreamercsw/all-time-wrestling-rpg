package com.github.javydreamercsw.management.service.match;

import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentAdjudicationServiceTest {

  @Mock private RivalryService rivalryService;
  @Mock private WrestlerService wrestlerService;
  @Mock private FeudResolutionService feudResolutionService;
  @Mock private MultiWrestlerFeudService feudService;
  @Mock private Random random;

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

    Show show = new Show();
    ShowType showType = new ShowType();
    showType.setName("Weekly Show");
    show.setType(showType);
    segment.setShow(show);
  }

  @Test
  void testAdjudicateMatch_ChallengerPaysFee() {
    // Given
    when(random.nextInt(anyInt())).thenReturn(10); // Mock the dice roll
    Assertions.assertNotNull(challenger.getId());
    when(wrestlerService.awardFans(challenger.getId(), -title.getContenderEntryFee()))
        .thenReturn(Optional.of(challenger));

    // When
    segmentAdjudicationService.adjudicateMatch(segment);

    // Then
    Assertions.assertNotNull(challenger.getId());
    verify(wrestlerService, times(1)).awardFans(challenger.getId(), -title.getContenderEntryFee());
    Assertions.assertNotNull(champion.getId());
    verify(wrestlerService, never()).awardFans(champion.getId(), -title.getContenderEntryFee());
  }

  @Test
  void testAdjudicateMatch_PLEFeudResolution() {
    // Given
    Show show = new Show();
    ShowType showType = new ShowType();
    showType.setName("Premium Live Event (PLE)");
    show.setType(showType);
    show.setName("Biggest Event of the Year");
    segment.setShow(show);

    MultiWrestlerFeud feud1 = new MultiWrestlerFeud();
    feud1.setId(1L);
    feud1.setHeat(25);
    feud1.setIsActive(true);
    MultiWrestlerFeud feud2 = new MultiWrestlerFeud();
    feud2.setId(2L);
    feud2.setHeat(30);
    feud2.setIsActive(true);

    Assertions.assertNotNull(champion.getId());
    when(feudService.getActiveFeudsForWrestler(champion.getId())).thenReturn(List.of(feud1));
    Assertions.assertNotNull(challenger.getId());
    when(feudService.getActiveFeudsForWrestler(challenger.getId())).thenReturn(List.of(feud2));

    // When
    segmentAdjudicationService.adjudicateMatch(segment);

    // Then
    verify(feudResolutionService, times(1)).attemptFeudResolution(feud1);
    verify(feudResolutionService, times(1)).attemptFeudResolution(feud2);
  }
}
