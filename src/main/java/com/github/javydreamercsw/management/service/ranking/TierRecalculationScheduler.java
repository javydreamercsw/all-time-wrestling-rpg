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
package com.github.javydreamercsw.management.service.ranking;

import com.github.javydreamercsw.base.service.ranking.RankingService;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.sync.review.SeedReviewGate;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TierRecalculationScheduler {

  private final RankingService rankingService;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final SeedReviewGate seedReviewGate;

  // Run every week on Sunday at midnight
  @Scheduled(cron = "0 0 0 * * SUN")
  public void recalculateTiers() {
    // Seed-update review gate: scheduled gameplay writes stay frozen while seed collisions
    // are undecided.
    if (seedReviewGate.isUp()) {
      log.info(
          "Weekly tier recalculation skipped: seed-update review gate is up ({} pending"
              + " decision(s)).",
          seedReviewGate.pendingCount());
      return;
    }
    rankingService.recalculateRanking(new ArrayList<>(wrestlerStateRepository.findAll()));
  }
}
