package com.github.javydreamercsw.management.service.show;

import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

  @Mock private ShowRepository showRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private SegmentAdjudicationService segmentAdjudicationService;

  @InjectMocks private ShowService showService;

  private Show show;
  private Segment pendingSegment;
  private Segment adjudicatedSegment;

  @BeforeEach
  void setUp() {
    show = new Show();
    show.setId(1L);

    pendingSegment = new Segment();
    pendingSegment.setId(10L);
    pendingSegment.setAdjudicationStatus(AdjudicationStatus.PENDING);

    adjudicatedSegment = new Segment();
    adjudicatedSegment.setId(11L);
    adjudicatedSegment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
  }

  @Test
  void testAdjudicateShow() {
    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(segmentRepository.findByShow(show))
        .thenReturn(Arrays.asList(pendingSegment, adjudicatedSegment));

    showService.adjudicateShow(1L);

    verify(segmentAdjudicationService, times(1)).adjudicateMatch(pendingSegment);
    verify(segmentAdjudicationService, never()).adjudicateMatch(adjudicatedSegment);

    verify(segmentRepository, times(1)).save(pendingSegment);
    verify(segmentRepository, never()).save(adjudicatedSegment);

    assert (pendingSegment.getAdjudicationStatus() == AdjudicationStatus.ADJUDICATED);
  }
}
