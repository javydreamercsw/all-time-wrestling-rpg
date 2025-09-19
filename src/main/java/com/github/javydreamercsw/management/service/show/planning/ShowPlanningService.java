package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.PromoBookingService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningDtoMapper;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
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
  private final TitleService titleService;
  private final ShowService showService;
  private final com.github.javydreamercsw.management.service.segment.SegmentService segmentService;
  private final com.github.javydreamercsw.management.service.segment.SegmentSummaryService
      segmentSummaryService;
  private final SegmentTypeService segmentTypeService;
  private final WrestlerService wrestlerService;

  @Transactional
  public ShowPlanningContextDTO getShowPlanningContext(@NonNull Show show) {
    ShowPlanningContext context = new ShowPlanningContext();

    // Get segments from the last 30 days
    Instant showDate = show.getShowDate().atStartOfDay(clock.getZone()).toInstant();
    Instant lastMonth = showDate.minus(30, ChronoUnit.DAYS);
    log.debug("Getting segments between {} and {}", lastMonth, showDate);
    List<Segment> lastMonthSegments =
        segmentRepository.findBySegmentDateBetween(lastMonth, showDate);
    log.debug("Found {} segments", lastMonthSegments.size());

    // New logic to generate summaries
    lastMonthSegments.forEach(
        segment -> {
          if ((segment.getSummary() == null || segment.getSummary().isEmpty())
              && (segment.getNarration() != null && !segment.getNarration().isEmpty())) {
            try {
              segmentSummaryService.summarizeSegment(segment.getId());
              // After summarizing, reload the segment to get the updated summary
              segmentService
                  .findById(segment.getId())
                  .ifPresent(
                      updatedSegment -> {
                        segment.setSummary(updatedSegment.getSummary());
                      });
            } catch (Exception e) {
              log.error("Failed to generate summary for segment: {}", segment.getId(), e);
            }
          }
        });

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

    // Get championships
    List<ShowPlanningChampionship> championships = new ArrayList<>();
    List<Title> activeTitles = titleService.getActiveTitles();
    log.info("Found {} active titles", activeTitles.size());
    for (Title title : activeTitles) {
      ShowPlanningChampionship championship = new ShowPlanningChampionship();
      championship.setTitle(title);
      if (!title.getCurrentChampions().isEmpty()) {
        championship.getChampions().addAll(title.getCurrentChampions());
      }
      // Use only the current #1 contender(s) for this title
      List<Wrestler> numberOneContenders = title.getContender();
      if (numberOneContenders != null && !numberOneContenders.isEmpty()) {
        championship.getContenders().addAll(numberOneContenders);
      }
      championships.add(championship);
    }
    context.setChampionships(championships);

    // Get next PLE
    Optional<Show> nextPle =
        showService.getUpcomingShows(10).stream().filter(Show::isPremiumLiveEvent).findFirst();
    if (nextPle.isPresent()) {
      ShowPlanningPle ple = new ShowPlanningPle();
      ple.setPle(nextPle.get());
      context.setNextPle(ple);
    }

    return mapper.toDto(context);
  }

  @Transactional
  public void approveSegments(@NonNull Show show, @NonNull List<ProposedSegment> proposedSegments) {
    List<Segment> segmentsToSave = new ArrayList<>();
    for (ProposedSegment proposedSegment : proposedSegments) {
      log.info("Processing segment: {}", proposedSegment);
      Segment segment = new Segment();
      segment.setShow(show);
      segment.setSegmentDate(show.getShowDate().atStartOfDay(clock.getZone()).toInstant());
      segment.setNarration(proposedSegment.getDescription());

      segmentTypeService.findByName(proposedSegment.getType()).ifPresent(segment::setSegmentType);

      for (String participantName : proposedSegment.getParticipants()) {
        wrestlerService.findByName(participantName).ifPresent(segment::addParticipant);
      }

      segmentsToSave.add(segment);
    }
    segmentRepository.saveAll(segmentsToSave);
    log.info("Approved and saved {} segments for show: {}", segmentsToSave.size(), show.getName());
  }
}
