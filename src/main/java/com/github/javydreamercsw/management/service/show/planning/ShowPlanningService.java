package com.github.javydreamercsw.management.service.show.planning;

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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowPlanningService {

  private final SegmentRepository segmentRepository;
  private final RivalryService rivalryService;
  private final PromoBookingService promoBookingService;
  private final ShowPlanningDtoMapper mapper;
  private final Clock clock;

  @Transactional(readOnly = true)
  public ShowPlanningContextDTO getShowPlanningContext(Show show) {
    ShowPlanningContext context = new ShowPlanningContext();

    // Get segments from the last 30 days
    Instant showDate = show.getShowDate().atStartOfDay(clock.getZone()).toInstant();
    Instant lastMonth = showDate.minus(30, ChronoUnit.DAYS);
    log.debug("Getting segments between {} and {}", lastMonth, showDate);
    List<Segment> lastMonthSegments =
        segmentRepository.findBySegmentDateBetween(lastMonth, showDate);
    log.debug("Found {} segments", lastMonthSegments.size());
    context.setLastMonthSegments(lastMonthSegments);

    // Get current rivalries
    List<Rivalry> currentRivalries = rivalryService.getActiveRivalriesBetween(lastMonth, showDate);
    log.debug("Found {} active rivalries", currentRivalries.size());
    context.setCurrentRivalries(currentRivalries);

    // Get promos from the last month
    List<Segment> lastMonthPromos =
        lastMonthSegments.stream()
            .filter(promoBookingService::isPromoSegment)
            .collect(Collectors.toList());
    log.debug("Found {} promos in the last month", lastMonthPromos.size());
    context.setLastMonthPromos(lastMonthPromos);

    // Get show template (hardcoded for now)
    ShowTemplate template = new ShowTemplate();
    template.setShowName(show.getName());
    template.setDescription("Weekly wrestling show");
    template.setExpectedMatches(5);
    template.setExpectedPromos(2);
    context.setShowTemplate(template);

    return mapper.toDto(context);
  }
}
