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

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
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
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.gm.GmModeService;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
@lombok.extern.slf4j.Slf4j
public class ShowService {
  private final ShowRepository showRepository;
  private final ShowTypeRepository showTypeRepository;
  private final SeasonRepository seasonRepository;
  private final ShowTemplateRepository showTemplateRepository;
  private final UniverseRepository universeRepository;
  private final LeagueRepository leagueRepository;
  private final Clock clock;
  private final SegmentAdjudicationService segmentAdjudicationService;
  private final SegmentRepository segmentRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final WrestlerService wrestlerService;
  private final WrestlerRepository wrestlerRepository;
  private final GameSettingService gameSettingService;
  private final CommentaryTeamRepository commentaryTeamRepository;
  private final NewsGenerationService newsGenerationService;
  private final LegacyService legacyService;
  private final SecurityUtils securityUtils;
  private final ArenaRepository arenaRepository;
  private final GmModeService gmModeService;

  ShowService(
      final ShowRepository showRepository,
      final ShowTypeRepository showTypeRepository,
      final SeasonRepository seasonRepository,
      final ShowTemplateRepository showTemplateRepository,
      final UniverseRepository universeRepository,
      final LeagueRepository leagueRepository,
      final Clock clock,
      final SegmentAdjudicationService segmentAdjudicationService,
      final SegmentRepository segmentRepository,
      final ApplicationEventPublisher eventPublisher,
      final WrestlerService wrestlerService,
      final WrestlerRepository wrestlerRepository,
      final GameSettingService gameSettingService,
      final CommentaryTeamRepository commentaryTeamRepository,
      final NewsGenerationService newsGenerationService,
      final LegacyService legacyService,
      final SecurityUtils securityUtils,
      final ArenaRepository arenaRepository,
      final GmModeService gmModeService) {
    this.showRepository = showRepository;
    this.showTypeRepository = showTypeRepository;
    this.seasonRepository = seasonRepository;
    this.showTemplateRepository = showTemplateRepository;
    this.universeRepository = universeRepository;
    this.leagueRepository = leagueRepository;
    this.clock = clock;
    this.segmentAdjudicationService = segmentAdjudicationService;
    this.segmentRepository = segmentRepository;
    this.eventPublisher = eventPublisher;
    this.wrestlerService = wrestlerService;
    this.wrestlerRepository = wrestlerRepository;
    this.gameSettingService = gameSettingService;
    this.commentaryTeamRepository = commentaryTeamRepository;
    this.newsGenerationService = newsGenerationService;
    this.legacyService = legacyService;
    this.securityUtils = securityUtils;
    this.arenaRepository = arenaRepository;
    this.gmModeService = gmModeService;
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> list(final Pageable pageable) {
    return showRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return showRepository.count();
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Show save(@NonNull final Show show) {
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

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
      key = "'allWithRelationships'")
  public List<Show> findAllWithRelationships() {
    return showRepository.findAllWithRelationships();
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> findByName(final String showName) {
    return showRepository.findByName(showName);
  }

  @PreAuthorize("isAuthenticated()")
  public boolean existsByNameAndShowDate(final String name, final LocalDate showDate) {
    return showRepository.findByNameAndShowDate(name, showDate).isPresent();
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Show> findByExternalId(final String externalId) {
    return showRepository.findByExternalId(externalId);
  }

  @PreAuthorize("isAuthenticated()")
  public List<String> getAllExternalIds() {
    return showRepository.findAllExternalIds();
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> findAllByExternalId(final List<String> externalIds) {
    return showRepository.findAllByExternalIdIn(externalIds);
  }

  @PreAuthorize("isAuthenticated()")
  public Page<Show> getAllShows(final Pageable pageable) {
    return showRepository.findAllBy(pageable);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
      key = "#id")
  public Optional<Show> getShowById(final Long id) {
    return showRepository.findById(id);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE,
      key = "#startDate + '-' + #endDate")
  public List<Show> getShowsByDateRange(final LocalDate startDate, final LocalDate endDate) {
    return showRepository.findByShowDateBetweenOrderByShowDate(startDate, endDate);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> getShowsForMonth(final int year, final int month) {
    YearMonth yearMonth = YearMonth.of(year, month);
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    return getShowsByDateRange(startDate, endDate);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> getUpcomingShows(final int limit) {
    LocalDate referenceDate = gameSettingService.getCurrentGameDate();
    Pageable pageable = PageRequest.of(0, limit, Sort.by("showDate").ascending());
    return showRepository.findByShowDateGreaterThanEqualOrderByShowDate(referenceDate, pageable);
  }

  public List<Show> getUpcomingShowsWithRelationships(
      final LocalDate referenceDate, final int limit) {
    Pageable pageable = PageRequest.of(0, limit, Sort.by("showDate").ascending());
    return showRepository.findUpcomingWithRelationships(referenceDate, pageable);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> getUpcomingShowsForWrestler(@NonNull final Wrestler wrestler, final int limit) {
    LocalDate referenceDate = gameSettingService.getCurrentGameDate();
    Sort sort = Sort.by("showDate").ascending();

    List<Object[]> results =
        showRepository.findUpcomingShowIdsAndDatesForWrestler(
            referenceDate, wrestler.getId(), limit, 0);
    List<Long> showIds = results.stream().map(result -> ((Number) result[0]).longValue()).toList();

    if (showIds.isEmpty()) {
      return Collections.emptyList();
    }

    return showRepository.findByIdsWithRelationships(showIds, sort);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = {
        com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE
      },
      allEntries = true)
  public Show createShow(
      final String name,
      final String description,
      final Long showTypeId,
      final LocalDate showDate,
      final Long seasonId,
      final Long templateId,
      final Long universeId,
      @org.springframework.lang.Nullable final Long leagueId,
      final Long commentaryTeamId,
      final Long arenaId) {

    Show show = new Show();
    show.setName(name);
    show.setDescription(description);
    show.setShowDate(showDate);
    show.setCreationDate(clock.instant());

    ShowType showType =
        showTypeRepository
            .findById(showTypeId)
            .orElseThrow(() -> new IllegalArgumentException("Show type not found: " + showTypeId));
    show.setType(showType);

    if (seasonId != null) {
      Season season =
          seasonRepository
              .findById(seasonId)
              .orElseThrow(() -> new IllegalArgumentException("Season not found: " + seasonId));
      show.setSeason(season);
    }

    if (templateId != null) {
      ShowTemplate template =
          showTemplateRepository
              .findById(templateId)
              .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
      show.setTemplate(template);
    }

    if (universeId != null) {
      Universe universe =
          universeRepository
              .findById(universeId)
              .orElseThrow(() -> new IllegalArgumentException("Universe not found: " + universeId));
      show.setUniverse(universe);
    }

    if (leagueId != null) {
      League league = leagueRepository.findById(leagueId).orElse(null);
      if (league != null) {
        show.setLeague(league);
      }
    }

    if (commentaryTeamId != null) {
      show.setCommentaryTeam(
          commentaryTeamRepository
              .findById(commentaryTeamId)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Commentary team not found: " + commentaryTeamId)));
    }

    if (arenaId != null) {
      show.setArena(
          arenaRepository
              .findById(arenaId)
              .orElseThrow(() -> new IllegalArgumentException("Arena not found: " + arenaId)));
    }

    return showRepository.saveAndFlush(show);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = {
        com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE
      },
      allEntries = true)
  public Optional<Show> updateShow(
      final Long id,
      final String name,
      final String description,
      final Long showTypeId,
      final LocalDate showDate,
      final Long seasonId,
      final Long templateId,
      final Long universeId,
      final Long commentaryTeamId,
      final Long arenaId) {
    return updateShow(
        id,
        name,
        description,
        showTypeId,
        showDate,
        seasonId,
        templateId,
        universeId,
        commentaryTeamId,
        arenaId,
        null,
        null,
        null);
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = {
        com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE
      },
      allEntries = true)
  public Optional<Show> updateShow(
      final Long id,
      final String name,
      final String description,
      final Long showTypeId,
      final LocalDate showDate,
      final Long seasonId,
      final Long templateId,
      final Long universeId,
      final Long commentaryTeamId,
      final Long arenaId,
      final Long leagueId,
      final Integer attendance,
      final java.math.BigDecimal gateRevenue) {

    return showRepository
        .findById(id)
        .map(
            show -> {
              if (name != null) {
                show.setName(name);
              }
              if (description != null) {
                show.setDescription(description);
              }
              if (showDate != null) {
                show.setShowDate(showDate);
              }

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

              if (universeId != null) {
                Universe universe =
                    universeRepository
                        .findById(universeId)
                        .orElseThrow(
                            () ->
                                new IllegalArgumentException("Universe not found: " + universeId));
                show.setUniverse(universe);
              } else {
                show.setUniverse(null);
              }

              if (commentaryTeamId != null) {
                show.setCommentaryTeam(
                    commentaryTeamRepository
                        .findById(commentaryTeamId)
                        .orElseThrow(
                            () ->
                                new IllegalArgumentException(
                                    "Commentary team not found: " + commentaryTeamId)));
              } else {
                show.setCommentaryTeam(null);
              }

              if (arenaId != null) {
                show.setArena(
                    arenaRepository
                        .findById(arenaId)
                        .orElseThrow(
                            () -> new IllegalArgumentException("Arena not found: " + arenaId)));
              } else {
                show.setArena(null);
              }

              if (leagueId != null) {
                show.setLeague(
                    leagueRepository
                        .findById(leagueId)
                        .orElseThrow(
                            () -> new IllegalArgumentException("League not found: " + leagueId)));
              } else {
                show.setLeague(null);
              }

              if (attendance != null) {
                show.setAttendance(attendance);
              }

              if (gateRevenue != null) {
                show.setGateRevenue(gateRevenue);
              }

              return showRepository.saveAndFlush(show);
            });
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = {
        com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE
      },
      allEntries = true)
  public boolean deleteShow(@NonNull final Long id) {
    if (showRepository.existsById(id)) {
      showRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = {
        com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE
      },
      allEntries = true)
  public void adjudicateShow(@NonNull final Long showId) {
    Show show =
        showRepository
            .findById(showId)
            .orElseThrow(() -> new IllegalArgumentException("Show not found: " + showId));

    Set<Long> participatingWrestlerIds = new HashSet<>();

    segmentRepository.findByShow(show).stream()
        .filter(segment -> segment.getAdjudicationStatus() == AdjudicationStatus.PENDING)
        .forEach(
            segment -> {
              segmentAdjudicationService.adjudicateMatch(segment);
              if (!"Promo".equals(segment.getSegmentType().getName())) {
                segment.getWrestlers().forEach(w -> participatingWrestlerIds.add(w.getId()));
              }
              segment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
              segmentRepository.save(segment);
            });

    wrestlerRepository.findAll().stream()
        .filter(w -> !participatingWrestlerIds.contains(w.getId()))
        .forEach(
            resting -> {
              Long universeId = show.getUniverse() != null ? show.getUniverse().getId() : 1L;
              wrestlerService.healChance(resting.getId(), universeId);
            });

    if (show.getShowDate() != null) {
      gameSettingService.saveCurrentGameDate(show.getShowDate().plusDays(1));
    }

    if ("SHOW".equals(gameSettingService.getNewsStrategy())) {
      newsGenerationService.generateNewsForShow(show);
    }

    newsGenerationService.rollForRumor();

    securityUtils
        .getAuthenticatedUser()
        .ifPresent(details -> legacyService.incrementShowsBooked(details.getAccount()));

    gmModeService.processShowUpdates(show, participatingWrestlerIds);

    eventPublisher.publishEvent(new AdjudicationCompletedEvent(this, show));
  }

  @PreAuthorize("isAuthenticated()")
  public List<Segment> getSegments(@NonNull final Show show) {
    return segmentRepository.findByShow(show);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> getShowsByUniverse(@NonNull final Universe universe) {
    return showRepository.findByUniverseOrUniverseIsNull(universe);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> getShowsByLeague(@NonNull final League league) {
    return showRepository.findByLeague(league);
  }

  /**
   * Checks if all segments for the show are adjudicated and, if so, finalizes attendance and gate
   * revenue. The guard on existing attendance prevents re-running.
   */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void finalizeShowIfComplete(@NonNull final Show show) {
    if (show.getAttendance() != null && show.getAttendance() > 0) {
      return;
    }
    List<Segment> segments = segmentRepository.findByShow(show);
    if (segments.isEmpty()) {
      return;
    }
    boolean allAdjudicated =
        segments.stream()
            .allMatch(
                s ->
                    com.github.javydreamercsw.management.domain.AdjudicationStatus.ADJUDICATED
                        .equals(s.getAdjudicationStatus()));
    if (allAdjudicated) {
      finalizeShow(show, segments);
    }
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Show finalizeShow(@NonNull final Show show, @NonNull final List<Segment> segments) {
    // Collect unique wrestlers across all segments
    java.util.Set<Long> seen = new java.util.HashSet<>();
    long totalFanWeight =
        segments.stream()
            .flatMap(s -> s.getWrestlers().stream())
            .filter(w -> w.getId() != null && seen.add(w.getId()))
            .mapToLong(
                w -> {
                  Long universeId = show.getUniverse() != null ? show.getUniverse().getId() : null;
                  return w.getFanWeight(universeId);
                })
            .sum();

    int baseAttendance =
        (int) (totalFanWeight / ShowEconomicsConstants.FAN_WEIGHT_ATTENDANCE_DIVISOR);

    double showMultiplier =
        show.isPremiumLiveEvent()
            ? ShowEconomicsConstants.PREMIUM_EVENT_MULTIPLIER
            : ShowEconomicsConstants.BASE_TRAIT_MULTIPLIER;

    double traitMultiplier = ShowEconomicsConstants.BASE_TRAIT_MULTIPLIER;
    if (show.getArena() != null && !show.getArena().getEnvironmentalTraits().isEmpty()) {
      long matchingTraits =
          show.getArena().getEnvironmentalTraits().stream()
              .filter(trait -> traitMatchesCard(trait, segments))
              .count();
      traitMultiplier =
          ShowEconomicsConstants.BASE_TRAIT_MULTIPLIER
              + Math.min(
                  matchingTraits * ShowEconomicsConstants.TRAIT_BONUS_PER_MATCH,
                  ShowEconomicsConstants.MAX_TRAIT_BONUS);
    }

    int projected = (int) (baseAttendance * showMultiplier * traitMultiplier);
    int finalAttendance =
        show.getArena() != null && show.getArena().getCapacity() != null
            ? Math.min(projected, show.getArena().getCapacity())
            : projected;

    java.math.BigDecimal ticketPrice =
        show.isPremiumLiveEvent()
            ? ShowEconomicsConstants.PREMIUM_TICKET_PRICE
            : ShowEconomicsConstants.STANDARD_TICKET_PRICE;
    java.math.BigDecimal gateRevenue =
        ticketPrice.multiply(java.math.BigDecimal.valueOf(finalAttendance));

    show.setAttendance(finalAttendance);
    show.setGateRevenue(gateRevenue);
    Show saved = showRepository.save(show);

    // Credit gate revenue to league budget (GM mode only)
    if (show.getLeague() != null) {
      League league = show.getLeague();
      java.math.BigDecimal current =
          league.getBudget() != null ? league.getBudget() : java.math.BigDecimal.ZERO;
      league.setBudget(current.add(gateRevenue));
      leagueRepository.save(league);
      log.info(
          "Show '{}' finalized: attendance={}, gate revenue={}, added to league '{}'",
          show.getName(),
          finalAttendance,
          gateRevenue,
          league.getName());
    } else {
      log.info(
          "Show '{}' finalized: attendance={}, gate revenue={}",
          show.getName(),
          finalAttendance,
          gateRevenue);
    }

    return saved;
  }

  private boolean traitMatchesCard(
      @NonNull final String trait, @NonNull final List<Segment> segments) {
    String t = trait.toLowerCase();
    boolean isHardcore = t.contains("hardcore") || t.contains("barbed-wire");
    if (isHardcore) {
      return segments.stream()
          .anyMatch(
              s ->
                  s.getSegmentRules().stream()
                      .anyMatch(
                          r ->
                              Boolean.TRUE.equals(r.getNoDq())
                                  || r.getName().toLowerCase().contains("hardcore")
                                  || r.getName().toLowerCase().contains("extreme")));
    }
    return false;
  }
}
