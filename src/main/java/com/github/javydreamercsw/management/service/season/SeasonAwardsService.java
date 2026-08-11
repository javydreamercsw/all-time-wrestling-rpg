/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.season;

import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonAward;
import com.github.javydreamercsw.management.domain.season.SeasonAwardRepository;
import com.github.javydreamercsw.management.domain.season.SeasonAwardType;
import com.github.javydreamercsw.management.domain.season.WrestlerSeasonSnapshot;
import com.github.javydreamercsw.management.domain.season.WrestlerSeasonSnapshotRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.dto.SeasonEndedEvent;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeasonAwardsService {

  private final SeasonAwardRepository awardRepository;
  private final WrestlerSeasonSnapshotRepository snapshotRepository;
  private final SegmentRepository segmentRepository;
  private final TitleReignRepository titleReignRepository;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerService wrestlerService;
  private final NewsService newsService;
  private final Clock clock;

  @EventListener
  @Transactional
  public void onSeasonEnded(final SeasonEndedEvent event) {
    Season season = event.getSeason();
    log.info("Season '{}' ended — computing awards", season.getName());

    if (awardRepository.findBySeasonId(season.getId()).isEmpty()) {
      List<SeasonAward> awards = computeAwards(season);
      awards.forEach(awardRepository::save);
      applyFanBonuses(awards, season);
      generateAwardsNews(season, awards);
    }
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<SeasonAward> getAwards(final Long seasonId) {
    return awardRepository.findBySeasonId(seasonId);
  }

  List<SeasonAward> computeAwards(final Season season) {
    List<Show> shows = season.getShows();
    List<SeasonAward> awards = new ArrayList<>();

    selectWrestlerOfTheYear(season, shows).ifPresent(awards::add);
    selectMostImproved(season).ifPresent(awards::add);
    selectMostDecorated(season).ifPresent(awards::add);

    return awards;
  }

  private Optional<SeasonAward> selectWrestlerOfTheYear(
      final Season season, final List<Show> shows) {
    Map<Long, Long> winsByWrestler = new HashMap<>();

    for (Show show : shows) {
      for (Segment segment : segmentRepository.findByShow(show)) {
        for (Wrestler winner : segment.getWinners()) {
          if (winner.getId() != null) {
            winsByWrestler.merge(winner.getId(), 1L, Long::sum);
          }
        }
      }
    }

    return winsByWrestler.entrySet().stream()
        .max(Comparator.comparingLong(Map.Entry::getValue))
        .flatMap(
            entry ->
                wrestlerRepository
                    .findById(entry.getKey())
                    .map(
                        w ->
                            SeasonAward.builder()
                                .season(season)
                                .wrestler(w)
                                .awardType(SeasonAwardType.WRESTLER_OF_THE_YEAR)
                                .awardValue(entry.getValue() + " wins")
                                .awardedAt(Instant.now(clock))
                                .build()));
  }

  private Optional<SeasonAward> selectMostImproved(final Season season) {
    List<WrestlerSeasonSnapshot> snapshots =
        snapshotRepository.findAll().stream()
            .filter(s -> s.getSeason().getId().equals(season.getId()))
            .toList();

    long bestGrowth = Long.MIN_VALUE;
    Wrestler bestWrestler = null;

    for (WrestlerSeasonSnapshot snapshot : snapshots) {
      Wrestler wrestler = snapshot.getWrestler();
      long currentFans =
          wrestlerRepository
              .findByIdWithStates(wrestler.getId())
              .flatMap(w -> w.getDefaultState().map(state -> state.getFans()))
              .orElse(0L);
      long growth = currentFans - snapshot.getStartingFans();
      if (growth > bestGrowth) {
        bestGrowth = growth;
        bestWrestler = wrestler;
      }
    }

    if (bestWrestler == null || bestGrowth <= 0) {
      return Optional.empty();
    }

    final Wrestler winner = bestWrestler;
    final long fanGrowth = bestGrowth;
    return Optional.of(
        SeasonAward.builder()
            .season(season)
            .wrestler(winner)
            .awardType(SeasonAwardType.MOST_IMPROVED)
            .awardValue(String.format("%,d new fans", fanGrowth))
            .awardedAt(Instant.now(clock))
            .build());
  }

  private Optional<SeasonAward> selectMostDecorated(final Season season) {
    if (season.getStartDate() == null) {
      return Optional.empty();
    }
    Instant end = season.getEndDate() != null ? season.getEndDate() : Instant.now(clock);

    List<TitleReign> reigns =
        titleReignRepository.findByStartDateBetween(season.getStartDate(), end);

    Map<Long, Long> reignsByWrestler = new HashMap<>();
    Map<Long, Wrestler> wrestlerMap = new HashMap<>();

    for (TitleReign reign : reigns) {
      for (Wrestler champion : reign.getChampions()) {
        if (champion.getId() != null) {
          reignsByWrestler.merge(champion.getId(), 1L, Long::sum);
          wrestlerMap.put(champion.getId(), champion);
        }
      }
    }

    return reignsByWrestler.entrySet().stream()
        .max(Comparator.comparingLong(Map.Entry::getValue))
        .map(
            entry ->
                SeasonAward.builder()
                    .season(season)
                    .wrestler(wrestlerMap.get(entry.getKey()))
                    .awardType(SeasonAwardType.MOST_DECORATED)
                    .awardValue(entry.getValue() + " title reign(s)")
                    .awardedAt(Instant.now(clock))
                    .build());
  }

  private void applyFanBonuses(final List<SeasonAward> awards, final Season season) {
    Optional<Long> universeId =
        season.getShows().stream()
            .filter(s -> s.getUniverse() != null)
            .map(s -> s.getUniverse().getId())
            .findFirst();

    if (universeId.isEmpty()) {
      log.warn("No universe found for season '{}' — skipping fan bonuses", season.getName());
      return;
    }

    for (SeasonAward award : awards) {
      try {
        wrestlerService.awardFans(
            award.getWrestler().getId(), universeId.get(), award.getAwardType().getFanBonus());
        log.info(
            "Awarded {} fans to {} for {}",
            award.getAwardType().getFanBonus(),
            award.getWrestler().getName(),
            award.getAwardType().getDisplayName());
      } catch (Exception e) {
        log.error("Failed to apply fan bonus for award {}", award.getAwardType(), e);
      }
    }
  }

  private void generateAwardsNews(final Season season, final List<SeasonAward> awards) {
    if (awards.isEmpty()) {
      return;
    }

    StringBuilder content = new StringBuilder();
    content
        .append("The ")
        .append(season.getName())
        .append(
            " has come to a close. The promotion gathered to honour its finest performers:\n\n");

    for (SeasonAward award : awards) {
      content
          .append("  • ")
          .append(award.getAwardType().getDisplayName())
          .append(": ")
          .append(award.getWrestler().getName())
          .append(" (")
          .append(award.getAwardValue())
          .append(")\n");
    }

    content.append(
        "\n"
            + "Congratulations to all winners — the crowd will remember this season for years to"
            + " come.");

    newsService.createNewsItem(
        season.getName() + " Season Awards Ceremony",
        content.toString(),
        NewsCategory.ANALYSIS,
        false,
        4);
  }
}
