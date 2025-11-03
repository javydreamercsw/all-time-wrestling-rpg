package com.github.javydreamercsw.management.ui.view.segment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class NarrationDialogTest {

  @Mock private NpcService npcService;
  @Mock private WrestlerService wrestlerService;
  @Mock private TitleService titleService;
  @Mock private ShowService showService;

  private NarrationDialog narrationDialog;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    Show show = new Show();
    show.setId(1L);
    show.setName("Test Show");

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Type");

    Segment segment1 = new Segment();
    segment1.setId(1L);
    segment1.setSegmentOrder(1);
    segment1.setNarration("Segment 1 Narration");
    segment1.setSummary("Segment 1 Summary");
    segment1.setShow(show);
    segment1.setSegmentType(segmentType);

    Segment segment2 = new Segment();
    segment2.setId(2L);
    segment2.setSegmentOrder(2);
    segment2.setShow(show);
    segment2.setSegmentType(segmentType);

    List<Segment> segments = new ArrayList<>();
    segments.add(segment1);
    segments.add(segment2);

    when(showService.getSegments(show)).thenReturn(segments);

    narrationDialog =
        new NarrationDialog(
            segment2, npcService, wrestlerService, titleService, showService, segment -> {});
  }

  @Test
  void testBuildSegmentContext_withPreviousSegments() {
    // When
    SegmentNarrationService.SegmentNarrationContext context = narrationDialog.buildSegmentContext();

    // Then
    assertNotNull(context.getPreviousSegments());
    assertEquals(1, context.getPreviousSegments().size());

    SegmentNarrationService.SegmentNarrationContext previousSegmentContext =
        context.getPreviousSegments().get(0);
    assertEquals("Segment 1 Narration", previousSegmentContext.getNarration());
    assertEquals("Segment 1 Summary", previousSegmentContext.getDeterminedOutcome());
  }
}
