/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.SegmentsApprovedEvent;
import com.github.javydreamercsw.management.service.faction.FactionService;
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
import org.springframework.context.ApplicationEventPublisher;
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
  private final FactionService factionService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public ShowPlanningContextDTO getShowPlanningContext(@NonNull Show show) {
    ShowPlanningContext context = new ShowPlanningContext();

    // Get segments from the last 7 days
    Instant showDate = show.getShowDate().atStartOfDay(clock.getZone()).toInstant();
    context.setShowDate(showDate);
    Instant lastWeek = showDate.minus(7, ChronoUnit.DAYS);
    log.debug("Getting segments between {} and {}", lastWeek, showDate);
    List<Segment> lastWeekSegments = segmentRepository.findBySegmentDateBetween(lastWeek, showDate);
    log.debug("Found {} segments", lastWeekSegments.size());

    // New logic to generate summaries
    lastWeekSegments.forEach(
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

    context.setRecentSegments(lastWeekSegments);

    // Get current rivalries
    List<Rivalry> currentRivalries = rivalryService.getActiveRivalries();
    log.debug("Found {} active rivalries", currentRivalries.size());
    context.setCurrentRivalries(currentRivalries);

    // Get promos from the last month
    List<Segment> lastWeekPromos =
        lastWeekSegments.stream()
            .filter(promoBookingService::isPromoSegment)
            .collect(Collectors.toList());
    log.debug("Found {} promos in the last month", lastWeekPromos.size());
    context.setRecentPromos(lastWeekPromos);

    // Get show template (hardcoded for now)
    ShowTemplate template = new ShowTemplate();
    template.setShowName(show.getName());
    template.setDescription(show.getDescription());
    template.setExpectedMatches(show.getType().getExpectedMatches());
    template.setExpectedPromos(show.getType().getExpectedPromos());
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

    // Get all wrestlers
    List<Wrestler> allWrestlers = wrestlerService.findAll();
    log.debug("Found {} wrestlers in the roster", allWrestlers.size());
    context.setFullRoster(allWrestlers);

    // Get all factions
    List<com.github.javydreamercsw.management.domain.faction.Faction> allFactions =
        factionService.findAll();
    log.debug("Found {} factions", allFactions.size());
    context.setFactions(allFactions);

    // Get next PLE
    Optional<Show> nextPle =
        showService.getUpcomingShows(show.getShowDate(), 10).stream()
            .filter(Show::isPremiumLiveEvent)
            .findFirst();
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
    int currentSegmentCount = segmentRepository.findByShow(show).size();
    for (int i = 0; i < proposedSegments.size(); i++) {
      ProposedSegment proposedSegment = proposedSegments.get(i);
      log.debug("Processing segment: {}", proposedSegment);
      Segment segment = new Segment();
      segment.setShow(show);
      segment.setSegmentType(segmentTypeService.findByName(proposedSegment.getType()).get());
      segment.setSegmentDate(show.getShowDate().atStartOfDay(clock.getZone()).toInstant());
      segment.setNarration(proposedSegment.getDescription());
      segment.setSegmentOrder(currentSegmentCount + i + 1);

      segmentTypeService.findByName(proposedSegment.getType()).ifPresent(segment::setSegmentType);

      for (String participantName : proposedSegment.getParticipants()) {
        wrestlerService.findByName(participantName).ifPresent(segment::addParticipant);
      }

      segmentsToSave.add(segment);
    }
    segmentRepository.saveAll(segmentsToSave);
    log.info("Approved and saved {} segments for show: {}", segmentsToSave.size(), show.getName());
    eventPublisher.publishEvent(new SegmentsApprovedEvent(this, show));
  }
}
