package com.github.javydreamercsw.management.service.segment;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentAdjudicationServiceUnitTest {

  @Mock private WrestlerService wrestlerService;

  @Mock private Random random;

  @InjectMocks private SegmentAdjudicationService adjudicationService;

  private Segment promoSegment;
  private Wrestler wrestler1;
  private Wrestler wrestler2;

  @BeforeEach
  void setUp() {
    wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler 1");

    wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler 2");

    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");

    promoSegment = new Segment();
    promoSegment.setSegmentType(promoType);
    promoSegment.addParticipant(wrestler1);
    promoSegment.addParticipant(wrestler2);
  }

  @Test
  void testAdjudicatePromo_Roll1() {
    // Roll 1 on d20
    when(random.nextInt(20)).thenReturn(0);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 0
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(0L));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(0L));
  }

  @Test
  void testAdjudicatePromo_Roll2() {
    // Roll 2 on d20
    when(random.nextInt(20)).thenReturn(1);
    // Roll 2 on d3 for bonus (1+1)
    when(random.nextInt(3)).thenReturn(1);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 2
    long expectedFans = 2 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll3() {
    // Roll 3 on d20
    when(random.nextInt(20)).thenReturn(2);
    // Roll 3 on d3 for bonus (2+1)
    when(random.nextInt(3)).thenReturn(2);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 3
    long expectedFans = 3 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll4() {
    // Roll 4 on d20
    when(random.nextInt(20)).thenReturn(3);
    // Roll 4 on d6 for bonus (3+1)
    when(random.nextInt(6)).thenReturn(3);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 4
    long expectedFans = 4 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll16() {
    // Roll 16 on d20
    when(random.nextInt(20)).thenReturn(15);
    // Roll 5 on d6 for bonus (4+1)
    when(random.nextInt(6)).thenReturn(4);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 5
    long expectedFans = 5 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll17() {
    // Roll 17 on d20
    when(random.nextInt(20)).thenReturn(16);
    // Roll 3 on first d6 (2+1), 4 on second d6 (3+1) for bonus
    when(random.nextInt(6)).thenReturn(2).thenReturn(3);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 3 + 4 = 7
    long expectedFans = 7 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll19() {
    // Roll 19 on d20
    when(random.nextInt(20)).thenReturn(18);
    // Roll 5 on first d6 (4+1), 6 on second d6 (5+1) for bonus
    when(random.nextInt(6)).thenReturn(4).thenReturn(5);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 5 + 6 = 11
    long expectedFans = 11 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll20() {
    // Roll 20 on d20
    when(random.nextInt(20)).thenReturn(19);
    // Roll 1, 2, 3 on three d6's for bonus
    when(random.nextInt(6)).thenReturn(0).thenReturn(1).thenReturn(2);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 1 + 2 + 3 = 6
    long expectedFans = 6 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }
}
