package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.show.match.MatchResultRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.PromoBookingService;
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

  @Mock private MatchResultRepository matchResultRepository;
  @Mock private RivalryService rivalryService;
  @Mock private PromoBookingService promoBookingService;

  private Clock clock;
  private ShowPlanningService showPlanningService;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    showPlanningService =
        new ShowPlanningService(matchResultRepository, rivalryService, promoBookingService, clock);
  }

  @Test
  void getShowPlanningContext() {
    // Given
    Show show = new Show();
    show.setName("Test Show");

    Instant lastMonth = clock.instant().minus(30, ChronoUnit.DAYS);
    List<MatchResult> matches = Collections.singletonList(new MatchResult());
    when(matchResultRepository.findByMatchDateAfter(lastMonth)).thenReturn(matches);

    List<Rivalry> rivalries = Collections.singletonList(new Rivalry());
    when(rivalryService.getActiveRivalries()).thenReturn(rivalries);

    when(promoBookingService.isPromoSegment(any())).thenReturn(true);

    // When
    ShowPlanningContext context = showPlanningService.getShowPlanningContext(show);

    // Then
    assertNotNull(context);
    assertEquals(matches, context.getLastMonthMatches());
    assertEquals(rivalries, context.getCurrentRivalries());
    assertEquals(1, context.getLastMonthPromos().size());
    assertEquals("Test Show", context.getShowTemplate().getShowName());
  }
}
