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
package com.github.javydreamercsw.management.service.show;

import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.utils.DiceBag;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShowService {
  private final ShowRepository showRepository;
  private final ShowTypeRepository showTypeRepository;
  private final SeasonRepository seasonRepository;
  private final ShowTemplateRepository showTemplateRepository;
  private final Clock clock;
  private final SegmentAdjudicationService segmentAdjudicationService;
  private final SegmentRepository segmentRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final RivalryService rivalryService;
  private final WrestlerService wrestlerService;

  ShowService(
      ShowRepository showRepository,
      ShowTypeRepository showTypeRepository,
      SeasonRepository seasonRepository,
      ShowTemplateRepository showTemplateRepository,
      Clock clock,
      SegmentAdjudicationService segmentAdjudicationService,
      SegmentRepository segmentRepository,
      ApplicationEventPublisher eventPublisher,
      RivalryService rivalryService,
      WrestlerService wrestlerService) {
    this.showRepository = showRepository;
    this.showTypeRepository = showTypeRepository;
    this.seasonRepository = seasonRepository;
    this.showTemplateRepository = showTemplateRepository;
    this.clock = clock;
    this.segmentAdjudicationService = segmentAdjudicationService;
    this.segmentRepository = segmentRepository;
    this.eventPublisher = eventPublisher;
    this.rivalryService = rivalryService;
    this.wrestlerService = wrestlerService;
  }

  public List<Show> list(Pageable pageable) {
    return showRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return showRepository.count();
  }

  public Show save(@NonNull Show show) {
    show.setCreationDate(clock.instant());
    return showRepository.saveAndFlush(show);
  }

  public List<Show> findAll() {
    return showRepository.findAll();
  }

  /**
   * Find all shows with eagerly loaded relationships. This is useful for export operations to
   * prevent LazyInitializationException.
   *
   * @return List of all shows with eagerly loaded relationships
   */
  public List<Show> findAllWithRelationships() {
    return showRepository.findAllWithRelationships();
  }

  public List<Show> findByName(String showName) {
    return showRepository.findByName(showName);
  }

  public boolean existsByNameAndShowDate(String name, LocalDate showDate) {
    return showRepository.findByNameAndShowDate(name, showDate).isPresent();
  }

  public Optional<Show> findByExternalId(String externalId) {
    return showRepository.findByExternalId(externalId);
  }

  /**
   * Gets all external IDs of all shows.
   *
   * @return List of all external IDs.
   */
  public List<String> getAllExternalIds() {
    return showRepository.findAllExternalIds();
  }

  // ==================== CALENDAR-SPECIFIC METHODS ====================

  /**
   * Get all shows with pagination.
   *
   * @param pageable Pagination information
   * @return Page of shows
   */
  public Page<Show> getAllShows(Pageable pageable) {
    return showRepository.findAllBy(pageable);
  }

  /**
   * Get show by ID.
   *
   * @param id Show ID
   * @return Optional containing the show if found
   */
  public Optional<Show> getShowById(Long id) {
    return showRepository.findById(id);
  }

  /**
   * Get shows within a date range for calendar view.
   *
   * @param startDate Start date (inclusive)
   * @param endDate End date (inclusive)
   * @return List of shows in the date range
   */
  public List<Show> getShowsByDateRange(LocalDate startDate, LocalDate endDate) {
    return showRepository.findByShowDateBetweenOrderByShowDate(startDate, endDate);
  }

  /**
   * Get shows for a specific month and year.
   *
   * @param year Year
   * @param month Month (1-12)
   * @return List of shows in the specified month
   */
  public List<Show> getShowsForMonth(int year, int month) {
    YearMonth yearMonth = YearMonth.of(year, month);
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    return getShowsByDateRange(startDate, endDate);
  }

  /**
   * Get upcoming shows from today onwards.
   *
   * @param limit Maximum number of shows to return
   * @return List of upcoming shows
   */
  public List<Show> getUpcomingShows(int limit) {
    return getUpcomingShows(LocalDate.now(clock), limit);
  }

  public List<Show> getUpcomingShows(LocalDate referenceDate, int limit) {
    Pageable pageable = PageRequest.of(0, limit, Sort.by("showDate").ascending());
    return showRepository.findByShowDateGreaterThanEqualOrderByShowDate(referenceDate, pageable);
  }

  public List<Show> getUpcomingShowsWithRelationships(int limit) {
    return getUpcomingShowsWithRelationships(LocalDate.now(clock), limit);
  }

  public List<Show> getUpcomingShowsWithRelationships(LocalDate referenceDate, int limit) {
    Pageable pageable = PageRequest.of(0, limit, Sort.by("showDate").ascending());
    return showRepository.findUpcomingWithRelationships(referenceDate, pageable);
  }

  /**
   * Create a new show.
   *
   * @param name Show name
   * @param description Show description
   * @param showTypeId Show type ID
   * @param showDate Show date (optional)
   * @param seasonId Season ID (optional)
   * @param templateId Template ID (optional)
   * @return Created show
   */
  public Show createShow(
      String name,
      String description,
      Long showTypeId,
      LocalDate showDate,
      Long seasonId,
      Long templateId) {

    Show show = new Show();
    show.setName(name);
    show.setDescription(description);
    show.setShowDate(showDate);
    show.setCreationDate(clock.instant());

    // Set show type (required)
    ShowType showType =
        showTypeRepository
            .findById(showTypeId)
            .orElseThrow(() -> new IllegalArgumentException("Show type not found: " + showTypeId));
    show.setType(showType);

    // Set season (optional)
    if (seasonId != null) {
      Season season =
          seasonRepository
              .findById(seasonId)
              .orElseThrow(() -> new IllegalArgumentException("Season not found: " + seasonId));
      show.setSeason(season);
    }

    // Set template (optional)
    if (templateId != null) {
      ShowTemplate template =
          showTemplateRepository
              .findById(templateId)
              .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
      show.setTemplate(template);
    }

    return showRepository.saveAndFlush(show);
  }

  /**
   * Update an existing show.
   *
   * @param id Show ID
   * @param name Show name (optional)
   * @param description Show description (optional)
   * @param showTypeId Show type ID (optional)
   * @param showDate Show date (optional)
   * @param seasonId Season ID (optional)
   * @param templateId Template ID (optional)
   * @return Updated show if found
   */
  public Optional<Show> updateShow(
      Long id,
      String name,
      String description,
      Long showTypeId,
      LocalDate showDate,
      Long seasonId,
      Long templateId) {

    return showRepository
        .findById(id)
        .map(
            show -> {
              if (name != null) show.setName(name);
              if (description != null) show.setDescription(description);
              if (showDate != null) show.setShowDate(showDate);

              if (showTypeId != null) {
                ShowType showType =
                    showTypeRepository
                        .findById(showTypeId)
                        .orElseThrow(
                            () ->
                                new IllegalArgumentException("Show type not found: " + showTypeId));
                show.setType(showType);
              }

              if (seasonId != null) {
                Season season =
                    seasonRepository
                        .findById(seasonId)
                        .orElseThrow(
                            () -> new IllegalArgumentException("Season not found: " + seasonId));
                show.setSeason(season);
              } else {
                show.setSeason(null);
              }

              if (templateId != null) {
                ShowTemplate template =
                    showTemplateRepository
                        .findById(templateId)
                        .orElseThrow(
                            () ->
                                new IllegalArgumentException("Template not found: " + templateId));
                show.setTemplate(template);
              } else {
                show.setTemplate(null);
              }

              return showRepository.saveAndFlush(show);
            });
  }

  /**
   * Delete a show by ID.
   *
   * @param id Show ID
   * @return true if deleted, false if not found
   */
  public boolean deleteShow(Long id) {
    if (showRepository.existsById(id)) {
      showRepository.deleteById(id);
      return true;
    }
    return false;
  }

  public void adjudicateShow(Long showId) {
    Show show =
        showRepository
            .findById(showId)
            .orElseThrow(() -> new IllegalArgumentException("Show not found: " + showId));

    List<Wrestler> participatingWrestlers = new ArrayList<>();

    segmentRepository.findByShow(show).stream()
        .filter(segment -> segment.getAdjudicationStatus() == AdjudicationStatus.PENDING)
        .forEach(
            segment -> {
              segmentAdjudicationService.adjudicateMatch(segment);
              if (segment.getSegmentType().getName().equals("Promo")) {
                participatingWrestlers.addAll(segment.getWrestlers());
              }
              if (show.isPremiumLiveEvent()) {
                // Check if feuds should be resolved.
                switch (segment.getSegmentType().getName()) {
                  case "Tag Team":
                    attemptRivalryResolution(
                        segment.getWrestlers().get(0), segment.getWrestlers().get(2));
                    attemptRivalryResolution(
                        segment.getWrestlers().get(0), segment.getWrestlers().get(3));
                    attemptRivalryResolution(
                        segment.getWrestlers().get(1), segment.getWrestlers().get(2));
                    attemptRivalryResolution(
                        segment.getWrestlers().get(1), segment.getWrestlers().get(3));
                    break;
                  case "Abu Dhabi Rumble":
                  case "One on One":
                  case "Free-for-All":
                    int size = segment.getParticipants().size();
                    for (int i = 1; i < size; i++) {
                      attemptRivalryResolution(
                          segment.getWrestlers().get(0), segment.getWrestlers().get(i));
                    }
                    break;
                }
              }
              segment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
              segmentRepository.save(segment);
            });

    getNonParticipatingWrestlers(participatingWrestlers)
        .forEach(resting -> wrestlerService.healChance(resting.getId()));

    eventPublisher.publishEvent(new AdjudicationCompletedEvent(this, show));
  }

  public List<Segment> getSegments(@NonNull Show show) {
    return segmentRepository.findByShow(show);
  }

  private void attemptRivalryResolution(@NonNull Wrestler w1, @NonNull Wrestler w2) {
    DiceBag diceBag = new DiceBag(20);
    Optional<Rivalry> rivalryBetweenWrestlers =
        rivalryService.getRivalryBetweenWrestlers(w1.getId(), w2.getId());
    rivalryBetweenWrestlers.ifPresent(
        rivalry ->
            rivalryService.attemptResolution(rivalry.getId(), diceBag.roll(), diceBag.roll()));
  }

  /**
   * Returns a list of Wrestlers not in the provided participatingWrestlers list.
   *
   * @param participatingWrestlers List of currently participating wrestlers
   * @return List of Wrestlers not participating
   */
  public List<Wrestler> getNonParticipatingWrestlers(
      @NonNull List<Wrestler> participatingWrestlers) {
    List<Wrestler> allWrestlers = new ArrayList<>(wrestlerService.findAll());
    allWrestlers.removeAll(participatingWrestlers);
    return allWrestlers;
  }
}
