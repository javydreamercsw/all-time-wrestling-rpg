package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ShowDetailViewTest extends ManagementIntegrationTest {

  @Autowired private ShowService showService;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;

  @Test
  void testEditSegmentResetsAdjudicationStatus() {
    ShowType showType = new ShowType();
    showType.setName("Test Show Type");
    showType.setDescription("Test Description");
    showType = showTypeRepository.save(showType);

    Show show = new Show();
    show.setName("Test Show");
    show.setDescription("Test Description");
    show.setType(showType);
    show = showService.save(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Segment Type");
    segmentType = segmentTypeRepository.save(segmentType);

    Segment segment = new Segment();
    segment.setId(10L);
    segment.setShow(show);
    segment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
    segment.setSegmentType(segmentType);

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler 1");

    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler 2");

    Set<Wrestler> wrestlers = new HashSet<>(Arrays.asList(wrestler1, wrestler2));

    ShowDetailView showDetailView = new ShowDetailView();
    ReflectionTestUtils.setField(showDetailView, "showService", showService);
    ReflectionTestUtils.setField(showDetailView, "segmentRepository", segmentRepository);

    ReflectionTestUtils.invokeMethod(
        showDetailView,
        "validateAndSaveSegment",
        show,
        segmentType,
        wrestlers,
        Collections.emptySet(),
        Collections.emptySet(),
        segment);

    assertEquals(AdjudicationStatus.PENDING, segment.getAdjudicationStatus());
  }

  @Test
  void testSegmentReordering() {
    // Create a show with two segments
    ShowType showType = new ShowType();
    showType.setName("Test Show Type");
    showType.setDescription("Test Description");
    showType = showTypeRepository.save(showType);

    Show show = new Show();
    show.setName("Test Show");
    show.setDescription("Test Description");
    show.setType(showType);
    show = showService.save(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Segment Type");
    segmentType = segmentTypeRepository.save(segmentType);

    Segment segment1 = new Segment();
    segment1.setShow(show);
    segment1.setSegmentOrder(1);
    segment1.setSegmentType(segmentType);
    segment1 = segmentRepository.save(segment1);

    Segment segment2 = new Segment();
    segment2.setShow(show);
    segment2.setSegmentOrder(2);
    segment2.setSegmentType(segmentType);
    segment2 = segmentRepository.save(segment2);

    ShowDetailView showDetailView = new ShowDetailView();
    ReflectionTestUtils.setField(showDetailView, "showService", showService);
    ReflectionTestUtils.setField(showDetailView, "segmentRepository", segmentRepository);
    BeforeEvent beforeEvent = Mockito.mock(BeforeEvent.class);
    Mockito.when(beforeEvent.getLocation()).thenReturn(new Location(""));
    showDetailView.setParameter(beforeEvent, show.getId());

    Grid<Segment> grid = showDetailView.getSegmentsGrid(segmentRepository.findByShow(show));
    assertNotNull(grid);

    // Simulate clicking the down button on the first segment
    showDetailView.moveSegment(segment1, 1);

    // Verify the new order
    assertEquals(2, segment1.getSegmentOrder());
    assertEquals(1, segment2.getSegmentOrder());
  }
}
