package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.vaadin.flow.component.UI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class ShowDetailViewTest {

  @Mock private SegmentRepository segmentRepository;

  @InjectMocks private ShowDetailView showDetailView;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    // Mock the UI since we are not in a Vaadin environment
    UI ui = mock(UI.class);
    UI.setCurrent(ui);
    when(ui.getUI()).thenReturn(Optional.of(ui));
  }

  @Test
  void testEditSegmentResetsAdjudicationStatus() {
    Show show = new Show();
    show.setId(1L);

    Segment segment = new Segment();
    segment.setId(10L);
    segment.setShow(show);
    segment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
    SegmentType type = new SegmentType();
    type.setName("Test Type");
    segment.setSegmentType(type);

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler 1");

    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler 2");

    Set<Wrestler> wrestlers = new HashSet<>(Arrays.asList(wrestler1, wrestler2));

    when(segmentRepository.save(any(Segment.class))).thenReturn(segment);

    ReflectionTestUtils.invokeMethod(
        showDetailView,
        "validateAndSaveSegment",
        show,
        type,
        wrestlers,
        Collections.emptySet(),
        Collections.emptySet(),
        segment);

    ArgumentCaptor<Segment> captor = ArgumentCaptor.forClass(Segment.class);
    verify(segmentRepository).save(captor.capture());

    assertEquals(AdjudicationStatus.PENDING, captor.getValue().getAdjudicationStatus());
  }
}
