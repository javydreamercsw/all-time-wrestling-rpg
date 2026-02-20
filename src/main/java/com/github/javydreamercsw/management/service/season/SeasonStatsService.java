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

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.SeasonStatsDTO;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for calculating season statistics for a wrestler. */
@Service
@Transactional
@Slf4j
public class SeasonStatsService {

  private final SegmentRepository segmentRepository;
  private final SeasonRepository seasonRepository;

  @Autowired
  public SeasonStatsService(
      SegmentRepository segmentRepository, SeasonRepository seasonRepository) {
    this.segmentRepository = segmentRepository;
    this.seasonRepository = seasonRepository;
  }

  /**
   * Calculates season statistics for a given wrestler and season.
   *
   * @param wrestler the wrestler to calculate stats for
   * @param season the season to calculate stats for
   * @return the season statistics
   */
  public SeasonStatsDTO calculateStats(@NonNull Wrestler wrestler, @NonNull Season season) {
    log.info(
        "Calculating season stats for wrestler {} in season {}",
        wrestler.getName(),
        season.getName());

    List<Segment> segments =
        segmentRepository
            .findByWrestlerParticipationAndSeason(
                wrestler, season, org.springframework.data.domain.Pageable.unpaged())
            .getContent();

    int wins = 0;
    int losses = 0;
    int draws = 0;

    for (Segment segment : segments) {
      if (isMatch(segment)) {
        if (segment.getWinners().contains(wrestler)) {
          wins++;
        } else if (segment.getWinners().isEmpty()) {
          draws++;
        } else {
          losses++;
        }
      }
    }

    List<String> accolades =
        wrestler.getReigns().stream()
            .filter(reign -> isReignInSeason(reign, season))
            .map(reign -> reign.getTitle().getName())
            .distinct()
            .collect(java.util.stream.Collectors.toList());

    return SeasonStatsDTO.builder()
        .seasonName(season.getName())
        .wins(wins)
        .losses(losses)
        .draws(draws)
        .startingFans(0L) // TODO: Implement fan history tracking
        .endingFans(wrestler.getFans())
        .accolades(accolades)
        .build();
  }

  private boolean isMatch(Segment segment) {
    if (segment.getSegmentType() == null) return false;
    String typeName = segment.getSegmentType().getName().toLowerCase();
    return !typeName.contains("promo")
        && !typeName.contains("beatdown")
        && !typeName.contains("confrontation")
        && !typeName.contains("review");
  }

  private boolean isReignInSeason(TitleReign reign, Season season) {
    Instant seasonStart = season.getStartDate();
    Instant seasonEnd = season.getEndDate() != null ? season.getEndDate() : Instant.now();
    Instant reignStart = reign.getStartDate();
    Instant reignEnd = reign.getEndDate() != null ? reign.getEndDate() : Instant.now();

    // Check for overlap
    return !reignStart.isAfter(seasonEnd) && !reignEnd.isBefore(seasonStart);
  }
}
