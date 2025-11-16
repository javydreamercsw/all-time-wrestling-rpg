package com.github.javydreamercsw.management.service.match;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentAdjudicationServiceTest {

  @Mock private RivalryService rivalryService;
  @Mock private WrestlerService wrestlerService;
  @Mock private FeudResolutionService feudResolutionService;
  @Mock private MultiWrestlerFeudService feudService;
  @Mock private Random random;
  @Mock private Segment segment;
  @Mock private Wrestler winner;
  @Mock private Wrestler loser;
  @Mock private SegmentType segmentType;

  private SegmentAdjudicationService segmentAdjudicationService;

  @BeforeEach
  void setUp() {
    segmentAdjudicationService =
        new SegmentAdjudicationService(
            rivalryService, wrestlerService, feudResolutionService, feudService, random);
    when(segment.getWinners()).thenReturn(List.of(winner));
    when(segment.getLosers()).thenReturn(List.of(loser));
    when(segment.getWrestlers()).thenReturn(List.of(winner, loser));
    when(winner.getId()).thenReturn(1L);
    when(loser.getId()).thenReturn(2L);
    when(segment.getSegmentType()).thenReturn(segmentType);
    when(segmentType.getName()).thenReturn("Test Match");
  }

  @Test
  void testBumpAdditionWinners() {
    SegmentRule rule = new SegmentRule();
    rule.setBumpAddition(BumpAddition.WINNERS);
    when(segment.getSegmentRules()).thenReturn(List.of(rule));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(wrestlerService, times(1)).addBump(1L);
    verify(wrestlerService, times(0)).addBump(2L);
  }

  @Test
  void testBumpAdditionLosers() {
    SegmentRule rule = new SegmentRule();
    rule.setBumpAddition(BumpAddition.LOSERS);
    when(segment.getSegmentRules()).thenReturn(List.of(rule));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(wrestlerService, times(0)).addBump(1L);
    verify(wrestlerService, times(1)).addBump(2L);
  }

  @Test
  void testBumpAdditionAll() {
    SegmentRule rule = new SegmentRule();
    rule.setBumpAddition(BumpAddition.ALL);
    when(segment.getSegmentRules()).thenReturn(List.of(rule));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(wrestlerService, times(1)).addBump(1L);
    verify(wrestlerService, times(1)).addBump(2L);
  }

  @Test
  void testBumpAdditionNone() {
    SegmentRule rule = new SegmentRule();
    rule.setBumpAddition(BumpAddition.NONE);
    when(segment.getSegmentRules()).thenReturn(List.of(rule));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(wrestlerService, times(0)).addBump(1L);
    verify(wrestlerService, times(0)).addBump(2L);
  }
}
