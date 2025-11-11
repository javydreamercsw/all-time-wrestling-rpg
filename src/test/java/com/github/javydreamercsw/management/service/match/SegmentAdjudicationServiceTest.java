package com.github.javydreamercsw.management.service.match;

import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
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
    ShowTemplate showTemplate = new ShowTemplate();
    ShowType showType = new ShowType();
    showType.setName("Premium Live Event (PLE)");
    show.setTemplate(showTemplate);
    show.setType(showType);
    showTemplate.setShowType(showType);
    show.setName("Biggest Event of the Year");
    segment.setShow(show);
    SegmentType st = new SegmentType();
    st.setName("One on One");
    segment.setSegmentType(st);

    MultiWrestlerFeud feud1 = new MultiWrestlerFeud();
    feud1.setId(1L);
    feud1.setName("Feud 1");
    feud1.setIsActive(true);
    MultiWrestlerFeud feud2 = new MultiWrestlerFeud();
    feud2.setId(2L);
    feud2.setName("Feud 2");
    feud2.setIsActive(true);

    Assertions.assertNotNull(champion.getId());
    when(feudService.getActiveFeudsForWrestler(champion.getId())).thenReturn(List.of(feud1));
    Assertions.assertNotNull(challenger.getId());
    when(feudService.getActiveFeudsForWrestler(challenger.getId())).thenReturn(List.of(feud2));

    when(rivalryService.getRivalryBetweenWrestlers(champion.getId(), challenger.getId()))
        .thenReturn(Optional.of(new Rivalry()));

    // When
    segmentAdjudicationService.adjudicateMatch(segment);

    // Then
    verify(feudResolutionService, times(1)).attemptFeudResolution(feud1);
    verify(feudResolutionService, times(1)).attemptFeudResolution(feud2);
    verify(rivalryService, times(1)).attemptResolution(any(), anyInt(), anyInt());
  }

  @Test
  void testAdjudicateMatch_AssignsBumpsToLosers() {
    // Given
    when(random.nextInt(anyInt())).thenReturn(10); // Mock the dice roll for non-promo
    segment.setWinners(List.of(champion)); // Champion is winner

    // When
    segmentAdjudicationService.adjudicateMatch(segment);

    // Then
    Assertions.assertNotNull(challenger.getId());
    verify(wrestlerService, times(1)).addBump(challenger.getId());
    Assertions.assertNotNull(champion.getId());
    verify(wrestlerService, never()).addBump(champion.getId());
  }

  @Test
  void testAdjudicateMatch_AddsHeatToFeuds() {
    // Given
    // Create a new segment for this test to avoid interference from setUp
    Segment testSegment = new Segment();
    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Match"); // Not a promo
    testSegment.setSegmentType(segmentType);
    testSegment.setIsTitleSegment(false); // Not a title segment
    testSegment.setTitles(Set.of()); // No titles
    testSegment.setShow(
        segment.getShow()); // Use the show from setUp, or create a new one if needed

    // Mock dice roll to ensure heat is added (e.g., not a promo, so heat = 1)
    when(random.nextInt(anyInt())).thenReturn(10); // This will make the roll 11, so not a promo.

    // Create wrestlers
    Wrestler wrestler1 = new Wrestler();
    wrestler1.setId(3L);
    wrestler1.setName("Wrestler 1");
    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(4L);
    wrestler2.setName("Wrestler 2");
    Wrestler wrestler3 = new Wrestler();
    wrestler3.setId(5L);
    wrestler3.setName("Wrestler 3");

    // Add wrestlers to testSegment
    testSegment.addParticipant(wrestler1);
    testSegment.addParticipant(wrestler2);
    testSegment.addParticipant(wrestler3);

    // Create a feud
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setId(10L);
    feud.setName("Test Feud");
    feud.setIsActive(true);
    feud.addParticipant(wrestler1, FeudRole.PROTAGONIST);
    feud.addParticipant(wrestler2, FeudRole.ANTAGONIST);
    feud.addParticipant(wrestler3, FeudRole.NEUTRAL);

    // Mock feudService to return the feud for each participant
    Assertions.assertNotNull(wrestler1.getId());
    when(feudService.getActiveFeudsForWrestler(wrestler1.getId())).thenReturn(List.of(feud));
    Assertions.assertNotNull(wrestler2.getId());
    when(feudService.getActiveFeudsForWrestler(wrestler2.getId())).thenReturn(List.of(feud));
    Assertions.assertNotNull(wrestler3.getId());
    when(feudService.getActiveFeudsForWrestler(wrestler3.getId())).thenReturn(List.of(feud));

    // Mock addHeat to do nothing, we just want to verify it's called
    when(feudService.addHeat(anyLong(), anyInt(), anyString())).thenReturn(Optional.of(feud));

    // When
    segmentAdjudicationService.adjudicateMatch(testSegment); // Use testSegment here

    // Then
    // Verify that addHeat is called exactly once for the feud with the correct heat (1 for
    // non-promo)
    Assertions.assertNotNull(feud.getId());
    verify(feudService, times(1)).addHeat(feud.getId(), 1, "From segment: Test Match");
  }
}
