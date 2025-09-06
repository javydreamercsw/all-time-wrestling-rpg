package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.show.match.MatchResultRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.PromoBookingService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowPlanningService {

  private final MatchResultRepository matchResultRepository;
  private final RivalryService rivalryService;
  private final PromoBookingService promoBookingService;
  private final Clock clock;

  public ShowPlanningContext getShowPlanningContext(Show show) {
    ShowPlanningContext context = new ShowPlanningContext();

    // Get matches from the last 30 days
    Instant showDate = show.getShowDate().atStartOfDay(clock.getZone()).toInstant();
    Instant lastMonth = showDate.minus(30, ChronoUnit.DAYS);
    log.debug("Getting matches between {} and {}", lastMonth, showDate);
    List<MatchResult> lastMonthMatches =
        matchResultRepository.findByMatchDateBetween(lastMonth, showDate);
    log.debug("Found {} matches", lastMonthMatches.size());
    context.setLastMonthMatches(lastMonthMatches);

    // Get current rivalries
    List<Rivalry> currentRivalries = rivalryService.getActiveRivalriesBetween(lastMonth, showDate);
    log.debug("Found {} active rivalries", currentRivalries.size());
    context.setCurrentRivalries(currentRivalries);

    // Get promos from the last month
    List<MatchResult> lastMonthPromos =
        lastMonthMatches.stream()
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

    return context;
  }
}
