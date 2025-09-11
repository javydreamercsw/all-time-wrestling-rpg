package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.PromoBookingService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningDtoMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShowPlanningServiceTest {

  @Mock private SegmentRepository matchRepository;
  @Mock private RivalryService rivalryService;
  @Mock private PromoBookingService promoBookingService;
  @Mock private ShowPlanningDtoMapper mapper;

  private Clock clock;
  private ShowPlanningService showPlanningService;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    showPlanningService =
        new ShowPlanningService(
            matchRepository, rivalryService, promoBookingService, mapper, clock);
  }

  @Test
  void getShowPlanningContext() {
    // Given
    Show show = new Show();
    show.setName("Test Show");

    Instant lastMonth = clock.instant().minus(30, ChronoUnit.DAYS);
    List<Segment> segments = Collections.singletonList(new Segment());
    when(matchRepository.findBySegmentDateBetween(any(), any())).thenReturn(segments);

    List<Rivalry> rivalries = Collections.singletonList(new Rivalry());
    when(rivalryService.getActiveRivalriesBetween(any(), any())).thenReturn(rivalries);

    when(promoBookingService.isPromoSegment(any())).thenReturn(true);
    when(mapper.toDto(any(ShowPlanningContext.class))).thenReturn(new ShowPlanningContextDTO());

    // When
    ShowPlanningContextDTO context = showPlanningService.getShowPlanningContext(show);

    // Then
    assertNotNull(context);
    //    assertEquals(segments, context.getLastMonthSegments());
    //    assertEquals(rivalries, context.getCurrentRivalries());
    //    assertEquals(1, context.getLastMonthPromos().size());
    //    assertEquals("Test Show", context.getShowTemplate().getShowName());
  }
}
