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
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
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
import com.github.javydreamercsw.management.domain.wrestler.WrestlerContractRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.gm.SalaryCalculator;
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
  private final WrestlerStateRepository wrestlerStateRepository;
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
  private final LeagueRosterRepository leagueRosterRepository;
  private final WrestlerContractRepository contractRepository;
  private final SalaryCalculator salaryCalculator;
  private final com.github.javydreamercsw.management.service.wrestler.RetirementService
      retirementService;

  ShowService(
      ShowRepository showRepository,
      ShowTypeRepository showTypeRepository,
      SeasonRepository seasonRepository,
      ShowTemplateRepository showTemplateRepository,
      UniverseRepository universeRepository,
      LeagueRepository leagueRepository,
      WrestlerStateRepository wrestlerStateRepository,
      Clock clock,
      SegmentAdjudicationService segmentAdjudicationService,
      SegmentRepository segmentRepository,
      ApplicationEventPublisher eventPublisher,
      WrestlerService wrestlerService,
      WrestlerRepository wrestlerRepository,
      GameSettingService gameSettingService,
      CommentaryTeamRepository commentaryTeamRepository,
      NewsGenerationService newsGenerationService,
      LegacyService legacyService,
      SecurityUtils securityUtils,
      ArenaRepository arenaRepository,
      LeagueRosterRepository leagueRosterRepository,
      WrestlerContractRepository contractRepository,
      SalaryCalculator salaryCalculator,
      com.github.javydreamercsw.management.service.wrestler.RetirementService retirementService) {
    this.showRepository = showRepository;
    this.showTypeRepository = showTypeRepository;
    this.seasonRepository = seasonRepository;
    this.showTemplateRepository = showTemplateRepository;
    this.universeRepository = universeRepository;
    this.leagueRepository = leagueRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
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
    this.leagueRosterRepository = leagueRosterRepository;
    this.contractRepository = contractRepository;
    this.salaryCalculator = salaryCalculator;
    this.retirementService = retirementService;
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> list(Pageable pageable) {
    return showRepository.findAllBy(pageable).toList();
  }

  @PreAuthorize("isAuthenticated()")
  public long count() {
    return showRepository.count();
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
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

  @PreAuthorize("isAuthenticated()")
  public List<String> getAllExternalIds() {
    return showRepository.findAllExternalIds();
  }

  @PreAuthorize("isAuthenticated()")
  public Page<Show> getAllShows(Pageable pageable) {
    return showRepository.findAllBy(pageable);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SHOWS_CACHE,
      key = "#id")
  public Optional<Show> getShowById(Long id) {
    return showRepository.findById(id);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.CALENDAR_CACHE,
      key = "#startDate + '-' + #endDate")
  public List<Show> getShowsByDateRange(LocalDate startDate, LocalDate endDate) {
    return showRepository.findByShowDateBetweenOrderByShowDate(startDate, endDate);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> getShowsForMonth(int year, int month) {
    YearMonth yearMonth = YearMonth.of(year, month);
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    return getShowsByDateRange(startDate, endDate);
  }

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

    List<Object[]> results =
        showRepository.findUpcomingShowIdsAndDatesForWrestler(
            referenceDate, wrestler.getId(), limit, 0);
    List<Long> showIds = results.stream().map(result -> ((Number) result[0]).longValue()).toList();

    if (showIds.isEmpty()) {
      return Collections.emptyList();
    }

    return showRepository.findByIdsWithRelationships(showIds, sort);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
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
      Long templateId,
      Long universeId,
      Long commentaryTeamId,
      Long arenaId) {

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

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
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
      Long templateId,
      Long universeId,
      Long commentaryTeamId,
      Long arenaId) {

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

              return showRepository.saveAndFlush(show);
            });
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
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

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
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

    Set<Long> participatingWrestlerIds = new HashSet<>();

    segmentRepository.findByShow(show).stream()
        .filter(segment -> segment.getAdjudicationStatus() == AdjudicationStatus.PENDING)
        .forEach(
            segment -> {
              segmentAdjudicationService.adjudicateMatch(segment);
              if (!segment.getSegmentType().getName().equals("Promo")) {
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

    gameSettingService.saveCurrentGameDate(show.getShowDate());

    if ("SHOW".equals(gameSettingService.getNewsStrategy())) {
      newsGenerationService.generateNewsForShow(show);
    }

    newsGenerationService.rollForRumor();

    securityUtils
        .getAuthenticatedUser()
        .ifPresent(details -> legacyService.incrementShowsBooked(details.getAccount()));

    processGmModeUpdates(show, participatingWrestlerIds);

    eventPublisher.publishEvent(new AdjudicationCompletedEvent(this, show));
  }

  private void processGmModeUpdates(Show show, Set<Long> participatingWrestlerIds) {
    if (show.getUniverse() == null) return;

    League league = leagueRepository.findByUniverse(show.getUniverse()).orElse(null);
    if (league == null) return;

    Long universeId = show.getUniverse().getId();

    java.math.BigDecimal totalExpenses = java.math.BigDecimal.ZERO;
    java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;

    double averageRating =
        segmentRepository.findByShow(show).stream()
            .mapToDouble(s -> s.getSegmentRating() != null ? s.getSegmentRating() : 0)
            .average()
            .orElse(0.0);

    if (show.getArena() != null && show.getArena().getCapacity() != null) {
      totalRevenue =
          java.math.BigDecimal.valueOf(show.getArena().getCapacity())
              .multiply(java.math.BigDecimal.valueOf(averageRating / 100.0))
              .multiply(new java.math.BigDecimal("10.00"));
    }

    List<Wrestler> allWrestlers = wrestlerRepository.findAll();
    for (Wrestler w : allWrestlers) {
      boolean participated = participatingWrestlerIds.contains(w.getId());

      WrestlerState state = wrestlerService.getOrCreateState(w.getId(), universeId);

      int currentStamina =
          state.getManagementStamina() != null ? state.getManagementStamina() : 100;
      if (participated) {
        state.setManagementStamina(
            Math.max(0, currentStamina - (10 + new java.util.Random().nextInt(11))));
      } else {
        state.setManagementStamina(
            Math.min(100, currentStamina + (15 + new java.util.Random().nextInt(11))));
      }

      int currentMorale = state.getMorale() != null ? state.getMorale() : 100;
      if (participated) {
        state.setMorale(Math.min(100, currentMorale + 2));
      } else if (state.getTier().ordinal()
          >= com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.MIDCARDER.ordinal()) {
        state.setMorale(Math.max(0, currentMorale - 5));
      }

      wrestlerStateRepository.save(state);

      if (participated) {
        totalExpenses = totalExpenses.add(salaryCalculator.calculateWeeklySalary(w));
      }

      // Check for retirement
      retirementService.checkRetirement(w, universeId);
    }

    java.math.BigDecimal currentBudget =
        league.getBudget() != null ? league.getBudget() : java.math.BigDecimal.ZERO;
    league.setBudget(currentBudget.add(totalRevenue).subtract(totalExpenses));

    List<WrestlerState> leagueStates =
        wrestlerStateRepository.findAll().stream()
            .filter(s -> s.getUniverse().equals(show.getUniverse()))
            .toList();

    if (!leagueStates.isEmpty()) {
      double avgMorale =
          leagueStates.stream().mapToInt(WrestlerState::getMorale).average().orElse(100.0);
      league.setLockerRoomMorale((int) avgMorale);
    }

    leagueRepository.save(league);

    log.info(
        "GM Mode Update for {}: Revenue: {}, Expenses: {}, New Budget: {}",
        show.getName(),
        totalRevenue,
        totalExpenses,
        league.getBudget());
  }

  @PreAuthorize("isAuthenticated()")
  public List<Segment> getSegments(@NonNull Show show) {
    return segmentRepository.findByShow(show);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Show> getShowsByUniverse(@NonNull Universe universe) {
    return showRepository.findByUniverse(universe);
  }
}
