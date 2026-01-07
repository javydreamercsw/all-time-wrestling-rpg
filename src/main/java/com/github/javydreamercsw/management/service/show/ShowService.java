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
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
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
  private final WrestlerRepository wrestlerRepository;
  private final GameSettingService gameSettingService;

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
      WrestlerService wrestlerService,
      WrestlerRepository wrestlerRepository,
      GameSettingService gameSettingService) {
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
    this.wrestlerRepository = wrestlerRepository;
    this.gameSettingService = gameSettingService;
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> list(Pageable pageable) {
    return showRepository.findAllBy(pageable).toList();
  }

  @PreAuthorize("isAuthenticated()")
  public long count() {
    return showRepository.count();
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  public Show save(@NonNull Show show) {
    show.setCreationDate(clock.instant());
    return showRepository.saveAndFlush(show);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
      key = "'all'")
  public List<Show> findAll() {
    return showRepository.findAll();
  }

  /**
   * Find all shows with eagerly loaded relationships. This is useful for export operations to
   * prevent LazyInitializationException.
   *
   * @return List of all shows with eagerly loaded relationships
   */
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
      key = "'allWithRelationships'")
  public List<Show> findAllWithRelationships() {
    return showRepository.findAllWithRelationships();
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> findByName(String showName) {
    return showRepository.findByName(showName);
  }

  @PreAuthorize("isAuthenticated()")
  public boolean existsByNameAndShowDate(String name, LocalDate showDate) {
    return showRepository.findByNameAndShowDate(name, showDate).isPresent();
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Show> findByExternalId(String externalId) {
    return showRepository.findByExternalId(externalId);
  }

  /**
   * Gets all external IDs of all shows.
   *
   * @return List of all external IDs.
   */
  @PreAuthorize("isAuthenticated()")
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
  @PreAuthorize("isAuthenticated()")
  public Page<Show> getAllShows(Pageable pageable) {
    return showRepository.findAllBy(pageable);
  }

  /**
   * Get show by ID.
   *
   * @param id Show ID
   * @return Optional containing the show if found
   */
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
      key = "#id")
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
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE,
      key = "#startDate + '-' + #endDate")
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
  @PreAuthorize("isAuthenticated()")
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
  @PreAuthorize("isAuthenticated()")
  public List<Show> getUpcomingShows(int limit) {
    LocalDate referenceDate = gameSettingService.getCurrentGameDate();
    Pageable pageable = PageRequest.of(0, limit, Sort.by("showDate").ascending());
    return showRepository.findByShowDateGreaterThanEqualOrderByShowDate(referenceDate, pageable);
  }

  public List<Show> getUpcomingShowsWithRelationships(LocalDate referenceDate, int limit) {
    Pageable pageable = PageRequest.of(0, limit, Sort.by("showDate").ascending());
    return showRepository.findUpcomingWithRelationships(referenceDate, pageable);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> getUpcomingShowsForWrestler(@NonNull Wrestler wrestler, int limit) {
    LocalDate referenceDate = gameSettingService.getCurrentGameDate();
    Sort sort = Sort.by("showDate").ascending();

    // Step 1: Get show IDs using the native query with limit and offset
    // For offset, we can assume 0 for the first page for now.
    List<Object[]> results =
        showRepository.findUpcomingShowIdsAndDatesForWrestler(
            referenceDate, wrestler.getId(), limit, 0);
    List<Long> showIds = results.stream().map(result -> ((Number) result[0]).longValue()).toList();

    if (showIds.isEmpty()) {
      return Collections.emptyList();
    }

    // Step 2: Fetch shows by IDs with relationships and apply sorting
    return showRepository.findByIdsWithRelationships(showIds, sort);
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
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = {
        com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE
      },
      allEntries = true)
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
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = {
        com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE
      },
      allEntries = true)
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
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = {
        com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE
      },
      allEntries = true)
  public boolean deleteShow(@NonNull Long id) {
    if (showRepository.existsById(id)) {
      showRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = {
        com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE
      },
      allEntries = true)
  public void adjudicateShow(@NonNull Long showId) {
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
              if (!segment.getSegmentType().getName().equals("Promo")) {
                participatingWrestlers.addAll(segment.getWrestlers());
              }
              segment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
              segmentRepository.save(segment);
            });

    getNonParticipatingWrestlers(participatingWrestlers)
        .forEach(resting -> wrestlerService.healChance(resting.getId()));

    gameSettingService.saveCurrentGameDate(show.getShowDate());

    eventPublisher.publishEvent(new AdjudicationCompletedEvent(this, show));
  }

  @PreAuthorize("isAuthenticated()")
  public List<Segment> getSegments(@NonNull Show show) {
    return segmentRepository.findByShow(show);
  }

  /**
   * Returns a list of Wrestlers not in the provided participatingWrestlers list.
   *
   * @param participatingWrestlers List of currently participating wrestlers
   * @return List of Wrestlers not participating
   */
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> getNonParticipatingWrestlers(
      @NonNull List<Wrestler> participatingWrestlers) {
    List<Wrestler> allWrestlers = new ArrayList<>(wrestlerRepository.findAll());
    allWrestlers.removeAll(participatingWrestlers);
    return allWrestlers;
  }
}
